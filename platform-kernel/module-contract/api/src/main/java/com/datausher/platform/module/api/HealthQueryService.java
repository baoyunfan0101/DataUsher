package com.datausher.platform.module.api;

import java.util.List;

public interface HealthQueryService {
    PlatformHealth platformHealth();

    List<ModuleHealth> moduleHealth();
}
