package com.datausher.governance.approval.core;

import com.datausher.governance.access.api.IdentityQueryService;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectStatus;
import com.datausher.governance.approval.api.*;
import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceLifecycle;
import com.datausher.governance.resource.api.ResourceQueryService;
import com.datausher.platform.audit.api.*;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.Clock;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DefaultApprovalService implements ApprovalCommandService, ApprovalQueryService {
    private final ApprovalStore store;
    private final ResourceQueryService resources;
    private final IdentityQueryService identities;
    private final ApprovalDecisionAuthorizer decisionAuthorizer;
    private final Map<ApproverSelectorType, ApproverSelectorResolver> resolvers;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final AuditedCommandExecutor commandExecutor;
    private final DomainEventPublisher eventPublisher;
    private final ApprovalTerminalHandler terminalHandler;

    public DefaultApprovalService(
            ApprovalStore store,
            ResourceQueryService resources,
            IdentityQueryService identities,
            ApprovalDecisionAuthorizer decisionAuthorizer,
            Collection<? extends ApproverSelectorResolver> resolvers,
            IdGenerator idGenerator,
            Clock clock,
            AuditedCommandExecutor commandExecutor,
            DomainEventPublisher eventPublisher,
            ApprovalTerminalHandler terminalHandler
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.resources = Objects.requireNonNull(resources, "resources must not be null");
        this.identities = Objects.requireNonNull(identities, "identities must not be null");
        this.decisionAuthorizer = Objects.requireNonNull(
                decisionAuthorizer, "decisionAuthorizer must not be null");
        this.resolvers = indexResolvers(resolvers);
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.terminalHandler = Objects.requireNonNull(terminalHandler, "terminalHandler must not be null");
    }

    @Override
    public ApprovalTemplate publishTemplate(PublishApprovalTemplateRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        long version = store.findLatestTemplate(request.templateKey())
                .map(template -> template.version() + 1)
                .orElse(1L);
        ApprovalTemplate template = new ApprovalTemplate(
                request.templateKey(), version, request.displayName(), request.purpose(), request.steps(),
                clock.now(), request.requestContext().actor().actorId(), request.attributes());
        return commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public ApprovalTemplate execute() {
                store.createTemplate(template);
                return template;
            }

            @Override
            public AuditRecordRequest audit(ApprovalTemplate result) {
                return new AuditRecordRequest(
                        request.requestContext(), "approval", "approval-template.publish",
                        AuditTarget.global("approval-template", result.templateKey().value()),
                        AuditOutcome.SUCCEEDED, Map.of("version", Long.toString(result.version())));
            }

            @Override
            public void rollback(ApprovalTemplate result, RuntimeException cause) {
                store.deleteTemplate(result);
            }
        });
    }

    @Override
    public ApprovalRequest submit(SubmitApprovalRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Optional<ApprovalRequest> existing = store.findRequestByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            if (!matchesSubmission(existing.orElseThrow(), request)) {
                throw new IllegalStateException("idempotency key was used for a different approval request");
            }
            return existing.orElseThrow();
        }
        requireActiveResource(request.targetResource());
        requireActiveSubject(request.requestedBy());
        ApprovalTemplate template = store.findLatestTemplate(request.templateKey())
                .orElseThrow(() -> new IllegalArgumentException(
                        "approval template does not exist: " + request.templateKey()));
        List<ApprovalStep> steps = resolveSteps(template, request.targetResource());
        ApprovalRequest approval = new ApprovalRequest(
                new ApprovalRequestId(idGenerator.nextIdValue(
                        IdGenerationRequest.of("governance", "approval-request"))),
                template.templateKey(), template.version(), template.purpose(), request.title(),
                request.targetResource(), request.requestedBy(), ApprovalRequestStatus.PENDING,
                steps, request.callback(), request.idempotencyKey(), clock.now(), null, request.attributes());
        ApprovalRequest saved = commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public ApprovalRequest execute() {
                store.createRequest(approval);
                return approval;
            }

            @Override
            public AuditRecordRequest audit(ApprovalRequest result) {
                return requestAudit(request.requestContext(), "approval-request.submit", result, Map.of());
            }

            @Override
            public void rollback(ApprovalRequest result, RuntimeException cause) {
                store.deleteRequest(result);
            }
        });
        eventPublisher.publish(new ApprovalRequestedEvent(
                nextEventId(), clock.now(), request.requestContext(), saved));
        return saved;
    }

    @Override
    public ApprovalRequest decide(DecideApprovalRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ApprovalRequest current = store.findRequest(request.requestId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "approval request does not exist: " + request.requestId()));
        int activeIndex = stepIndex(current, request.stepKey());
        ApprovalStep active = current.steps().get(activeIndex);
        decisionAuthorizer.authorize(current, active, request.approver(), request.requestContext());
        Optional<ApprovalStepDecision> previousDecision = active.decisions().stream()
                .filter(decision -> decision.approver().equals(request.approver()))
                .findFirst();
        if (previousDecision.isPresent()) {
            ApprovalStepDecision decision = previousDecision.orElseThrow();
            if (decision.decision() == request.decision() && decision.comment().equals(request.comment())) {
                return current;
            }
            throw new IllegalStateException("approver has already decided the approval step differently");
        }
        if (current.status() != ApprovalRequestStatus.PENDING) {
            throw new IllegalStateException("approval request is already terminal: " + request.requestId());
        }
        if (active.status() != ApprovalStepStatus.ACTIVE) {
            throw new IllegalStateException("approval step is not active: " + request.stepKey());
        }
        if (!active.eligibleApprovers().contains(request.approver())) {
            throw new IllegalArgumentException("subject is not eligible for the active approval step");
        }
        ApprovalRequest updated = applyDecision(current, activeIndex, request);
        ApprovalRequest saved = updateWithAudit(
                current, updated, request.requestContext(), "approval-request.decide",
                Map.of("decision", request.decision().name(), "approver", request.approver().canonicalValue()));
        publishIfTerminal(saved, request.requestContext());
        dispatchIfTerminal(saved, request.requestContext());
        return saved;
    }

    @Override
    public ApprovalRequest cancel(CancelApprovalRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ApprovalRequest current = store.findRequest(request.requestId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "approval request does not exist: " + request.requestId()));
        if (current.status() == ApprovalRequestStatus.CANCELLED) {
            return current;
        }
        if (current.status() != ApprovalRequestStatus.PENDING) {
            throw new IllegalStateException("approval request is already terminal: " + request.requestId());
        }
        List<ApprovalStep> steps = current.steps().stream()
                .map(step -> step.status() == ApprovalStepStatus.APPROVED ? step : withStatus(step, ApprovalStepStatus.SKIPPED))
                .toList();
        ApprovalRequest cancelled = copy(current, ApprovalRequestStatus.CANCELLED, steps, clock.now());
        ApprovalRequest saved = updateWithAudit(
                current, cancelled, request.requestContext(), "approval-request.cancel",
                Map.of("reason", request.reason()));
        publishIfTerminal(saved, request.requestContext());
        dispatchIfTerminal(saved, request.requestContext());
        return saved;
    }

    @Override
    public Optional<ApprovalTemplate> findTemplate(ApprovalTemplateKey templateKey, long version) {
        return store.findTemplate(Objects.requireNonNull(templateKey, "templateKey must not be null"), version);
    }

    @Override
    public Optional<ApprovalTemplate> findLatestTemplate(ApprovalTemplateKey templateKey) {
        return store.findLatestTemplate(Objects.requireNonNull(templateKey, "templateKey must not be null"));
    }

    @Override
    public List<ApprovalTemplate> listTemplateVersions(ApprovalTemplateKey templateKey) {
        return store.listTemplateVersions(Objects.requireNonNull(templateKey, "templateKey must not be null"));
    }

    @Override
    public Optional<ApprovalRequest> findRequest(ApprovalRequestId requestId) {
        return store.findRequest(Objects.requireNonNull(requestId, "requestId must not be null"));
    }

    @Override
    public Optional<ApprovalRequest> findRequestByIdempotencyKey(String idempotencyKey) {
        String normalized = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        return store.findRequestByIdempotencyKey(normalized);
    }

    @Override
    public PageResult<ApprovalRequest> searchRequests(ApprovalRequestQuery query, PageRequest pageRequest) {
        return store.searchRequests(
                Objects.requireNonNull(query, "query must not be null"),
                Objects.requireNonNull(pageRequest, "pageRequest must not be null"));
    }

    private List<ApprovalStep> resolveSteps(ApprovalTemplate template, com.datausher.governance.resource.api.ResourceRef target) {
        List<ApprovalStep> steps = new ArrayList<>();
        for (int index = 0; index < template.steps().size(); index++) {
            ApprovalStepDefinition definition = template.steps().get(index);
            Set<SubjectRef> approvers = definition.approverSelectors().stream()
                    .map(selector -> resolver(selector.type()).resolve(selector, target))
                    .flatMap(Collection::stream)
                    .peek(this::requireActiveSubject)
                    .collect(Collectors.toUnmodifiableSet());
            if (definition.requiredApprovals() > approvers.size()) {
                throw new IllegalStateException(
                        "approval step does not resolve enough approvers: " + definition.stepKey());
            }
            steps.add(new ApprovalStep(
                    definition.stepKey(), definition.displayName(), approvers, definition.requiredApprovals(),
                    index == 0 ? ApprovalStepStatus.ACTIVE : ApprovalStepStatus.WAITING, List.of()));
        }
        return List.copyOf(steps);
    }

    private ApprovalRequest applyDecision(ApprovalRequest current, int activeIndex, DecideApprovalRequest request) {
        List<ApprovalStep> steps = new ArrayList<>(current.steps());
        ApprovalStep active = steps.get(activeIndex);
        List<ApprovalStepDecision> decisions = new ArrayList<>(active.decisions());
        decisions.add(new ApprovalStepDecision(request.approver(), request.decision(), request.comment(), clock.now()));
        if (request.decision() == ApprovalDecisionType.REJECT) {
            steps.set(activeIndex, withDecisions(active, ApprovalStepStatus.REJECTED, decisions));
            for (int index = activeIndex + 1; index < steps.size(); index++) {
                steps.set(index, withStatus(steps.get(index), ApprovalStepStatus.SKIPPED));
            }
            return copy(current, ApprovalRequestStatus.REJECTED, steps, clock.now());
        }
        long approvals = decisions.stream()
                .filter(decision -> decision.decision() == ApprovalDecisionType.APPROVE)
                .count();
        if (approvals < active.requiredApprovals()) {
            steps.set(activeIndex, withDecisions(active, ApprovalStepStatus.ACTIVE, decisions));
            return copy(current, ApprovalRequestStatus.PENDING, steps, null);
        }
        steps.set(activeIndex, withDecisions(active, ApprovalStepStatus.APPROVED, decisions));
        if (activeIndex + 1 < steps.size()) {
            steps.set(activeIndex + 1, withStatus(steps.get(activeIndex + 1), ApprovalStepStatus.ACTIVE));
            return copy(current, ApprovalRequestStatus.PENDING, steps, null);
        }
        return copy(current, ApprovalRequestStatus.APPROVED, steps, clock.now());
    }

    private ApprovalRequest updateWithAudit(
            ApprovalRequest current,
            ApprovalRequest updated,
            RequestContext context,
            String action,
            Map<String, String> details
    ) {
        return commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public ApprovalRequest execute() {
                store.updateRequest(current, updated);
                return updated;
            }

            @Override
            public AuditRecordRequest audit(ApprovalRequest result) {
                return requestAudit(context, action, result, details);
            }

            @Override
            public void rollback(ApprovalRequest result, RuntimeException cause) {
                store.updateRequest(updated, current);
            }
        });
    }

    private ApprovalRequest requirePending(ApprovalRequestId requestId) {
        ApprovalRequest request = store.findRequest(requestId)
                .orElseThrow(() -> new IllegalArgumentException("approval request does not exist: " + requestId));
        if (request.status() != ApprovalRequestStatus.PENDING) {
            throw new IllegalStateException("approval request is already terminal: " + requestId);
        }
        return request;
    }

    private void requireActiveResource(com.datausher.governance.resource.api.ResourceRef ref) {
        RegisteredResource resource = resources.find(ref)
                .orElseThrow(() -> new IllegalArgumentException("resource does not exist: " + ref.canonicalValue()));
        if (resource.lifecycle() != ResourceLifecycle.ACTIVE) {
            throw new IllegalStateException("resource is not active: " + ref.canonicalValue());
        }
    }

    private void requireActiveSubject(SubjectRef ref) {
        var subject = identities.find(ref)
                .orElseThrow(() -> new IllegalArgumentException("subject does not exist: " + ref.canonicalValue()));
        if (subject.status() != SubjectStatus.ACTIVE) {
            throw new IllegalStateException("subject is not active: " + ref.canonicalValue());
        }
    }

    private ApproverSelectorResolver resolver(ApproverSelectorType type) {
        ApproverSelectorResolver resolver = resolvers.get(type);
        if (resolver == null) {
            throw new IllegalArgumentException("no approver selector resolver is registered for: " + type);
        }
        return resolver;
    }

    private void dispatchIfTerminal(ApprovalRequest request, RequestContext context) {
        if (request.status() != ApprovalRequestStatus.PENDING) {
            terminalHandler.handle(request, context);
        }
    }

    private void publishIfTerminal(ApprovalRequest request, RequestContext context) {
        if (request.status() != ApprovalRequestStatus.PENDING) {
            eventPublisher.publish(new ApprovalCompletedEvent(
                    nextEventId(), clock.now(), context, request));
        }
    }

    private String nextEventId() {
        return idGenerator.nextIdValue(IdGenerationRequest.of("governance", "approval-event"));
    }

    private static int stepIndex(ApprovalRequest request, String stepKey) {
        for (int index = 0; index < request.steps().size(); index++) {
            if (request.steps().get(index).stepKey().equals(stepKey)) {
                return index;
            }
        }
        throw new IllegalArgumentException("approval step does not exist: " + stepKey);
    }

    private static ApprovalStep withStatus(ApprovalStep step, ApprovalStepStatus status) {
        return new ApprovalStep(step.stepKey(), step.displayName(), step.eligibleApprovers(),
                step.requiredApprovals(), status, step.decisions());
    }

    private static ApprovalStep withDecisions(
            ApprovalStep step,
            ApprovalStepStatus status,
            List<ApprovalStepDecision> decisions
    ) {
        return new ApprovalStep(step.stepKey(), step.displayName(), step.eligibleApprovers(),
                step.requiredApprovals(), status, decisions);
    }

    private static ApprovalRequest copy(
            ApprovalRequest request,
            ApprovalRequestStatus status,
            List<ApprovalStep> steps,
            java.time.Instant completedAt
    ) {
        return new ApprovalRequest(
                request.requestId(), request.templateKey(), request.templateVersion(), request.purpose(),
                request.title(), request.targetResource(), request.requestedBy(), status, steps,
                request.callback(), request.idempotencyKey(), request.createdAt(), completedAt, request.attributes());
    }

    private static boolean matchesSubmission(ApprovalRequest existing, SubmitApprovalRequest request) {
        return existing.templateKey().equals(request.templateKey())
                && existing.title().equals(request.title())
                && existing.targetResource().equals(request.targetResource())
                && existing.requestedBy().equals(request.requestedBy())
                && Objects.equals(existing.callback(), request.callback())
                && existing.attributes().equals(request.attributes());
    }

    private static AuditRecordRequest requestAudit(
            RequestContext context,
            String action,
            ApprovalRequest request,
            Map<String, String> details
    ) {
        Map<String, String> values = new HashMap<>(details);
        values.put("status", request.status().name());
        values.put("target", request.targetResource().canonicalValue());
        return new AuditRecordRequest(
                context, "approval", action,
                AuditTarget.global("approval-request", request.requestId().value()),
                AuditOutcome.SUCCEEDED, values);
    }

    private static Map<ApproverSelectorType, ApproverSelectorResolver> indexResolvers(
            Collection<? extends ApproverSelectorResolver> resolvers
    ) {
        Objects.requireNonNull(resolvers, "resolvers must not be null");
        return resolvers.stream().collect(Collectors.toUnmodifiableMap(
                ApproverSelectorResolver::selectorType,
                Function.identity(),
                (first, second) -> {
                    throw new IllegalArgumentException("duplicate approver selector resolver: " + first.selectorType());
                }
        ));
    }
}
