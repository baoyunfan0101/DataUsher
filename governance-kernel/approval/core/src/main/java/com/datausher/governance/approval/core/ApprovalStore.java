package com.datausher.governance.approval.core;

import com.datausher.governance.approval.api.ApprovalRequest;
import com.datausher.governance.approval.api.ApprovalRequestId;
import com.datausher.governance.approval.api.ApprovalRequestQuery;
import com.datausher.governance.approval.api.ApprovalTemplate;
import com.datausher.governance.approval.api.ApprovalTemplateKey;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.List;
import java.util.Optional;

public interface ApprovalStore {
    Optional<ApprovalTemplate> findTemplate(ApprovalTemplateKey templateKey, long version);

    Optional<ApprovalTemplate> findLatestTemplate(ApprovalTemplateKey templateKey);

    List<ApprovalTemplate> listTemplateVersions(ApprovalTemplateKey templateKey);

    void createTemplate(ApprovalTemplate template);

    void deleteTemplate(ApprovalTemplate template);

    void updateTemplate(ApprovalTemplate expected, ApprovalTemplate replacement);

    Optional<ApprovalRequest> findRequest(ApprovalRequestId requestId);

    Optional<ApprovalRequest> findRequestByIdempotencyKey(String idempotencyKey);

    void createRequest(ApprovalRequest request);

    void deleteRequest(ApprovalRequest request);

    void updateRequest(ApprovalRequest expected, ApprovalRequest replacement);

    PageResult<ApprovalRequest> searchRequests(ApprovalRequestQuery query, PageRequest pageRequest);
}
