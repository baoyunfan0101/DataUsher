package com.datausher.platform.config.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigValueTest {
    @Test
    void parsesSupportedTypedValuesStrictly() {
        assertEquals(true, value(" TRUE ").asBoolean());
        assertEquals(42, value(" 42 ").asInt());
        assertEquals(Duration.ofSeconds(5), value(" PT5S ").asDuration());
    }

    @Test
    void rejectsMalformedBooleanInsteadOfTreatingItAsFalse() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> value("ture").asBoolean()
        );

        assertEquals("configuration feature.enabled from test is not a valid boolean", failure.getMessage());
    }

    private static ConfigValue value(String value) {
        return new ConfigValue(ConfigKey.of("feature.enabled"), value, "test");
    }
}
