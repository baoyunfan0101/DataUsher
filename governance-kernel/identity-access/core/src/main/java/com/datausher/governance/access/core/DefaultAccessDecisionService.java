package com.datausher.governance.access.core;

import com.datausher.governance.access.api.AccessDecision;
import com.datausher.governance.access.api.AccessDecisionCode;
import com.datausher.governance.access.api.AccessDecisionService;
import com.datausher.governance.access.api.AccessRequest;
import com.datausher.governance.access.api.IdentityQueryService;
import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectStatus;
import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceLifecycle;
import com.datausher.governance.resource.api.ResourceQueryService;
import com.datausher.governance.resource.api.ResourceTypeDefinition;
import com.datausher.governance.resource.api.ResourceTypeRegistry;
import com.datausher.platform.audit.api.AuditCommandService;
import com.datausher.platform.audit.api.AuditOutcome;
import com.datausher.platform.audit.api.AuditRecordRequest;
import com.datausher.platform.audit.api.AuditTarget;
import com.datausher.platform.shared.time.Clock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultAccessDecisionService implements AccessDecisionService {
    private final IdentityQueryService identityQueryService;
    private final ResourceTypeRegistry resourceTypeRegistry;
    private final ResourceQueryService resourceQueryService;
    private final AccessPolicyStore policyStore;
    private final Clock clock;
    private final AuditCommandService auditService;

    public DefaultAccessDecisionService(
            IdentityQueryService identityQueryService,
            ResourceTypeRegistry resourceTypeRegistry,
            ResourceQueryService resourceQueryService,
            AccessPolicyStore policyStore,
            Clock clock,
            AuditCommandService auditService
    ) {
        this.identityQueryService = Objects.requireNonNull(identityQueryService,
                "identityQueryService must not be null");
        this.resourceTypeRegistry = Objects.requireNonNull(resourceTypeRegistry,
                "resourceTypeRegistry must not be null");
        this.resourceQueryService = Objects.requireNonNull(resourceQueryService,
                "resourceQueryService must not be null");
        this.policyStore = Objects.requireNonNull(policyStore, "policyStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
    }

    @Override
    public AccessDecision decide(AccessRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        for (SubjectRef ref : request.subjects()) {
            Subject subject = identityQueryService.find(ref).orElse(null);
            if (subject == null) {
                return audited(request, false, AccessDecisionCode.DENIED_UNKNOWN_SUBJECT,
                        "subject is not registered", null);
            }
            if (subject.status() != SubjectStatus.ACTIVE) {
                return audited(request, false, AccessDecisionCode.DENIED_INACTIVE_SUBJECT,
                        "subject is not active", null);
            }
        }

        ResourceTypeDefinition type = resourceTypeRegistry.find(request.resource().resourceType()).orElse(null);
        if (type == null) {
            return audited(request, false, AccessDecisionCode.DENIED_UNKNOWN_RESOURCE,
                    "resource type is not registered", null);
        }
        if (!type.supports(request.action())) {
            return audited(request, false, AccessDecisionCode.DENIED_UNSUPPORTED_ACTION,
                    "action is not declared by the resource type", null);
        }

        RegisteredResource resource = resourceQueryService.find(request.resource()).orElse(null);
        if (resource == null) {
            return audited(request, false, AccessDecisionCode.DENIED_UNKNOWN_RESOURCE,
                    "resource is not registered", null);
        }
        if (resource.lifecycle() != ResourceLifecycle.ACTIVE) {
            return audited(request, false, AccessDecisionCode.DENIED_INACTIVE_RESOURCE,
                    "resource is not active", null);
        }

        AccessPolicy policy = policyStore.findEffective(
                request.subjects(), request.action(), request.resource()).orElse(null);
        if (policy == null) {
            return audited(request, false, AccessDecisionCode.DENIED_NO_MATCHING_POLICY,
                    "no matching access policy", null);
        }
        if (policy.effect() == PolicyEffect.DENY) {
            return audited(request, false, AccessDecisionCode.DENIED_BY_POLICY,
                    "access denied by policy", policy.policyId());
        }
        return audited(request, true, AccessDecisionCode.ALLOWED,
                "access allowed by policy", policy.policyId());
    }

    private AccessDecision audited(
            AccessRequest request,
            boolean allowed,
            AccessDecisionCode code,
            String reason,
            String policyId
    ) {
        AccessDecision decision = new AccessDecision(allowed, code, reason, policyId, clock.now());
        Map<String, String> details = new LinkedHashMap<>();
        details.put("decisionCode", code.name());
        details.put("action", request.action());
        if (policyId != null) {
            details.put("policyId", policyId);
        }
        auditService.record(new AuditRecordRequest(
                request.requestContext(),
                "identity-access",
                "access.decide",
                new AuditTarget(
                        request.resource().resourceType(),
                        request.resource().resourceId(),
                        request.resource().scope().canonicalValue()
                ),
                allowed ? AuditOutcome.SUCCEEDED : AuditOutcome.DENIED,
                details
        ));
        return decision;
    }
}
