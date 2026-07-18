package com.datausher.integration.contract;

import com.datausher.integration.compute.api.ComputeCapabilities;
import com.datausher.integration.compute.api.ComputeEngineAdapter;
import com.datausher.integration.compute.api.ComputeJobHandle;
import com.datausher.integration.compute.api.ComputeJobLogPage;
import com.datausher.integration.compute.api.ComputeJobRequest;
import com.datausher.integration.compute.api.ComputeJobResultPage;
import com.datausher.integration.compute.api.ComputeJobStatus;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ComputeEngineAdapterContract {
    private ComputeEngineAdapterContract() {
    }

    public static void verify(
            ComputeEngineAdapter adapter,
            ComputeJobRequest request,
            ComputeJobHandle handle,
            Set<String> sensitiveValues
    ) {
        IntegrationAdapterContract.verify(adapter, sensitiveValues);
        assertEquals(AdapterType.COMPUTE_ENGINE, adapter.descriptor().type());
        assertTrue(adapter.descriptor().supports(ComputeCapabilities.JOB_EXECUTION),
                "compute adapters must declare job execution capability");
        assertEquals(adapter.descriptor().adapterId(), handle.adapterId(),
                "job handles must preserve adapter identity");
        assertEquals(request.bindingId(), handle.bindingId(),
                "job handles must preserve credential binding identity");
    }

    public static ComputeJobHandle verifyManagedJob(
            ComputeEngineAdapter adapter,
            AdapterRequestContext context,
            ComputeJobRequest request,
            Set<String> sensitiveValues
    ) {
        ComputeJobHandle handle = adapter.submit(context, request);
        verify(adapter, request, handle, sensitiveValues);

        ComputeJobStatus status = adapter.status(context, handle);
        assertEquals(handle, status.handle(),
                "job status must preserve the submitted handle");

        if (adapter.descriptor().supports(ComputeCapabilities.JOB_LOGS)) {
            ComputeJobLogPage logs = adapter.readLogs(context, handle, -1, 100);
            assertEquals(handle, logs.handle(),
                    "job logs must preserve the submitted handle");
            assertTrue(logs.entries().size() <= 100,
                    "job logs must honor the requested limit");
            long previous = -1;
            for (var entry : logs.entries()) {
                assertTrue(entry.sequence() > previous,
                        "job logs must be ordered by sequence");
                previous = entry.sequence();
            }
        }

        if (adapter.descriptor().supports(ComputeCapabilities.JOB_RESULTS)) {
            ComputeJobResultPage result = adapter.readResult(context, handle, 0, 100);
            assertEquals(handle, result.handle(),
                    "job results must preserve the submitted handle");
            assertEquals(0, result.offset(),
                    "job results must preserve the requested offset");
            assertTrue(result.rows().size() <= 100,
                    "job results must honor the requested limit");
        }

        return handle;
    }
}
