package com.datausher.data.quality.api;

public interface ProfileCommandService {
    ProfileJob start(StartProfileJobRequest request);

    ProfileJob cancel(CancelProfileJobRequest request);
}
