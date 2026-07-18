package com.datausher.workflow.core;

import com.datausher.execution.api.CancelExecutionRequest;
import com.datausher.execution.api.ExecutionCommandService;
import com.datausher.execution.api.ExecutionOrigin;
import com.datausher.execution.api.ExecutionOriginType;
import com.datausher.execution.api.ExecutionQueryService;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.workflow.api.ExecutionWorkflowTaskAction;
import com.datausher.workflow.api.WorkflowTaskRunReference;
import com.datausher.workflow.api.WorkflowTaskRunReferenceType;
import com.datausher.workflow.api.WorkflowTaskType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ExecutionWorkflowTaskExecutor implements WorkflowTaskExecutor {
    private final ExecutionCommandService executions;
    private final ExecutionQueryService executionQueries;

    public ExecutionWorkflowTaskExecutor(
            ExecutionCommandService executions,
            ExecutionQueryService executionQueries
    ) {
        this.executions = Objects.requireNonNull(executions, "executions must not be null");
        this.executionQueries = Objects.requireNonNull(
                executionQueries, "executionQueries must not be null");
    }

    @Override
    public WorkflowTaskType taskType() {
        return WorkflowTaskType.EXECUTION;
    }

    @Override
    public WorkflowTaskRunReference dispatch(WorkflowTaskDispatchRequest request) {
        if (!(request.taskDefinition().action() instanceof ExecutionWorkflowTaskAction action)) {
            throw new IllegalArgumentException("execution task requires ExecutionWorkflowTaskAction");
        }
        ExecutionSpecification specification = withParameters(
                action.executionSpecification(), request.workflowInstance().parameters());
        ExecutionRequest execution = executions.submit(new SubmitExecutionRequest(
                specification,
                executionKey(request),
                new ExecutionOrigin(
                        ExecutionOriginType.WORKFLOW_TASK,
                        request.taskInstance().taskInstanceId().value(),
                        Integer.toString(request.taskInstance().attempt()),
                        Map.of("workflowInstanceId", request.workflowInstance().instanceId().value(),
                                "taskKey", request.taskInstance().taskKey())),
                request.requestContext()));
        return new WorkflowTaskRunReference(
                WorkflowTaskRunReferenceType.EXECUTION,
                execution.requestId().value(), Map.of());
    }

    @Override
    public void cancel(WorkflowTaskCancelRequest request) {
        request.taskInstance().executionRequestId()
                .flatMap(executionQueries::findRequest)
                .filter(execution -> !execution.state().terminal())
                .ifPresent(execution -> executions.cancel(new CancelExecutionRequest(
                        execution.requestId(), execution.revision(), request.requestContext())));
    }

    private static ExecutionSpecification withParameters(
            ExecutionSpecification specification,
            Map<String, ExecutionValue> workflowParameters
    ) {
        Map<String, ExecutionValue> parameters = new HashMap<>(specification.workload().parameters());
        parameters.putAll(workflowParameters);
        ExecutionWorkload workload = new ExecutionWorkload(
                specification.workload().type(), specification.workload().payload(),
                parameters, specification.workload().options());
        return new ExecutionSpecification(
                specification.queueId(), specification.accountId(), workload,
                specification.resultMode(), specification.resultPageSize());
    }

    private static String executionKey(WorkflowTaskDispatchRequest request) {
        return request.workflowInstance().instanceId().value() + ":"
                + request.taskInstance().taskKey() + ":" + request.taskInstance().attempt();
    }
}
