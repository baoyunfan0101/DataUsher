package com.datausher.governance.approval.api;

import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.List;
import java.util.Optional;

public interface ApprovalQueryService {
    Optional<ApprovalTemplate> findTemplate(ApprovalTemplateKey templateKey, long version);

    Optional<ApprovalTemplate> findLatestTemplate(ApprovalTemplateKey templateKey);

    List<ApprovalTemplate> listTemplateVersions(ApprovalTemplateKey templateKey);

    Optional<ApprovalRequest> findRequest(ApprovalRequestId requestId);

    Optional<ApprovalRequest> findRequestByIdempotencyKey(String idempotencyKey);

    PageResult<ApprovalRequest> searchRequests(ApprovalRequestQuery query, PageRequest pageRequest);
}
