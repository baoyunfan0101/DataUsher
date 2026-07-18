package com.datausher.workflow.api;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowContractTest {
    @Test
    void rejectsCyclicTaskGraphs() {
        WorkflowTaskDefinition first = task("first");
        WorkflowTaskDefinition second = task("second");

        assertThrows(IllegalArgumentException.class, () -> new WorkflowVersionSpec(
                List.of(first, second),
                List.of(
                        new TaskDependency("first", "second", TaskDependencyCondition.ON_SUCCESS),
                        new TaskDependency("second", "first", TaskDependencyCondition.ON_SUCCESS)),
                Optional.empty(), Map.of()));
    }

    @Test
    void keepsTaskActionsAndRuntimeOwnershipOpenForExtension() {
        WorkflowTaskDefinition task = new WorkflowTaskDefinition(
                "sensor", "Sensor", new FixtureAction(new WorkflowTaskType("sensor")),
                TaskRetryPolicy.NONE, Duration.ofMinutes(5), Map.of());
        WorkflowRuntimeBinding binding = WorkflowRuntimeBinding.schedulerManaged(
                "scheduler", "binding", Map.of());
        WorkflowVersionSpec specification = new WorkflowVersionSpec(
                List.of(task), List.of(), List.of(
                        schedule("hourly", "0 * * * *"),
                        schedule("daily", "0 0 * * *")), binding, Map.of());

        assertEquals("sensor", specification.tasks().getFirst().action().taskType().value());
        assertEquals(2, specification.schedules().size());
        assertEquals(WorkflowRuntimeType.SCHEDULER_MANAGED,
                specification.runtimeBinding().runtimeType());
    }

    @Test
    void requiresSchedulerBindingsOnlyForSchedulerManagedRuntimes() {
        assertThrows(IllegalArgumentException.class, () -> new WorkflowRuntimeBinding(
                WorkflowRuntimeType.SCHEDULER_MANAGED, Optional.empty(), Optional.empty(), Map.of()));
    }

    private static WorkflowSchedule schedule(String id, String expression) {
        return new WorkflowSchedule(
                new WorkflowScheduleId(id), new WorkflowScheduleType("cron"), expression,
                ZoneId.of("UTC"), WorkflowScheduleStatus.ENABLED, Map.of());
    }

    private record FixtureAction(WorkflowTaskType taskType) implements WorkflowTaskAction {
    }

    private static WorkflowTaskDefinition task(String key) {
        return new WorkflowTaskDefinition(
                key, key,
                new ExecutionSpecification(
                        new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                        new ExecutionWorkload(new ExecutionWorkloadType("fixture"), key, Map.of(), Map.of()),
                        ExecutionResultMode.DISCARD, 100),
                TaskRetryPolicy.NONE, Duration.ofMinutes(10), Map.of());
    }
}
