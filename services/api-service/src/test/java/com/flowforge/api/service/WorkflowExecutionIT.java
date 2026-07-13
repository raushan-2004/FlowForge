package com.flowforge.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.api.BasePersistenceIT;
import com.flowforge.api.model.*;
import com.flowforge.api.repository.*;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WorkflowExecutionIT extends BasePersistenceIT {

    @Autowired
    private WorkflowEngineService workflowEngineService;

    @Autowired
    private WorkflowDefinitionRepository definitionRepository;

    @Autowired
    private WorkflowRunRepository workflowRunRepository;

    @Autowired
    private NodeExecutionRepository nodeExecutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    @Autowired
    private Clock clock;

    @Autowired
    private ObjectMapper objectMapper;

    private Tenant tenant;
    private Project project;
    private Job job1;
    private Job job2;
    private Job job3;

    @BeforeEach
    public void setupData() {
        User user = new User(publicIdGenerator.generate(), "john@example.com", "password123", UserStatus.ACTIVE);
        userRepository.saveAndFlush(user);

        tenant = new Tenant(publicIdGenerator.generate(), "Tenant 1", TenantStatus.ACTIVE, user.getPublicId());
        tenantRepository.saveAndFlush(tenant);

        project = new Project(publicIdGenerator.generate(), tenant, "Project 1", ProjectStatus.ACTIVE, user.getPublicId());
        projectRepository.saveAndFlush(project);

        job1 = new Job(
                publicIdGenerator.generate(), project, "Job 1", "Desc", true,
                JobHttpMethod.GET, "http://trusted.com", null, null, 30,
                null, null, null, JobScheduleType.MANUAL, null, JobStatus.ACTIVE, user.getPublicId()
        );
        jobRepository.saveAndFlush(job1);

        job2 = new Job(
                publicIdGenerator.generate(), project, "Job 2", "Desc", true,
                JobHttpMethod.POST, "http://trusted2.com", null, null, 30,
                null, null, null, JobScheduleType.MANUAL, null, JobStatus.ACTIVE, user.getPublicId()
        );
        jobRepository.saveAndFlush(job2);

        job3 = new Job(
                publicIdGenerator.generate(), project, "Job 3", "Desc", true,
                JobHttpMethod.GET, "http://trusted3.com", null, null, 30,
                null, null, null, JobScheduleType.MANUAL, null, JobStatus.ACTIVE, user.getPublicId()
        );
        jobRepository.saveAndFlush(job3);
    }

    @Test
    public void testCycleDetection() {
        // Create cyclic graph: start -> job1 -> job2 -> job1 -> end
        String cyclicJson = "{\n" +
                "  \"nodes\": [\n" +
                "    {\"id\": \"start-1\", \"type\": \"START\"},\n" +
                "    {\"id\": \"job-1\", \"type\": \"JOB\", \"jobPublicId\": \"" + job1.getPublicId() + "\"},\n" +
                "    {\"id\": \"job-2\", \"type\": \"JOB\", \"jobPublicId\": \"" + job2.getPublicId() + "\"},\n" +
                "    {\"id\": \"end-1\", \"type\": \"END\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"from\": \"start-1\", \"to\": \"job-1\"},\n" +
                "    {\"from\": \"job-1\", \"to\": \"job-2\"},\n" +
                "    {\"from\": \"job-2\", \"to\": \"job-1\"},\n" +
                "    {\"from\": \"job-2\", \"to\": \"end-1\"}\n" +
                "  ]\n" +
                "}";

        assertThatThrownBy(() -> workflowEngineService.createDefinition(project.getPublicId(), "Cyclic Flow", cyclicJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cycle detected");
    }

    @Test
    public void testLinearWorkflowExecution() {
        // start -> job1 -> job2 -> end
        String linearJson = "{\n" +
                "  \"nodes\": [\n" +
                "    {\"id\": \"start-1\", \"type\": \"START\"},\n" +
                "    {\"id\": \"job-1\", \"type\": \"JOB\", \"jobPublicId\": \"" + job1.getPublicId() + "\"},\n" +
                "    {\"id\": \"job-2\", \"type\": \"JOB\", \"jobPublicId\": \"" + job2.getPublicId() + "\"},\n" +
                "    {\"id\": \"end-1\", \"type\": \"END\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"from\": \"start-1\", \"to\": \"job-1\"},\n" +
                "    {\"from\": \"job-1\", \"to\": \"job-2\"},\n" +
                "    {\"from\": \"job-2\", \"to\": \"end-1\"}\n" +
                "  ]\n" +
                "}";

        WorkflowDefinition def = workflowEngineService.createDefinition(project.getPublicId(), "Linear Flow", linearJson);
        assertThat(def.getVersion()).isEqualTo(1);

        // Start Run
        WorkflowRun run = workflowEngineService.startWorkflowRun(def.getPublicId());
        assertThat(run.getStatus()).isEqualTo(WorkflowRunStatus.RUNNING);

        // Check START node is succeeded, and JOB-1 node is running
        List<NodeExecution> nodeExecs = nodeExecutionRepository.findByWorkflowRunId(run.getId());
        assertThat(nodeExecs).hasSize(2);

        NodeExecution startExec = nodeExecs.stream().filter(e -> "start-1".equals(e.getNodeId())).findFirst().orElseThrow();
        assertThat(startExec.getStatus()).isEqualTo(NodeExecutionStatus.SUCCEEDED);

        NodeExecution job1Exec = nodeExecs.stream().filter(e -> "job-1".equals(e.getNodeId())).findFirst().orElseThrow();
        assertThat(job1Exec.getStatus()).isEqualTo(NodeExecutionStatus.RUNNING);
        UUID job1ExecId = job1Exec.getExecutionPublicId();
        assertThat(job1ExecId).isNotNull();

        // Complete JOB-1
        workflowEngineService.processNodeCompletion(job1ExecId, "SUCCEEDED", 200, null);

        // Verify JOB-1 node succeeded and JOB-2 activated
        NodeExecution job1ExecUpdated = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-1").orElseThrow();
        assertThat(job1ExecUpdated.getStatus()).isEqualTo(NodeExecutionStatus.SUCCEEDED);

        NodeExecution job2Exec = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-2").orElseThrow();
        assertThat(job2Exec.getStatus()).isEqualTo(NodeExecutionStatus.RUNNING);
        UUID job2ExecId = job2Exec.getExecutionPublicId();

        // Complete JOB-2
        workflowEngineService.processNodeCompletion(job2ExecId, "SUCCEEDED", 201, null);

        // Verify Workflow succeeded
        WorkflowRun runUpdated = workflowRunRepository.findById(run.getId()).orElseThrow();
        assertThat(runUpdated.getStatus()).isEqualTo(WorkflowRunStatus.SUCCEEDED);

        NodeExecution endExec = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "end-1").orElseThrow();
        assertThat(endExec.getStatus()).isEqualTo(NodeExecutionStatus.SUCCEEDED);

        // Verify outbox workflow completed event generated
        boolean hasWorkflowCompleted = outboxEventRepository.findAll().stream()
                .anyMatch(e -> "WORKFLOW_COMPLETED".equals(e.getEventType()));
        assertThat(hasWorkflowCompleted).isTrue();
    }

    @Test
    public void testFanOutWorkflowExecution() {
        // start -> job1 and job2 concurrently -> end
        String fanOutJson = "{\n" +
                "  \"nodes\": [\n" +
                "    {\"id\": \"start-1\", \"type\": \"START\"},\n" +
                "    {\"id\": \"job-1\", \"type\": \"JOB\", \"jobPublicId\": \"" + job1.getPublicId() + "\"},\n" +
                "    {\"id\": \"job-2\", \"type\": \"JOB\", \"jobPublicId\": \"" + job2.getPublicId() + "\"},\n" +
                "    {\"id\": \"end-1\", \"type\": \"END\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"from\": \"start-1\", \"to\": \"job-1\"},\n" +
                "    {\"from\": \"start-1\", \"to\": \"job-2\"},\n" +
                "    {\"from\": \"job-1\", \"to\": \"end-1\"},\n" +
                "    {\"from\": \"job-2\", \"to\": \"end-1\"}\n" +
                "  ]\n" +
                "}";

        WorkflowDefinition def = workflowEngineService.createDefinition(project.getPublicId(), "FanOut Flow", fanOutJson);
        WorkflowRun run = workflowEngineService.startWorkflowRun(def.getPublicId());

        // Verify both job-1 and job-2 started concurrently
        NodeExecution job1Exec = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-1").orElseThrow();
        NodeExecution job2Exec = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-2").orElseThrow();

        assertThat(job1Exec.getStatus()).isEqualTo(NodeExecutionStatus.RUNNING);
        assertThat(job2Exec.getStatus()).isEqualTo(NodeExecutionStatus.RUNNING);
    }

    @Test
    public void testFanInWorkflowExecution() {
        // start -> job1 and job2 (fan-out) -> job3 (fan-in: requires both job1 & job2 to succeed) -> end
        String fanInJson = "{\n" +
                "  \"nodes\": [\n" +
                "    {\"id\": \"start-1\", \"type\": \"START\"},\n" +
                "    {\"id\": \"job-1\", \"type\": \"JOB\", \"jobPublicId\": \"" + job1.getPublicId() + "\"},\n" +
                "    {\"id\": \"job-2\", \"type\": \"JOB\", \"jobPublicId\": \"" + job2.getPublicId() + "\"},\n" +
                "    {\"id\": \"job-3\", \"type\": \"JOB\", \"jobPublicId\": \"" + job3.getPublicId() + "\"},\n" +
                "    {\"id\": \"end-1\", \"type\": \"END\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"from\": \"start-1\", \"to\": \"job-1\"},\n" +
                "    {\"from\": \"start-1\", \"to\": \"job-2\"},\n" +
                "    {\"from\": \"job-1\", \"to\": \"job-3\"},\n" +
                "    {\"from\": \"job-2\", \"to\": \"job-3\"},\n" +
                "    {\"from\": \"job-3\", \"to\": \"end-1\"}\n" +
                "  ]\n" +
                "}";

        WorkflowDefinition def = workflowEngineService.createDefinition(project.getPublicId(), "FanIn Flow", fanInJson);
        WorkflowRun run = workflowEngineService.startWorkflowRun(def.getPublicId());

        NodeExecution job1Exec = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-1").orElseThrow();
        NodeExecution job2Exec = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-2").orElseThrow();

        // Complete job-1 only
        workflowEngineService.processNodeCompletion(job1Exec.getExecutionPublicId(), "SUCCEEDED", 200, null);

        // Verify job-3 is NOT running yet (waiting for job-2 to succeed)
        assertThat(nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-3")).isEmpty();

        // Complete job-2
        workflowEngineService.processNodeCompletion(job2Exec.getExecutionPublicId(), "SUCCEEDED", 200, null);

        // Verify job-3 is now activated (running)
        NodeExecution job3Exec = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-3").orElseThrow();
        assertThat(job3Exec.getStatus()).isEqualTo(NodeExecutionStatus.RUNNING);
    }

    @Test
    public void testConditionEvaluation() {
        // start -> job1 -> job2 (condition HTTP == 200) and job3 (condition HTTP != 200) -> end
        String condJson = "{\n" +
                "  \"nodes\": [\n" +
                "    {\"id\": \"start-1\", \"type\": \"START\"},\n" +
                "    {\"id\": \"job-1\", \"type\": \"JOB\", \"jobPublicId\": \"" + job1.getPublicId() + "\"},\n" +
                "    {\"id\": \"job-2\", \"type\": \"JOB\", \"jobPublicId\": \"" + job2.getPublicId() + "\"},\n" +
                "    {\"id\": \"job-3\", \"type\": \"JOB\", \"jobPublicId\": \"" + job3.getPublicId() + "\"},\n" +
                "    {\"id\": \"end-1\", \"type\": \"END\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"from\": \"start-1\", \"to\": \"job-1\"},\n" +
                "    {\n" +
                "      \"from\": \"job-1\",\n" +
                "      \"to\": \"job-2\",\n" +
                "      \"condition\": {\"field\": \"HTTP_STATUS\", \"operator\": \"EQUALS\", \"value\": \"200\"}\n" +
                "    },\n" +
                "    {\n" +
                "      \"from\": \"job-1\",\n" +
                "      \"to\": \"job-3\",\n" +
                "      \"condition\": {\"field\": \"HTTP_STATUS\", \"operator\": \"NOT_EQUALS\", \"value\": \"200\"}\n" +
                "    },\n" +
                "    {\"from\": \"job-2\", \"to\": \"end-1\"},\n" +
                "    {\"from\": \"job-3\", \"to\": \"end-1\"}\n" +
                "  ]\n" +
                "}";

        WorkflowDefinition def = workflowEngineService.createDefinition(project.getPublicId(), "Condition Flow", condJson);
        
        // Run Case A: HTTP status 200
        WorkflowRun runA = workflowEngineService.startWorkflowRun(def.getPublicId());
        NodeExecution job1ExecA = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(runA.getId(), "job-1").orElseThrow();
        workflowEngineService.processNodeCompletion(job1ExecA.getExecutionPublicId(), "SUCCEEDED", 200, null);

        // Verify job-2 started and job-3 pruned (not started)
        assertThat(nodeExecutionRepository.findByWorkflowRunIdAndNodeId(runA.getId(), "job-2")).isPresent();
        assertThat(nodeExecutionRepository.findByWorkflowRunIdAndNodeId(runA.getId(), "job-3")).isEmpty();

        // Run Case B: HTTP status 500
        WorkflowRun runB = workflowEngineService.startWorkflowRun(def.getPublicId());
        NodeExecution job1ExecB = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(runB.getId(), "job-1").orElseThrow();
        workflowEngineService.processNodeCompletion(job1ExecB.getExecutionPublicId(), "SUCCEEDED", 500, null);

        // Verify job-3 started and job-2 pruned
        assertThat(nodeExecutionRepository.findByWorkflowRunIdAndNodeId(runB.getId(), "job-3")).isPresent();
        assertThat(nodeExecutionRepository.findByWorkflowRunIdAndNodeId(runB.getId(), "job-2")).isEmpty();
    }

    @Test
    public void testDuplicateNodeCompletionIdempotency() {
        String linearJson = "{\n" +
                "  \"nodes\": [\n" +
                "    {\"id\": \"start-1\", \"type\": \"START\"},\n" +
                "    {\"id\": \"job-1\", \"type\": \"JOB\", \"jobPublicId\": \"" + job1.getPublicId() + "\"},\n" +
                "    {\"id\": \"end-1\", \"type\": \"END\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"from\": \"start-1\", \"to\": \"job-1\"},\n" +
                "    {\"from\": \"job-1\", \"to\": \"end-1\"}\n" +
                "  ]\n" +
                "}";

        WorkflowDefinition def = workflowEngineService.createDefinition(project.getPublicId(), "Idempotent Flow", linearJson);
        WorkflowRun run = workflowEngineService.startWorkflowRun(def.getPublicId());

        NodeExecution job1Exec = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-1").orElseThrow();
        
        // Process first completion
        workflowEngineService.processNodeCompletion(job1Exec.getExecutionPublicId(), "SUCCEEDED", 200, null);

        // Verify success
        NodeExecution job1ExecUpdated = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-1").orElseThrow();
        assertThat(job1ExecUpdated.getStatus()).isEqualTo(NodeExecutionStatus.SUCCEEDED);

        // Capture count of node executions
        int execCount = nodeExecutionRepository.findByWorkflowRunId(run.getId()).size();

        // Process duplicate completion (should be ignored safely)
        workflowEngineService.processNodeCompletion(job1Exec.getExecutionPublicId(), "SUCCEEDED", 200, null);

        // Verify count remains same and no exception thrown
        assertThat(nodeExecutionRepository.findByWorkflowRunId(run.getId())).hasSize(execCount);
    }

    @Test
    public void testWorkflowFailureCascade() {
        String linearJson = "{\n" +
                "  \"nodes\": [\n" +
                "    {\"id\": \"start-1\", \"type\": \"START\"},\n" +
                "    {\"id\": \"job-1\", \"type\": \"JOB\", \"jobPublicId\": \"" + job1.getPublicId() + "\"},\n" +
                "    {\"id\": \"end-1\", \"type\": \"END\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"from\": \"start-1\", \"to\": \"job-1\"},\n" +
                "    {\"from\": \"job-1\", \"to\": \"end-1\"}\n" +
                "  ]\n" +
                "}";

        WorkflowDefinition def = workflowEngineService.createDefinition(project.getPublicId(), "Cascade Fail Flow", linearJson);
        WorkflowRun run = workflowEngineService.startWorkflowRun(def.getPublicId());

        NodeExecution job1Exec = nodeExecutionRepository.findByWorkflowRunIdAndNodeId(run.getId(), "job-1").orElseThrow();
        
        // Complete JOB-1 with FAILED status
        workflowEngineService.processNodeCompletion(job1Exec.getExecutionPublicId(), "FAILED", 500, "CONNECTION_TIMEOUT");

        // Verify Workflow Run status is FAILED
        WorkflowRun runUpdated = workflowRunRepository.findById(run.getId()).orElseThrow();
        assertThat(runUpdated.getStatus()).isEqualTo(WorkflowRunStatus.FAILED);
    }
}
