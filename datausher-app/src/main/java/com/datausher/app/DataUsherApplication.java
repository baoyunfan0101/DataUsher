package com.datausher.app;

import com.datausher.governance.access.api.AccessDecision;
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
import com.datausher.governance.project.api.Project;
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
import com.datausher.platform.audit.core.CompensatingAuditedCommandExecutor;
import com.datausher.platform.audit.core.DefaultAuditService;
import com.datausher.platform.audit.core.InMemoryAuditEventStore;
import com.datausher.platform.config.api.ConfigKey;
import com.datausher.platform.config.api.ConfigNamespace;
import com.datausher.platform.config.api.ConfigProfile;
import com.datausher.platform.config.api.ConfigQueryService;
import com.datausher.platform.config.api.ConfigResolutionContext;
import com.datausher.platform.config.core.MapConfigQueryService;
import com.datausher.platform.module.api.ModuleCapability;
import com.datausher.platform.module.api.ModuleDependency;
import com.datausher.platform.module.api.ModuleDescriptor;
import com.datausher.platform.module.api.ModuleRegistry;
import com.datausher.platform.module.core.InMemoryModuleRegistry;
import com.datausher.platform.observability.core.NoopMetricRecorder;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;
import com.datausher.platform.shared.event.core.InProcessDomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.time.Clock;
import com.datausher.platform.shared.time.core.SystemClock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DataUsherApplication {
    private DataUsherApplication() {
    }

    public static void main(String[] args) {
        IdGenerator idGenerator = new UuidIdGenerator();
        Clock clock = new SystemClock();
        ModuleRegistry moduleRegistry = new InMemoryModuleRegistry();
        ConfigQueryService configQueryService = new MapConfigQueryService(Map.of(
                "app.name", "DataUsher",
                "global.default.platform.stage", "2",
                "platform-kernel.default.platform.stage", "2"
        ), "bootstrap");
        ConfigResolutionContext platformContext = ConfigResolutionContext.of(
                ConfigNamespace.of("platform-kernel"),
                ConfigProfile.DEFAULT
        );
        InProcessDomainEventPublisher eventPublisher = new InProcessDomainEventPublisher();
        NoopMetricRecorder metricRecorder = new NoopMetricRecorder();
        DefaultAuditService auditService = new DefaultAuditService(
                new InMemoryAuditEventStore(), idGenerator, clock);
        CompensatingAuditedCommandExecutor commandExecutor =
                new CompensatingAuditedCommandExecutor(auditService);
        DefaultProjectEnvironmentService projectService = new DefaultProjectEnvironmentService(
                new InMemoryProjectEnvironmentStore(), idGenerator, clock, commandExecutor);
        DefaultResourceRegistryService resourceService = new DefaultResourceRegistryService(
                new InMemoryResourceStore(), clock, commandExecutor,
                new ProjectEnvironmentScopeValidator(projectService, projectService));
        InMemorySubjectStore subjectStore = new InMemorySubjectStore();
        DefaultIdentityQueryService identityService = new DefaultIdentityQueryService(subjectStore);
        InMemoryAccessPolicyStore policyStore = new InMemoryAccessPolicyStore();
        DefaultAccessDecisionService accessDecisionService = new DefaultAccessDecisionService(
                identityService, resourceService, resourceService, policyStore, clock, auditService);
        IdGenerationRequest eventIdRequest = IdGenerationRequest.of("platform-kernel", "domain-event");
        IdGenerationRequest requestIdRequest = IdGenerationRequest.of("platform-kernel", "request");

        moduleRegistry.register(new ModuleDescriptor(
                "platform-kernel",
                "0.1.0-SNAPSHOT",
                "DataUsher platform foundation contracts",
                List.of(),
                List.of(),
                Map.of("stage", "1")
        ));

        moduleRegistry.register(new ModuleDescriptor(
                "audit",
                "0.1.0-SNAPSHOT",
                "Immutable platform audit journal",
                List.of(new ModuleDependency("platform-kernel", "0.1.0-SNAPSHOT", true)),
                List.of(new ModuleCapability("audit.record", Map.of()),
                        new ModuleCapability("audit.query", Map.of())),
                Map.of("stage", "2")
        ));
        moduleRegistry.register(new ModuleDescriptor(
                "project-environment",
                "0.1.0-SNAPSHOT",
                "Project isolation and environment boundaries",
                List.of(new ModuleDependency("audit", "0.1.0-SNAPSHOT", true)),
                List.of(new ModuleCapability("project.manage", Map.of())),
                Map.of("stage", "2")
        ));
        moduleRegistry.register(new ModuleDescriptor(
                "resource-registry",
                "0.1.0-SNAPSHOT",
                "Unified resource references and type registry",
                List.of(new ModuleDependency("audit", "0.1.0-SNAPSHOT", true)),
                List.of(new ModuleCapability("resource.register", Map.of())),
                Map.of("stage", "2")
        ));
        moduleRegistry.register(new ModuleDescriptor(
                "identity-access",
                "0.1.0-SNAPSHOT",
                "Identity queries and default-deny access decisions",
                List.of(
                        new ModuleDependency("resource-registry", "0.1.0-SNAPSHOT", true),
                        new ModuleDependency("audit", "0.1.0-SNAPSHOT", true)
                ),
                List.of(new ModuleCapability("access.decide", Map.of())),
                Map.of("stage", "2")
        ));

        Instant bootstrapTime = clock.now();
        RequestContext bootstrapContext = RequestContext.system(
                idGenerator.nextIdValue(requestIdRequest), bootstrapTime);
        SubjectRef systemSubject = new SubjectRef(SubjectType.SERVICE_ACCOUNT, "system");
        subjectStore.save(new Subject(systemSubject, "System", SubjectStatus.ACTIVE, Map.of()));
        Project bootstrapProject = projectService.create(new CreateProjectRequest(
                "bootstrap",
                "Bootstrap Project",
                List.of(new EnvironmentSpec("dev", "Development")),
                Map.of(),
                bootstrapContext
        ));
        resourceService.register(new RegisterResourceTypeRequest(
                new ResourceTypeDefinition(
                        "project", "project-environment", "Project", Set.of("read", "manage")),
                bootstrapContext
        ));
        ResourceRef projectRef = ResourceRef.global("project", bootstrapProject.projectId());
        resourceService.register(new RegisterResourceRequest(
                projectRef, bootstrapProject.displayName(), Map.of(), bootstrapContext));
        policyStore.save(new AccessPolicy(
                "bootstrap-system-project-read",
                systemSubject,
                "project",
                "read",
                ResourceScope.global(),
                PolicyEffect.ALLOW,
                100,
                true
        ));
        AccessDecision bootstrapDecision = accessDecisionService.decide(new AccessRequest(
                Set.of(systemSubject), "read", projectRef, bootstrapContext, Map.of()));

        eventPublisher.subscribe(event -> System.out.println("event=" + event.eventType()));
        Instant moduleStartedAt = clock.now();
        eventPublisher.publish(new ModuleStartedEvent(
                idGenerator.nextIdValue(eventIdRequest),
                "platform-kernel",
                moduleStartedAt,
                RequestContext.system(idGenerator.nextIdValue(requestIdRequest), moduleStartedAt),
                "platform-kernel"
        ));
        metricRecorder.incrementCounter("datausher.bootstrap.started", Map.of("module", "platform-kernel"));

        System.out.println("app.name=" + configQueryService.getString(ConfigKey.of("app.name"), "DataUsher"));
        System.out.println("platform.stage=" + configQueryService.getString(
                ConfigKey.of("platform.stage"),
                platformContext,
                "unknown"
        ));
        System.out.println("modules=" + moduleRegistry.listModules().size());
        System.out.println("bootstrap.access=" + bootstrapDecision.code());
    }

    private record ModuleStartedEvent(
            String eventId,
            String sourceModule,
            Instant occurredAt,
            RequestContext requestContext,
            String module
    ) implements DomainEvent {
        @Override
        public String eventType() {
            return "ModuleStarted";
        }
    }
}
