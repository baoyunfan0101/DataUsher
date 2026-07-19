package com.datausher.data.quality.core;

import com.datausher.data.quality.api.ProfileJob;

import java.util.Objects;

public record ProfileJobCreateResult(ProfileJob job, boolean created) {
    public ProfileJobCreateResult {
        job = Objects.requireNonNull(job, "job must not be null");
    }
}
