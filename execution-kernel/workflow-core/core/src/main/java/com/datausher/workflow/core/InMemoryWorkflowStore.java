package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowDefinition;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowVersion;
import com.datausher.platform.shared.concurrent.RevisionConflictException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryWorkflowStore implements WorkflowStore {
    private final Map<WorkflowId, WorkflowDefinition> workflows = new HashMap<>();
    private final Map<String, WorkflowVersion> versions = new HashMap<>();

    @Override
    public synchronized void createWorkflow(WorkflowDefinition workflow) {
        if (workflows.putIfAbsent(workflow.workflowId(), workflow) != null) {
            throw new IllegalStateException("workflow already exists: " + workflow.workflowId());
        }
    }

    @Override
    public synchronized void deleteWorkflow(WorkflowDefinition workflow) {
        if (!workflows.remove(workflow.workflowId(), workflow)) {
            throw new IllegalStateException("workflow changed before rollback: " + workflow.workflowId());
        }
    }

    @Override
    public synchronized void createVersion(
            WorkflowDefinition expectedWorkflow,
            WorkflowDefinition updatedWorkflow,
            WorkflowVersion version
    ) {
        if (!workflows.replace(expectedWorkflow.workflowId(), expectedWorkflow, updatedWorkflow)) {
            WorkflowDefinition actual = workflows.get(expectedWorkflow.workflowId());
            if (actual != null) {
                throw new RevisionConflictException(
                        "workflow", expectedWorkflow.workflowId().value(),
                        expectedWorkflow.revision(), actual.revision());
            }
            throw new IllegalStateException("workflow no longer exists: " + expectedWorkflow.workflowId());
        }
        String key = versionKey(version.workflowId(), version.version());
        if (versions.putIfAbsent(key, version) != null) {
            workflows.put(expectedWorkflow.workflowId(), expectedWorkflow);
            throw new IllegalStateException("workflow version already exists: " + key);
        }
    }

    @Override
    public synchronized void deleteVersion(
            WorkflowDefinition expectedWorkflow,
            WorkflowDefinition restoredWorkflow,
            WorkflowVersion version
    ) {
        if (!workflows.replace(expectedWorkflow.workflowId(), expectedWorkflow, restoredWorkflow)
                || !versions.remove(versionKey(version.workflowId(), version.version()), version)) {
            throw new IllegalStateException("workflow version changed before rollback");
        }
    }

    @Override
    public synchronized Optional<WorkflowDefinition> findWorkflow(WorkflowId workflowId) {
        return Optional.ofNullable(workflows.get(workflowId));
    }

    @Override
    public synchronized Optional<WorkflowVersion> findVersion(WorkflowId workflowId, long version) {
        return Optional.ofNullable(versions.get(versionKey(workflowId, version)));
    }

    @Override
    public synchronized List<WorkflowVersion> listVersions(WorkflowId workflowId) {
        return versions.values().stream()
                .filter(version -> version.workflowId().equals(workflowId))
                .sorted(Comparator.comparingLong(WorkflowVersion::version))
                .toList();
    }

    private static String versionKey(WorkflowId workflowId, long version) {
        return workflowId.value() + "@" + version;
    }
}
