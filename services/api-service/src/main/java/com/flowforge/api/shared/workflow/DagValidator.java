package com.flowforge.api.shared.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class DagValidator {

    public static class Node {
        public String id;
        public String type; 
        public String jobPublicId; 
    }

    public static class Edge {
        public String from;
        public String to;
        public Condition condition;
    }

    public static class Condition {
        public String field;     
        public String operator;  
        public String value;
    }

    public static class WorkflowGraph {
        public List<Node> nodes = new ArrayList<>();
        public List<Edge> edges = new ArrayList<>();
    }

    public static void validate(String definitionJson, ObjectMapper objectMapper) {
        WorkflowGraph graph;
        try {
            graph = objectMapper.readValue(definitionJson, WorkflowGraph.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid workflow definition JSON format", e);
        }

        if (graph.nodes == null || graph.nodes.isEmpty()) {
            throw new IllegalArgumentException("Workflow must contain at least one node");
        }

        Set<String> nodeIds = new HashSet<>();
        int startCount = 0;
        int endCount = 0;
        for (Node node : graph.nodes) {
            if (node.id == null || node.id.trim().isEmpty()) {
                throw new IllegalArgumentException("Node ID cannot be null or empty");
            }
            if (!nodeIds.add(node.id)) {
                throw new IllegalArgumentException("Duplicate node ID found: " + node.id);
            }
            if ("START".equalsIgnoreCase(node.type)) {
                startCount++;
            } else if ("END".equalsIgnoreCase(node.type)) {
                endCount++;
            } else if (!"JOB".equalsIgnoreCase(node.type)) {
                throw new IllegalArgumentException("Unsupported node type: " + node.type);
            }
        }

        if (startCount != 1) {
            throw new IllegalArgumentException("Workflow must have exactly one START node. Found: " + startCount);
        }
        if (endCount < 1) {
            throw new IllegalArgumentException("Workflow must have at least one END node");
        }

        Map<String, List<String>> adjList = new HashMap<>();
        Map<String, List<String>> reverseAdjList = new HashMap<>();
        for (String id : nodeIds) {
            adjList.put(id, new ArrayList<>());
            reverseAdjList.put(id, new ArrayList<>());
        }

        for (Edge edge : graph.edges) {
            if (edge.from == null || edge.to == null) {
                throw new IllegalArgumentException("Edge from/to cannot be null");
            }
            if (!nodeIds.contains(edge.from)) {
                throw new IllegalArgumentException("Edge references non-existent from node: " + edge.from);
            }
            if (!nodeIds.contains(edge.to)) {
                throw new IllegalArgumentException("Edge references non-existent to node: " + edge.to);
            }
            adjList.get(edge.from).add(edge.to);
            reverseAdjList.get(edge.to).add(edge.from);
        }

        String startNodeId = graph.nodes.stream()
                .filter(n -> "START".equalsIgnoreCase(n.type))
                .findFirst().orElseThrow().id;

        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        for (String node : nodeIds) {
            if (hasCycle(node, adjList, visited, recStack)) {
                throw new IllegalArgumentException("Cycle detected in workflow graph");
            }
        }

        Set<String> reachableFromStart = new HashSet<>();
        dfsReachable(startNodeId, adjList, reachableFromStart);
        for (String nodeId : nodeIds) {
            if (!reachableFromStart.contains(nodeId)) {
                throw new IllegalArgumentException("Unreachable node from START: " + nodeId);
            }
        }
    }

    private static boolean hasCycle(String node, Map<String, List<String>> adjList, Set<String> visited, Set<String> recStack) {
        if (recStack.contains(node)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }

        visited.add(node);
        recStack.add(node);

        for (String neighbor : adjList.get(node)) {
            if (hasCycle(neighbor, adjList, visited, recStack)) {
                return true;
            }
        }

        recStack.remove(node);
        return false;
    }

    private static void dfsReachable(String node, Map<String, List<String>> adjList, Set<String> reachable) {
        if (reachable.add(node)) {
            for (String neighbor : adjList.get(node)) {
                dfsReachable(neighbor, adjList, reachable);
            }
        }
    }
}
