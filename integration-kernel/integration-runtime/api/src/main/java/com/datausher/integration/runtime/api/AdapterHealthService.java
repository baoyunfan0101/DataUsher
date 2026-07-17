package com.datausher.integration.runtime.api;

import java.util.List;

public interface AdapterHealthService {
    AdapterHealth check(String adapterId);

    List<AdapterHealth> checkAll();
}
