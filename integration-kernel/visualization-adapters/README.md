# visualization-adapters

`visualization-adapters-api` defines provider-neutral contracts for external
visualization systems.

## Public Contracts

- Implement `VisualizationAdapter` for BI and dashboard platforms.
- Use `VisualizationDatasetBinding` to represent an external dataset binding.
- Use `VisualizationChart` and `VisualizationDashboard` to represent published
  visual resources.
- Treat `VisualizationResourceRef` values as opaque external references.

## Usage Rules

- Keep visualization platform SDK types outside public contracts.
- Keep credentials and tenant-specific connection details outside request
  payloads.
- Declare dataset, chart, dashboard, and lookup capabilities in the adapter
  descriptor.
- Map external failures to integration runtime failures without leaking
  sensitive values.
