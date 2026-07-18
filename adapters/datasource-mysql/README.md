# mysql datasource adapter

## Owns

```text
MySQL metadata discovery
MySQL connection validation behavior
Relational query parameter and result mapping
Canonical MySQL external object identifiers
Safe external error details
```

## Does not own

```text
Datasource definitions
Catalog persistence
Credential storage or secret resolution
Connection pooling policy
Business metadata
Query job lifecycle
```

The host resolves `DatasourceConnection.bindingId()` through its credential
system. This adapter never accepts passwords as datasource properties and never
includes SQL text or vendor error messages in external error details.

External object IDs URL-encode each identifier segment. Human-readable qualified
names are carried separately for catalog display and search.
