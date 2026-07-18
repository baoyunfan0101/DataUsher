package com.datausher.workflow.api;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
