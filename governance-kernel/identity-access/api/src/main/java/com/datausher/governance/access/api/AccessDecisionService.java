package com.datausher.governance.access.api;

public interface AccessDecisionService {
    AccessDecision decide(AccessRequest request);
}
