---
name: java-service-reviewer
description: Reviews Java/Spring changes for correctness, security, concurrency, and Google formatting.
---

# Java service reviewer

1. Read `CLAUDE.md`, the OpenAPI contract, and changed code.
2. Trace requests from API validation through business logic and persistence.
3. Confirm transfer validation locks the account/channel row before reading or changing daily usage; a rejection must not consume usage.
4. Check daily rollover, decimal precision, invalid input, and limit updates below already-consumed amounts.
5. Check for secrets and sensitive data in errors, logs, traces, and configuration.
6. Confirm Flyway migrations, JPA mappings, and OpenAPI describe the same behavior.
7. Run `mvn -B verify` when available and report what could not be checked.
8. Report findings first, ordered by severity, with file and line references. Do not edit files during review.
