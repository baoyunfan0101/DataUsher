package com.datausher.platform.shared.time.core;

import com.datausher.platform.shared.time.Clock;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

public final class SystemClock implements Clock {
    private final ZoneId zone;

    public SystemClock() {
        this(ZoneOffset.UTC);
    }

    public SystemClock(ZoneId zone) {
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
    }

    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public ZoneId zone() {
        return zone;
    }
}
