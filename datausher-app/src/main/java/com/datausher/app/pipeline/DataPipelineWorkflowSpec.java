package com.datausher.app.pipeline;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.workflow.api.TaskDependency;
import com.datausher.workflow.api.WorkflowRuntimeBinding;
import com.datausher.workflow.api.WorkflowSchedule;
import com.datausher.workflow.api.WorkflowTaskDefinition;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DataPipelineWorkflowSpec(
        String pipelineId,
        DataPipelineEngineBoundary engineBoundary,
        List<WorkflowTaskDefinition> tasks,
        List<TaskDependency> dependencies,
        List<WorkflowSchedule> schedules,
        WorkflowRuntimeBinding runtimeBinding,
        Map<String, String> attributes
) {
    public DataPipelineWorkflowSpec {
        pipelineId = IntegrationIdentifiers.normalize(pipelineId, "pipelineId");
        engineBoundary = Objects.requireNonNull(engineBoundary, "engineBoundary must not be null");
        tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks must not be null"));
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        schedules = schedules == null ? List.of() : List.copyOf(schedules);
        runtimeBinding = runtimeBinding == null
                ? WorkflowRuntimeBinding.PLATFORM_MANAGED : runtimeBinding;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("tasks must not be empty");
        }
        DataPipelineWorkflowFactory.requireNoManagedAttributes(attributes);
    }
}
