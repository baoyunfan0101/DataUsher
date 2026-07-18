package com.datausher.governance.approval.core;

import com.datausher.governance.approval.api.ApprovalRequest;
import com.datausher.governance.approval.api.ApprovalRequestId;
import com.datausher.governance.approval.api.ApprovalRequestQuery;
import com.datausher.governance.approval.api.ApprovalStep;
import com.datausher.governance.approval.api.ApprovalTemplate;
import com.datausher.governance.approval.api.ApprovalTemplateKey;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryApprovalStore implements ApprovalStore {
    private final ConcurrentMap<String, ApprovalTemplate> templates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ApprovalRequest> requests = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> idempotencyIndex = new ConcurrentHashMap<>();

    @Override
    public Optional<ApprovalTemplate> findTemplate(ApprovalTemplateKey templateKey, long version) {
        return Optional.ofNullable(templates.get(templateKey(templateKey, version)));
    }

    @Override
    public Optional<ApprovalTemplate> findLatestTemplate(ApprovalTemplateKey templateKey) {
        return templates.values().stream()
                .filter(template -> template.templateKey().equals(templateKey))
                .max(Comparator.comparingLong(ApprovalTemplate::version));
    }

    @Override
    public List<ApprovalTemplate> listTemplateVersions(ApprovalTemplateKey templateKey) {
        return templates.values().stream()
                .filter(template -> template.templateKey().equals(templateKey))
                .sorted(Comparator.comparingLong(ApprovalTemplate::version))
                .toList();
    }

    @Override
    public synchronized void createTemplate(ApprovalTemplate template) {
        long latestVersion = findLatestTemplate(template.templateKey())
                .map(ApprovalTemplate::version)
                .orElse(0L);
        if (template.version() != latestVersion + 1) {
            throw new IllegalStateException("template version changed concurrently: " + template.templateKey());
        }
        templates.put(templateKey(template.templateKey(), template.version()), template);
    }

    @Override
    public void deleteTemplate(ApprovalTemplate template) {
        if (!templates.remove(templateKey(template.templateKey(), template.version()), template)) {
            throw new IllegalStateException("template changed before rollback: " + template.templateKey());
        }
    }

    @Override
    public void updateTemplate(ApprovalTemplate expected, ApprovalTemplate replacement) {
        if (!expected.templateKey().equals(replacement.templateKey())
                || expected.version() != replacement.version()) {
            throw new IllegalArgumentException("approval template identifiers must match");
        }
        if (!templates.replace(
                templateKey(expected.templateKey(), expected.version()), expected, replacement)) {
            throw new IllegalStateException("approval template changed concurrently: " + expected.templateKey());
        }
    }

    @Override
    public Optional<ApprovalRequest> findRequest(ApprovalRequestId requestId) {
        return Optional.ofNullable(requests.get(requestId.value()));
    }

    @Override
    public Optional<ApprovalRequest> findRequestByIdempotencyKey(String idempotencyKey) {
        String requestId = idempotencyIndex.get(idempotencyKey);
        return requestId == null ? Optional.empty() : Optional.ofNullable(requests.get(requestId));
    }

    @Override
    public synchronized void createRequest(ApprovalRequest request) {
        if (requests.containsKey(request.requestId().value())) {
            throw new IllegalStateException("approval request already exists: " + request.requestId());
        }
        String existingId = idempotencyIndex.putIfAbsent(
                request.idempotencyKey(), request.requestId().value());
        if (existingId != null) {
            throw new IllegalStateException(
                    "approval idempotency key already exists: " + request.idempotencyKey());
        }
        requests.put(request.requestId().value(), request);
    }

    @Override
    public synchronized void deleteRequest(ApprovalRequest request) {
        if (!requests.remove(request.requestId().value(), request)
                || !idempotencyIndex.remove(request.idempotencyKey(), request.requestId().value())) {
            throw new IllegalStateException("approval request changed before rollback: " + request.requestId());
        }
    }

    @Override
    public void updateRequest(ApprovalRequest expected, ApprovalRequest replacement) {
        if (!expected.requestId().equals(replacement.requestId())) {
            throw new IllegalArgumentException("approval request identifiers must match");
        }
        if (!requests.replace(expected.requestId().value(), expected, replacement)) {
            throw new IllegalStateException("approval request changed concurrently: " + expected.requestId());
        }
    }

    @Override
    public PageResult<ApprovalRequest> searchRequests(ApprovalRequestQuery query, PageRequest pageRequest) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        List<ApprovalRequest> matches = new ArrayList<>();
        for (ApprovalRequest request : requests.values()) {
            if ((query.purpose() == null || query.purpose().equals(request.purpose()))
                    && (query.status() == null || query.status() == request.status())
                    && (query.targetResource() == null || query.targetResource().equals(request.targetResource()))
                    && (query.requestedBy() == null || query.requestedBy().equals(request.requestedBy()))
                    && (query.eligibleApprover() == null || isEligible(request, query.eligibleApprover()))) {
                matches.add(request);
            }
        }
        matches.sort(Comparator.comparing(request -> request.requestId().value()));
        int fromIndex = (int) Math.min(pageRequest.offset(), matches.size());
        int toIndex = (int) Math.min((long) fromIndex + pageRequest.size(), matches.size());
        return new PageResult<>(
                matches.subList(fromIndex, toIndex),
                matches.size(),
                pageRequest.page(),
                pageRequest.size()
        );
    }

    private static boolean isEligible(ApprovalRequest request, SubjectRef subjectRef) {
        return request.steps().stream()
                .map(ApprovalStep::eligibleApprovers)
                .anyMatch(approvers -> approvers.contains(subjectRef));
    }

    private static String templateKey(ApprovalTemplateKey key, long version) {
        return key.value() + "@" + version;
    }
}
