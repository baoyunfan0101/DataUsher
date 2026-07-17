package com.datausher.platform.shared.time;

import java.time.Instant;
import java.time.ZoneId;

public interface Clock {
    Instant now();

    ZoneId zone();
}
