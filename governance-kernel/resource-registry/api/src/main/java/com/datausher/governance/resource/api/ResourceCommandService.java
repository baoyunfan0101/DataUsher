package com.datausher.governance.resource.api;

public interface ResourceCommandService {
    RegisteredResource register(RegisterResourceRequest request);

    RegisteredResource changeLifecycle(ChangeResourceLifecycleRequest request);
}
