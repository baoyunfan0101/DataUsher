package com.datausher.workflow.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record TaskInstanceStateChangedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        TaskInstanceState previousState,
        TaskInstance taskInstance
) implements DomainEvent {
    public TaskInstanceStateChangedEvent {
        eventId = requireText(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        previousState = Objects.requireNonNull(previousState, "previousState must not be null");
        taskInstance = Objects.requireNonNull(taskInstance, "taskInstance must not be null");
        if (previousState == taskInstance.state()) {
            throw new IllegalArgumentException("task instance state must change");
        }
    }

    @Override
    public String eventType() {
        return switch (taskInstance.state()) {
            case READY -> WorkflowEvents.TASK_READY;
            case QUEUED -> WorkflowEvents.TASK_SUBMITTED;
            case RUNNING -> WorkflowEvents.TASK_STARTED;
            case RETRY_WAIT -> WorkflowEvents.TASK_RETRY_WAIT;
            case SUCCEEDED -> WorkflowEvents.TASK_COMPLETED;
            case FAILED -> WorkflowEvents.TASK_FAILED;
            case TIMED_OUT -> WorkflowEvents.TASK_TIMED_OUT;
            case CANCELLED -> WorkflowEvents.TASK_CANCELLED;
            case SKIPPED -> WorkflowEvents.TASK_SKIPPED;
            case WAITING -> throw new IllegalStateException("waiting is an initial task state");
        };
    }

    @Override
    public String sourceModule() {
        return "workflow-core";
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
