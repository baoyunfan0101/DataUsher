package com.datausher.workflow.core;

import com.datausher.execution.api.ExecutionValue;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.Map;
import java.util.stream.Collectors;

final class WorkflowIntegrationValues {
    private WorkflowIntegrationValues() {
    }

    static IntegrationValue convert(ExecutionValue value) {
        return switch (value) {
            case ExecutionValue.NullValue ignored -> new IntegrationValue.NullValue();
            case ExecutionValue.TextValue text -> new IntegrationValue.TextValue(text.value());
            case ExecutionValue.BooleanValue bool -> new IntegrationValue.BooleanValue(bool.value());
            case ExecutionValue.DecimalValue decimal -> new IntegrationValue.DecimalValue(decimal.value());
            case ExecutionValue.InstantValue instant -> new IntegrationValue.InstantValue(instant.value());
            case ExecutionValue.DateValue date -> new IntegrationValue.DateValue(date.value());
            case ExecutionValue.DateTimeValue dateTime -> new IntegrationValue.DateTimeValue(dateTime.value());
            case ExecutionValue.BinaryValue binary -> new IntegrationValue.BinaryValue(binary.base64());
            case ExecutionValue.ArrayValue array -> new IntegrationValue.ArrayValue(
                    array.values().stream().map(WorkflowIntegrationValues::convert).toList());
            case ExecutionValue.ObjectValue object -> new IntegrationValue.ObjectValue(
                    object.values().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey, entry -> convert(entry.getValue()))));
        };
    }
}
