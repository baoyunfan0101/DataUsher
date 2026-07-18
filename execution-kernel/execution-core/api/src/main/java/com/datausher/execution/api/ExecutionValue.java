package com.datausher.execution.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public sealed interface ExecutionValue {
    record NullValue() implements ExecutionValue {
    }

    record TextValue(String value) implements ExecutionValue {
        public TextValue {
            value = Objects.requireNonNull(value, "value must not be null");
        }
    }

    record BooleanValue(boolean value) implements ExecutionValue {
    }

    record DecimalValue(BigDecimal value) implements ExecutionValue {
        public DecimalValue {
            value = Objects.requireNonNull(value, "value must not be null");
        }

        public DecimalValue(long value) {
            this(BigDecimal.valueOf(value));
        }
    }

    record BinaryValue(String base64) implements ExecutionValue {
        public BinaryValue {
            base64 = Objects.requireNonNull(base64, "base64 must not be null");
            Base64.getDecoder().decode(base64);
        }

        public static BinaryValue fromBytes(byte[] value) {
            Objects.requireNonNull(value, "value must not be null");
            return new BinaryValue(Base64.getEncoder().encodeToString(value));
        }

        public byte[] bytes() {
            return Base64.getDecoder().decode(base64);
        }
    }

    record InstantValue(Instant value) implements ExecutionValue {
        public InstantValue {
            value = Objects.requireNonNull(value, "value must not be null");
        }
    }

    record DateValue(LocalDate value) implements ExecutionValue {
        public DateValue {
            value = Objects.requireNonNull(value, "value must not be null");
        }
    }

    record DateTimeValue(LocalDateTime value) implements ExecutionValue {
        public DateTimeValue {
            value = Objects.requireNonNull(value, "value must not be null");
        }
    }

    record ArrayValue(List<ExecutionValue> values) implements ExecutionValue {
        public ArrayValue {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }

    record ObjectValue(Map<String, ExecutionValue> values) implements ExecutionValue {
        public ObjectValue {
            values = values == null ? Map.of() : Map.copyOf(values);
        }
    }
}
