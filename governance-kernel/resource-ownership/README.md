# resource-ownership

`resource-ownership-api` provides owner assignment and query contracts for any
registered `ResourceRef`. `resource-ownership-core` provides the default service.

## Usage Rules

- Depend on `resource-ownership-api` from business and library modules.
- Construct `resource-ownership-core` only in an application composition root.
- Register the resource and subject before assigning ownership.
- Use `OwnershipRole` values to distinguish responsibilities without changing the API.
- Use `listOwners` for normal resource-level lookup and `search` for paged queries.
- Treat a resource, subject, and role tuple as one owner assignment.
- Pass the caller's `RequestContext` to owner commands.
