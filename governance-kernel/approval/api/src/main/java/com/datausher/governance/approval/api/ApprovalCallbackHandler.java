package com.datausher.governance.approval.api;

public interface ApprovalCallbackHandler {
    ApprovalCallbackType callbackType();

    void handle(ApprovalCallbackInvocation invocation);
}
