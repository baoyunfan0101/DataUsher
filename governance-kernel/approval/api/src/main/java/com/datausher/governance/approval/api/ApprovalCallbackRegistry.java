package com.datausher.governance.approval.api;

public interface ApprovalCallbackRegistry {
    void register(ApprovalCallbackHandler handler);
}
