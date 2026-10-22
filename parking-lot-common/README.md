# parking-lot-common

Parking application DTOs and events have been removed. The remaining contracts
support analytics, chatbot, diagnostics, and other portfolio features.

Shared request/response/model contracts used by both:
- `api-service`
- `sdk`

## Packages
- `com.example.parkinglot.common.request`
- `com.example.parkinglot.common.response`
- `com.example.parkinglot.common.model`

`api-service` and `sdk` should depend on this module and import contract types from these packages.
