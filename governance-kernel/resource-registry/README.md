# resource-registry

`resource-registry-api` contains resource type, reference, scope, lifecycle,
command, and query contracts. `resource-registry-core` contains the default
registry, scope validation, and local storage implementation.

## Usage Rules

- Business modules depend on `resource-registry-api` only.
- The application composition root selects and constructs `resource-registry-core` implementations.
- Register a resource type through `RegisterResourceTypeRequest` with a stable owner module and action set.
- Use `ResourceRef.global`, `ResourceScope.project`, or `ResourceScope.environment` to describe scope.
- Create referenced projects and environments before registering scoped resources.
- Persist `ResourceRef.canonicalValue()` only when a single string key is required.
- Treat `DELETED` as terminal and do not reactivate deleted resources.
- Pass the caller's `RequestContext` to resource type and resource commands.
