package com.flowforge.scheduler.model;

public enum OutboxStatus {
    PENDING,
    PUBLISHING,
    PUBLISHED,
    FAILED,
    DEAD
}
