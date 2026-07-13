package com.flowforge.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.api.dto.InternalExecutionFinalizeRequest;
import com.flowforge.api.exception.ResourceNotFoundException;
import com.flowforge.api.model.*;
import com.flowforge.api.repository.*;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import com.flowforge.api.shared.workflow.DagValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowEngineService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngineService.class);

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final NodeExecutionRepository nodeExecutionRepository;
    private final ProjectRepository projectRepository;
    private final JobRepository jobRepository;
    private final ExecutionService executionService;
    private final OutboxEventRepository outboxEventRepository;
    private final PublicIdGenerator publicIdGenerator;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    public WorkflowEngineService(
            WorkflowDefinitionRepository definitionRepository,
            WorkflowRunRepository workflowRunRepository,
            NodeExecutionRepository nodeExecutionRepository,
            ProjectRepository projectRepository,
            JobRepository jobRepository,
            ExecutionService executionService,
            OutboxEventRepository outboxEventRepository,
            PublicIdGenerator publicIdGenerator,
            Clock clock,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate,
            MeterRegistry meterRegistry) {
        this.definitionRepository = definitionRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.nodeExecutionRepository = nodeExecutionRepository;
        this.projectRepository = projectRepository;
        this.jobRepository = jobRepository;
        this.executionService = executionService;
        this.outboxEventRepository = outboxEventRepository;
        this.publicIdGenerator = publicIdGenerator;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public WorkflowDefinition createDefinition(UUID projectPublicId, String name, String definitionJson) {
        Project project = projectRepository.findByPublicId(projectPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        DagValidator.validate(definitionJson, objectMapper);

        Integer maxVersion = jdbcTemplate.queryForObject(
                "SELECT MAX(version) FROM workflow_definitions WHERE project_id = ? AND name = ?",
                Integer.class,
                project.getId(),
                name
        );
        int version = (maxVersion != null) ? maxVersion + 1 : 1;

        WorkflowDefinition definition = new WorkflowDefinition(
                publicIdGenerator.generate(),
                project,
                name,
                version,
                definitionJson,
                clock.instant()
        );

        meterRegistry.counter("flowforge.workflow.definitions.created").increment();
        return definitionRepository.save(definition);
    }

    @Transactional
    public WorkflowRun startWorkflowRun(UUID definitionPublicId) {
        WorkflowDefinition definition = definitionRepository.findByPublicId(definitionPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow definition not found"));

        Instant now = clock.instant();
        WorkflowRun run = new WorkflowRun(
                publicIdGenerator.generate(),
                definition,
                now
        );

        workflowRunRepository.save(run);

        // Emit WORKFLOW_STARTED event
        emitWorkflowStartedEvent(run);

        // Deserialize DAG definition
        DagValidator.WorkflowGraph graph;
        try {
            graph = objectMapper.readValue(definition.getDefinitionJson(), DagValidator.WorkflowGraph.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse workflow graph", e);
        }

        // Find START node
        DagValidator.Node startNode = graph.nodes.stream()
                .filter(n -> "START".equalsIgnoreCase(n.type))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Start node not found"));

        // Create START NodeExecution in SUCCEEDED status (instant)
        NodeExecution startExec = new NodeExecution(run, startNode.id, NodeExecutionStatus.SUCCEEDED, now);
        startExec.succeed(now);
        nodeExecutionRepository.save(startExec);

        emitNodeStartedEvent(run, startNode.id, now);
        emitNodeCompletedEvent(run, startNode.id, "SUCCEEDED", now);

        meterRegistry.counter("flowforge.workflow.runs.active").increment();
        meterRegistry.counter("flowforge.workflow.node.executions").increment();

        // Process outgoing edges from START node
        List<DagValidator.Edge> outgoing = graph.edges.stream()
                .filter(e -> startNode.id.equals(e.from))
                .collect(Collectors.toList());

        if (outgoing.size() > 1) {
            meterRegistry.counter("flowforge.workflow.fanout").increment(outgoing.size());
        }

        for (DagValidator.Edge edge : outgoing) {
            activateNode(run, graph, edge.to);
        }

        return run;
    }

    @Transactional
    public void processNodeCompletion(UUID executionPublicId, String finalStatus, Integer httpStatus, String networkError) {
        Optional<NodeExecution> nodeOpt = nodeExecutionRepository.findByExecutionPublicId(executionPublicId);
        if (nodeOpt.isEmpty()) {
            // Not a workflow execution, ignore
            return;
        }

        NodeExecution nodeExec = nodeOpt.get();
        if (nodeExec.getStatus() != NodeExecutionStatus.RUNNING) {
            logger.warn("Idempotency: duplicate completed event received for node execution: {}. Status is already: {}.",
                    nodeExec.getNodeId(), nodeExec.getStatus());
            return;
        }

        Instant now = clock.instant();
        WorkflowRun run = nodeExec.getWorkflowRun();

        // 1. Transition NodeExecution status
        if ("SUCCEEDED".equalsIgnoreCase(finalStatus)) {
            nodeExec.succeed(now);
        } else {
            nodeExec.fail(now);
        }
        nodeExecutionRepository.save(nodeExec);

        emitNodeCompletedEvent(run, nodeExec.getNodeId(), nodeExec.getStatus().name(), now);

        // Deserialize DAG definition
        DagValidator.WorkflowGraph graph;
        try {
            graph = objectMapper.readValue(run.getWorkflowDefinition().getDefinitionJson(), DagValidator.WorkflowGraph.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse workflow graph", e);
        }

        // 2. Handle failure
        if (nodeExec.getStatus() == NodeExecutionStatus.FAILED) {
            logger.info("Workflow run {} failed because node {} failed.", run.getPublicId(), nodeExec.getNodeId());
            // Transition other active executions to CANCELLED or just fail run
            run.fail(now);
            workflowRunRepository.save(run);
            emitWorkflowCompletedEvent(run, "FAILED");
            meterRegistry.counter("flowforge.workflow.runs.failed").increment();
            return;
        }

        // 3. Handle success (transition downstream)
        List<DagValidator.Edge> outgoing = graph.edges.stream()
                .filter(e -> nodeExec.getNodeId().equals(e.from))
                .collect(Collectors.toList());

        for (DagValidator.Edge edge : outgoing) {
            boolean condResult = evaluateCondition(edge.condition, httpStatus, finalStatus);
            if (condResult) {
                // Fan-in wait check: evaluate if ALL parent nodes are succeeded
                List<DagValidator.Edge> incomingEdges = graph.edges.stream()
                        .filter(e -> edge.to.equals(e.to))
                        .collect(Collectors.toList());

                boolean allParentsSucceeded = true;
                for (DagValidator.Edge incoming : incomingEdges) {
                    Optional<NodeExecution> parentExec = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), incoming.from);
                    if (parentExec.isEmpty() || parentExec.get().getStatus() != NodeExecutionStatus.SUCCEEDED) {
                        allParentsSucceeded = false;
                        break;
                    }
                }

                if (allParentsSucceeded) {
                    activateNode(run, graph, edge.to);
                } else {
                    meterRegistry.counter("flowforge.workflow.fanin.wait").increment();
                }
            }
        }

        // 4. Verify workflow completion
        checkWorkflowCompletion(run, graph);
    }

    private void activateNode(WorkflowRun run, DagValidator.WorkflowGraph graph, String nodeId) {
        DagValidator.Node node = graph.nodes.stream()
                .filter(n -> nodeId.equals(n.id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Node not found: " + nodeId));

        Instant now = clock.instant();

        if ("JOB".equalsIgnoreCase(node.type)) {
            UUID jobPublicId = UUID.fromString(node.jobPublicId);
            Job job = jobRepository.findByPublicId(jobPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Job in workflow not found: " + jobPublicId));

            NodeExecution nodeExec = new NodeExecution(run, nodeId, NodeExecutionStatus.RUNNING, now);
            nodeExecutionRepository.save(nodeExec);

            emitNodeStartedEvent(run, nodeId, now);

            // Trigger internal Execution
            Execution execution = executionService.triggerExecutionInternal(
                    job,
                    ExecutionTriggerType.WORKFLOW,
                    run.getPublicId().toString(),
                    now
            );

            nodeExec.setExecutionPublicId(execution.getPublicId());
            nodeExecutionRepository.save(nodeExec);

            meterRegistry.counter("flowforge.workflow.node.executions").increment();

        } else if ("END".equalsIgnoreCase(node.type)) {
            NodeExecution nodeExec = new NodeExecution(run, nodeId, NodeExecutionStatus.SUCCEEDED, now);
            nodeExec.succeed(now);
            nodeExecutionRepository.save(nodeExec);

            emitNodeStartedEvent(run, nodeId, now);
            emitNodeCompletedEvent(run, nodeId, "SUCCEEDED", now);

            meterRegistry.counter("flowforge.workflow.node.executions").increment();
        }
    }

    private boolean evaluateCondition(DagValidator.Condition cond, Integer httpStatus, String finalStatus) {
        if (cond == null) {
            return true;
        }

        String fieldValue = null;
        if ("HTTP_STATUS".equalsIgnoreCase(cond.field)) {
            fieldValue = httpStatus != null ? httpStatus.toString() : "";
        } else if ("STATUS".equalsIgnoreCase(cond.field)) {
            fieldValue = finalStatus;
        }

        if (fieldValue == null) {
            return false;
        }

        boolean equals = fieldValue.equalsIgnoreCase(cond.value);
        if ("EQUALS".equalsIgnoreCase(cond.operator)) {
            return equals;
        } else if ("NOT_EQUALS".equalsIgnoreCase(cond.operator)) {
            return !equals;
        }

        return false;
    }

    private void checkWorkflowCompletion(WorkflowRun run, DagValidator.WorkflowGraph graph) {
        List<NodeExecution> executions = nodeExecutionRepository.findByWorkflowRunId(run.getId());

        boolean hasActive = executions.stream().anyMatch(e ->
                e.getStatus() == NodeExecutionStatus.RUNNING || e.getStatus() == NodeExecutionStatus.PENDING);

        if (!hasActive) {
            boolean endReached = executions.stream().anyMatch(e ->
                    e.getNodeId().toLowerCase().startsWith("end") && e.getStatus() == NodeExecutionStatus.SUCCEEDED);

            boolean hasFailed = executions.stream().anyMatch(e ->
                    e.getStatus() == NodeExecutionStatus.FAILED);

            Instant now = clock.instant();

            if (hasFailed) {
                run.fail(now);
                workflowRunRepository.save(run);
                emitWorkflowCompletedEvent(run, "FAILED");
                meterRegistry.counter("flowforge.workflow.runs.failed").increment();
            } else if (endReached) {
                run.succeed(now);
                workflowRunRepository.save(run);
                emitWorkflowCompletedEvent(run, "SUCCEEDED");
                meterRegistry.counter("flowforge.workflow.runs.completed").increment();
                
                // workflow latency metric
                long latency = run.getFinishedAt().toEpochMilli() - run.getStartedAt().toEpochMilli();
                meterRegistry.timer("flowforge.workflow.latency").record(java.time.Duration.ofMillis(latency));
            }
        }
    }

    // --- Outbox Helpers ---

    private void emitWorkflowStartedEvent(WorkflowRun run) {
        com.flowforge.event.dto.WorkflowStartedPayload payload = new com.flowforge.event.dto.WorkflowStartedPayload(
                run.getPublicId(),
                run.getWorkflowDefinition().getPublicId(),
                run.getStartedAt()
        );
        writeOutboxEvent("WORKFLOW_STARTED", payload, 1, run.getPublicId());
    }

    private void emitNodeStartedEvent(WorkflowRun run, String nodeId, Instant now) {
        com.flowforge.event.dto.NodeStartedPayload payload = new com.flowforge.event.dto.NodeStartedPayload(
                run.getPublicId(),
                nodeId,
                now
        );
        writeOutboxEvent("NODE_STARTED", payload, 1, run.getPublicId());
    }

    private void emitNodeCompletedEvent(WorkflowRun run, String nodeId, String status, Instant now) {
        com.flowforge.event.dto.NodeCompletedPayload payload = new com.flowforge.event.dto.NodeCompletedPayload(
                run.getPublicId(),
                nodeId,
                status,
                now
        );
        writeOutboxEvent("NODE_COMPLETED", payload, 1, run.getPublicId());
    }

    private void emitWorkflowCompletedEvent(WorkflowRun run, String status) {
        com.flowforge.event.dto.WorkflowCompletedPayload payload = new com.flowforge.event.dto.WorkflowCompletedPayload(
                run.getPublicId(),
                status,
                run.getFinishedAt()
        );
        writeOutboxEvent("WORKFLOW_COMPLETED", payload, 1, run.getPublicId());
    }

    private void writeOutboxEvent(String eventType, Object payload, int version, UUID aggregateId) {
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }

        OutboxEvent outboxEvent = new OutboxEvent(
                publicIdGenerator.generate(),
                OutboxAggregateType.EXECUTION,
                aggregateId,
                eventType,
                jsonPayload,
                version,
                clock.instant()
        );
        outboxEventRepository.saveAndFlush(outboxEvent);
    }
}
