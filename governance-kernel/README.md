# governance-kernel

Stable project, resource, subject, and access-control boundaries.

| Project | Use for | Depends on |
| --- | --- | --- |
| `project-environment-api` | Project lifecycle and environment query contracts | `shared-types-api` |
| `project-environment-core` | Default project and environment services | `project-environment-api`, `audit-api`, `shared-types-api` |
| `resource-registry-api` | Resource types, references, scopes, and lifecycle contracts | `shared-types-api` |
| `resource-registry-core` | Default resource registry and scope validation | `resource-registry-api`, `project-environment-api`, `audit-api`, `shared-types-api` |
| `identity-access-api` | Subject queries and access-decision contracts | `resource-registry-api`, `shared-types-api` |
| `identity-access-core` | Default identity lookup and policy evaluation | `identity-access-api`, `resource-registry-api`, `audit-api`, `shared-types-api` |

## Usage Rules

- Business and library modules may depend on `*-api` projects only.
- Only an application composition root may depend on `*-core` projects.
- A core project may depend on another module's API, never on another core.
- Create project and environment scopes before registering scoped resources.
- Register each resource type with its owning module and supported actions.
- Check `AccessDecisionService` before executing protected commands.
- Pass `RequestContext` into state-changing and access-decision requests.
- Run `./gradlew verifyModuleBoundaries` after changing project dependencies.
