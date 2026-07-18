package com.datausher.execution.core;

import com.datausher.execution.api.ExecutionValue;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.Map;
import java.util.stream.Collectors;

final class ExecutionValueMapper {
    private ExecutionValueMapper() {
    }

    static IntegrationValue toIntegration(ExecutionValue value) {
        return switch (value) {
            case ExecutionValue.NullValue ignored -> new IntegrationValue.NullValue();
            case ExecutionValue.TextValue text -> new IntegrationValue.TextValue(text.value());
            case ExecutionValue.BooleanValue bool ->
                    new IntegrationValue.BooleanValue(bool.value());
            case ExecutionValue.DecimalValue decimal ->
                    new IntegrationValue.DecimalValue(decimal.value());
            case ExecutionValue.BinaryValue binary ->
                    new IntegrationValue.BinaryValue(binary.base64());
            case ExecutionValue.InstantValue instant ->
                    new IntegrationValue.InstantValue(instant.value());
            case ExecutionValue.DateValue date -> new IntegrationValue.DateValue(date.value());
            case ExecutionValue.DateTimeValue dateTime ->
                    new IntegrationValue.DateTimeValue(dateTime.value());
            case ExecutionValue.ArrayValue array -> new IntegrationValue.ArrayValue(
                    array.values().stream().map(ExecutionValueMapper::toIntegration).toList());
            case ExecutionValue.ObjectValue object -> new IntegrationValue.ObjectValue(
                    object.values().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> toIntegration(entry.getValue())
                    )));
        };
    }

    static ExecutionValue fromIntegration(IntegrationValue value) {
        return switch (value) {
            case IntegrationValue.NullValue ignored -> new ExecutionValue.NullValue();
            case IntegrationValue.TextValue text -> new ExecutionValue.TextValue(text.value());
            case IntegrationValue.BooleanValue bool ->
                    new ExecutionValue.BooleanValue(bool.value());
            case IntegrationValue.DecimalValue decimal ->
                    new ExecutionValue.DecimalValue(decimal.value());
            case IntegrationValue.BinaryValue binary ->
                    new ExecutionValue.BinaryValue(binary.base64());
            case IntegrationValue.InstantValue instant ->
                    new ExecutionValue.InstantValue(instant.value());
            case IntegrationValue.DateValue date -> new ExecutionValue.DateValue(date.value());
            case IntegrationValue.DateTimeValue dateTime ->
                    new ExecutionValue.DateTimeValue(dateTime.value());
            case IntegrationValue.ArrayValue array -> new ExecutionValue.ArrayValue(
                    array.values().stream().map(ExecutionValueMapper::fromIntegration).toList());
            case IntegrationValue.ObjectValue object -> new ExecutionValue.ObjectValue(
                    object.values().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> fromIntegration(entry.getValue())
                    )));
        };
    }
}
