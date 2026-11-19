# MS-TRANSFER-LIMITS

## Purpose
Implement a Java 25 / Spring Boot 4.1.2 service that manages per-account, per-channel transfer limits and atomically consumes daily usage.

## Mandatory engineering rules
- Keep Google Java Format. Run `mvn fmt:check`; fix with `mvn fmt:format`.
- Use Maven. Run `mvn verify` before proposing a change.
- Preserve the layered structure: `api`, `application`, `domain`, `infrastructure`.
- All money uses `BigDecimal`, with two decimal places and positive values.
- A transfer validation must lock the account/channel row inside one database transaction before reading or changing daily usage.
- Return RFC 9457 `ProblemDetail` responses for errors. Do not leak exception internals.
- Use Apache Commons Text for rejection-message templates.
- Add/update the OpenAPI contract when changing an endpoint or schema.
- Add unit tests for business logic. Do not add integration tests or enable dependency scanning unless the user changes that requirement.
- Keep secrets out of source control and logs. Use environment variables for credentials.
- Preserve OpenTelemetry tracing and health probes.

## Safety and quality review
For each change, inspect authorization assumptions, input validation, SQL/data integrity, race conditions, error handling, sensitive logs, and resource limits. Run the security reviewer agent for non-trivial changes.
