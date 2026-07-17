package com.datausher.platform.module.api;

public interface ModuleHealthContributor {
    String moduleName();

    ModuleHealth health();
}
