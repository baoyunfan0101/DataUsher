package com.datausher.integration.runtime.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public sealed interface IntegrationValue {
    record NullValue() implements IntegrationValue {
    }

    record TextValue(String value) implements IntegrationValue {
        public TextValue {
            value = Objects.requireNonNull(value, "value must not be null");
        }
    }

    record BooleanValue(boolean value) implements IntegrationValue {
    }

    record DecimalValue(BigDecimal value) implements IntegrationValue {
        public DecimalValue {
            value = Objects.requireNonNull(value, "value must not be null");
        }

        public DecimalValue(long value) {
            this(BigDecimal.valueOf(value));
        }

        public DecimalValue(double value) {
            this(BigDecimal.valueOf(value));
        }
    }

    record BinaryValue(String base64) implements IntegrationValue {
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

    record InstantValue(Instant value) implements IntegrationValue {
        public InstantValue {
            value = Objects.requireNonNull(value, "value must not be null");
        }
    }

    record DateValue(LocalDate value) implements IntegrationValue {
        public DateValue {
            value = Objects.requireNonNull(value, "value must not be null");
        }
    }

    record DateTimeValue(LocalDateTime value) implements IntegrationValue {
        public DateTimeValue {
            value = Objects.requireNonNull(value, "value must not be null");
        }
    }

    record ArrayValue(List<IntegrationValue> values) implements IntegrationValue {
        public ArrayValue {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }

    record ObjectValue(Map<String, IntegrationValue> values) implements IntegrationValue {
        public ObjectValue {
            values = values == null ? Map.of() : Map.copyOf(values);
            values.keySet().forEach(key ->
                    IntegrationIdentifiers.requireText(key, "object value key"));
        }
    }
}
