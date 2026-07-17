package com.datausher.platform.shared.time.core;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemClockTest {
    @Test
    void usesUtcByDefault() {
        assertEquals(ZoneOffset.UTC, new SystemClock().zone());
    }
}
