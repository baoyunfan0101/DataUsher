package com.datausher.app;

import com.datausher.governance.access.api.AccessDecisionCode;
import com.datausher.governance.access.api.AccessRequest;
import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectStatus;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.access.core.AccessPolicy;
import com.datausher.governance.access.core.DefaultAccessDecisionService;
import com.datausher.governance.access.core.DefaultIdentityQueryService;
import com.datausher.governance.access.core.InMemoryAccessPolicyStore;
import com.datausher.governance.access.core.InMemorySubjectStore;
import com.datausher.governance.access.core.PolicyEffect;
import com.datausher.governance.project.api.CreateProjectRequest;
import com.datausher.governance.project.api.EnvironmentSpec;
import com.datausher.governance.project.core.DefaultProjectEnvironmentService;
import com.datausher.governance.project.core.InMemoryProjectEnvironmentStore;
import com.datausher.governance.resource.api.RegisterResourceRequest;
import com.datausher.governance.resource.api.RegisterResourceTypeRequest;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.governance.resource.api.ResourceScope;
import com.datausher.governance.resource.api.ResourceTypeDefinition;
import com.datausher.governance.resource.core.DefaultResourceRegistryService;
import com.datausher.governance.resource.core.InMemoryResourceStore;
import com.datausher.governance.resource.core.ProjectEnvironmentScopeValidator;
import com.datausher.platform.audit.api.AuditQuery;
import com.datausher.platform.audit.core.CompensatingAuditedCommandExecutor;
import com.datausher.platform.audit.core.DefaultAuditService;
import com.datausher.platform.audit.core.InMemoryAuditEventStore;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GovernanceServiceCompositionTest {
    @Test
    void composesProjectResourceAccessAndAuditWithoutInternalCrossDependencies() {
        var idGenerator = new UuidIdGenerator();
        var clock = new SystemClock();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), idGenerator, clock);
        var commandExecutor = new CompensatingAuditedCommandExecutor(audit);
        var projects = new DefaultProjectEnvironmentService(
                new InMemoryProjectEnvironmentStore(), idGenerator, clock, commandExecutor);
        var resources = new DefaultResourceRegistryService(
                new InMemoryResourceStore(), clock, commandExecutor,
                new ProjectEnvironmentScopeValidator(projects, projects));
        var subjectStore = new InMemorySubjectStore();
        var identities = new DefaultIdentityQueryService(subjectStore);
        var policies = new InMemoryAccessPolicyStore();
        var access = new DefaultAccessDecisionService(
                identities, resources, resources, policies, clock, audit);
        var context = RequestContext.system("request-1", clock.now());
        var subjectRef = new SubjectRef(SubjectType.SERVICE_ACCOUNT, "build-agent");
        subjectStore.save(new Subject(subjectRef, "Build Agent", SubjectStatus.ACTIVE, Map.of()));

        var project = projects.create(new CreateProjectRequest(
                "analytics",
                "Analytics",
                List.of(
                        new EnvironmentSpec("dev", "Development"),
                        new EnvironmentSpec("prod", "Production")
                ),
                Map.of(),
                context
        ));
        resources.register(new RegisterResourceTypeRequest(
                new ResourceTypeDefinition(
                        "project", "project-environment", "Project", Set.of("read", "manage")),
                context
        ));
        var projectRef = ResourceRef.global("project", project.projectId());
        resources.register(new RegisterResourceRequest(projectRef, project.displayName(), Map.of(), context));

        assertEquals(AccessDecisionCode.DENIED_NO_MATCHING_POLICY,
                access.decide(new AccessRequest(Set.of(subjectRef), "read", projectRef, context, Map.of())).code());

        policies.save(new AccessPolicy(
                "allow-build-agent-read",
                subjectRef,
                "project",
                "read",
                ResourceScope.global(),
                PolicyEffect.ALLOW,
                100,
                true
        ));

        assertEquals(AccessDecisionCode.ALLOWED,
                access.decide(new AccessRequest(Set.of(subjectRef), "read", projectRef, context, Map.of())).code());
        assertEquals(2, projects.listByProject(project.projectId()).size());
        assertEquals(5, audit.search(AuditQuery.all(), PageRequest.firstPage()).total());
    }
}
