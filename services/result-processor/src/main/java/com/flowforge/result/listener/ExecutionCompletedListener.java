package com.flowforge.result.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.event.dto.ExecutionCompletedPayload;
import com.flowforge.result.service.RetryEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ExecutionCompletedListener {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionCompletedListener.class);

    private final ObjectMapper objectMapper;
    private final RetryEngineService retryEngineService;

    public ExecutionCompletedListener(ObjectMapper objectMapper, RetryEngineService retryEngineService) {
        this.objectMapper = objectMapper;
        this.retryEngineService = retryEngineService;
    }

    @KafkaListener(topics = "${flowforge.result.topic-execution-completed:execution-completed}", groupId = "${flowforge.result.group-id:result-processor-group}")
    public void onExecutionCompleted(String message) {
        try {
            ExecutionCompletedPayload payload = objectMapper.readValue(message, ExecutionCompletedPayload.class);
            logger.info("Received execution-completed event for execution: {}, status: {}", 
                    payload.getExecutionPublicId(), payload.getFinalStatus());
            
            retryEngineService.processCompletedEvent(payload);
        } catch (Exception e) {
            logger.error("Failed to process execution completed event message: " + message, e);
            throw new RuntimeException("Rollback transaction for message reprocessing", e);
        }
    }
}
