package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.ownership.api.OwnershipRole;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApprovalContractTest {
    @Test
    void templatesCopyStepsAndSupportExtensibleSelectors() {
        var direct = ApproverSelector.subject(new SubjectRef(SubjectType.USER, "approver-1"));
        var owners = ApproverSelector.resourceOwner(new OwnershipRole("data-steward"));
        var custom = new ApproverSelector(new ApproverSelectorType("on-call"), Map.of("team", "platform"));
        var steps = List.of(new ApprovalStepDefinition(
                "review", "Review", List.of(direct, owners, custom), 1));
        var request = new PublishApprovalTemplateRequest(
                new ApprovalTemplateKey("publish-task"),
                "Publish Task",
                new ApprovalPurpose("task-publish"),
                steps,
                Map.of(),
                RequestContext.system("request-1", Instant.parse("2026-07-18T00:00:00Z"))
        );

        assertEquals("on-call", request.steps().get(0).approverSelectors().get(2).type().value());
    }

    @Test
    void duplicateStepKeysAreRejected() {
        var selector = ApproverSelector.subject(new SubjectRef(SubjectType.USER, "approver-1"));
        var step = new ApprovalStepDefinition("review", "Review", List.of(selector), 1);

        assertThrows(IllegalArgumentException.class, () -> new PublishApprovalTemplateRequest(
                new ApprovalTemplateKey("publish-task"),
                "Publish Task",
                new ApprovalPurpose("task-publish"),
                List.of(step, step),
                Map.of(),
                RequestContext.system("request-1", Instant.parse("2026-07-18T00:00:00Z"))
        ));
    }
}
