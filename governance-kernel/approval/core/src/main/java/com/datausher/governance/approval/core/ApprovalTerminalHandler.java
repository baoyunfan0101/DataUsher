package com.datausher.governance.approval.core;

import com.datausher.governance.approval.api.ApprovalRequest;
import com.datausher.platform.shared.context.RequestContext;

@FunctionalInterface
public interface ApprovalTerminalHandler {
    void handle(ApprovalRequest request, RequestContext requestContext);

    static ApprovalTerminalHandler noop() {
        return (request, requestContext) -> {
        };
    }
}
