# Smart Parking Lot — Java Code Generation Plan

## Status

All six implementation stages are complete and verified; awaiting the user's
final review. This plan uses the technology choices confirmed by the user and
pauses for review after every implementation stage.

## Confirmed technology choices

- Java 17.
- Maven.
- JUnit Jupiter 5.
- Include Maven Wrapper.
- Pause for user review after every implementation stage.
- No runtime framework, REST API, database, or third-party runtime dependency.

## Proposed project conventions

- Maven standard layout:
  - `src/main/java/...`
  - `src/test/java/...`
  - `pom.xml`
  - Maven Wrapper scripts and `.mvn/wrapper/` configuration
- Package root `com.smartparkinglot` is confirmed by the user.
- Compile with `maven.compiler.release` set to `17`.
- Use JUnit Jupiter as a test-scope dependency and a compatible Maven Surefire
  plugin. Pin the Maven Wrapper distribution in `.mvn/wrapper/`.
- Use only JDK APIs at runtime, including `java.time`; use JUnit only for
  tests.

## Environment readiness check

Checked on 2026-11-26:

- Java runtime and compiler are available: Amazon Corretto OpenJDK/Javac
  17.0.21.1.
- Maven 3.9.9 is available.
- JUnit Jupiter 5.11.4 artifacts are already present in the local Maven
  repository.
- Maven Compiler Plugin 3.13.0 and Surefire Plugin 3.5.2 are cached locally.
- The Maven Wrapper scripts use Wrapper 3.3.4 and are configured to run Maven
  3.9.9. Both Unix and Windows scripts are present; the Unix wrapper has been
  exercised successfully.

The wrapper downloaded its configured Maven distribution when first run. No
application/runtime dependencies were added.

## Stage A completion — 2026-11-26

- Verified the Maven project targets Java 17 and uses JUnit Jupiter 5.11.4.
- Confirmed the Maven Wrapper distribution is Maven 3.9.9.
- Confirmed Unix and Windows wrapper entry-point files are present.
- Ran `./mvnw test`: 1 test, 0 failures, 0 errors; build succeeded.
- The smoke test confirms the runtime is Java 17 or newer.

The user confirmed proceeding to Stage B after reviewing Stage A.

## Stage B completion — 2026-11-26

- Added exact-cent `Money`, vehicle and loyalty values, spot compatibility,
  ticket lifecycle, validated parking intervals, and facility/level/spot
  entities.
- Added domain tests for monetary half-up conversion, value rates,
  compatibility, invalid intervals, occupancy transitions, ticket transitions,
  facility time zone/order, and duplicate identifiers.
- Focused domain tests: 15 passed, 0 failures, 0 errors.
- Full `./mvnw test`: 16 passed, 0 failures, 0 errors.

The user confirmed proceeding to Stage C after reviewing Stage B.

## Stage C completion — 2026-11-26

- Implemented Standard Hourly, Early Bird, and Night Owl policies with
  explicit facility-zone and holiday-calendar dependencies.
- Added observed US federal holiday dates, configurable year/date selection,
  and a clock-injected current-year factory.
- Tested floating hourly blocks, progressive rates, peak boundaries and
  overlap, vehicle and loyalty adjustments, special eligibility boundaries,
  observed holidays, and daylight-saving gaps/repeats.
- Focused pricing tests: 12 passed, 0 failures, 0 errors.
- Full `./mvnw test`: 28 passed, 0 failures, 0 errors.

The user confirmed proceeding to Stage D after reviewing Stage C.

## Stage D completion — 2026-11-26

- Implemented exhaustive policy evaluation, applicable/inapplicable result
  comparisons, lowest-fare selection, and the agreed deterministic tie order.
- Enforced special-policy exclusion for stays over 24 hours, while still
  evaluating every policy.
- Tested exhaustive evaluation, the $12.00 floating-hour example, the
  $31.00 hourly versus $12.00 Gold Early Bird case, tie precedence, and
  over-24-hour special exclusion.
- Focused best-fare tests: 5 passed, 0 failures, 0 errors.
- Full `./mvnw test`: 33 passed, 0 failures, 0 errors.

The user confirmed proceeding to Stage E after reviewing Stage D.

## Stage E completion — 2026-11-26

- Implemented in-memory ticket storage and entry, quote, and exit-completion
  use cases.
- Entry selects the first available compatible spot and rejects a full lot
  without creating a ticket or mutating occupancy.
- Exit quotes are read-only; completion validates the ticket/spot association,
  closes the ticket, and releases only its assigned spot.
- Tested successful entry/exit, deterministic allocation, full-lot rejection,
  unknown/closed tickets, invalid and missing timestamps, quote immutability,
  and state consistency after failures.
- Focused application tests: 7 passed, 0 failures, 0 errors.
- Full `./mvnw test`: 40 passed, 0 failures, 0 errors.

The user confirmed proceeding to Stage F and authorized recreating the deleted
README.

## Stage F completion — 2026-11-26

- Recreated `README.md` with prerequisites, build/test commands, package
  structure, operating rules, and in-memory limitations.
- Java runtime: Amazon Corretto 17.0.21.1.
- Maven Wrapper 3.3.4 runs Maven 3.9.9.
- Ran `./mvnw clean test`: 40 tests, 0 failures, 0 errors; clean build
  succeeded.

All implementation stages are complete.

## Generation stages

Each stage is a bounded implementation increment. After its code and tests are
complete, report the changed files, test command, and results, then ask the user
to confirm before starting the next stage. Do not begin the next stage until
the user confirms. If changes are requested, revise the current stage and ask
for confirmation again.

### Stage A — Maven skeleton

Create `pom.xml`, the Maven Wrapper, source/test package directories, and a
small JUnit smoke test that verifies the test engine discovers and runs a test.
Verify the Java 17 compiler configuration and that the Unix and Windows
wrapper scripts are present.

**Tests and checks:** run `./mvnw test`; confirm the smoke test is discovered
and passes, the project compiles with Java 17, and the wrapper can invoke
Maven. Record the pinned Wrapper distribution.

**Confirmation gate:** present the project files, Maven/Java configuration,
Wrapper version, and test result; ask for confirmation before Stage B.

### Stage B — Domain values and parking state

Implement the minimum domain types needed by the approved design:

- `Money`, with exact cents and explicit half-up conversion at pricing
  boundaries.
- Vehicle class, loyalty tier, spot type, and ticket lifecycle state.
- Facility, level, spot, and ticket entities with encapsulated state and
  validated transitions.
- A facility-local time-zone setting and validated entry/exit instants.

Add focused tests for valid values, invalid values, spot compatibility, state
transitions, and explicit validation failures. Cover exact-cent `Money`
behavior and reject invalid ticket transitions without changing state. Keep
the assigned simple operational rules visible in code and tests.

**Tests and checks:** run the focused domain JUnit tests, then `./mvnw test`;
confirm invalid construction/transitions fail explicitly and valid transitions
preserve the expected state.

**Confirmation gate:** summarize the implemented model and its focused/full
test results; ask for confirmation before Stage C.

### Stage C — Pricing policies

Implement the policy contract and the three concrete policies:

- Standard Hourly progressive floating segments, weekday/holiday peak
  overlap, vehicle multiplier, elapsed-time duration rounding, and hourly
  breakdown.
- Early Bird applicability, flat fee, loyalty reduction, and vehicle
  multiplier.
- Night Owl applicability on consecutive eligible local dates, flat fee,
  loyalty reduction, and vehicle multiplier.

Use explicit facility-zone and holiday-calendar dependencies. Do not rely on
the host default time zone or current system date in tests.

Add tests for rate progression, exact and partial interval boundaries, weekday
and observed-holiday behavior, daylight-saving elapsed-time handling,
rounding, and vehicle/loyalty combinations.

**Tests and checks:** run the focused policy tests, then `./mvnw test`.
Include assertions for the $12.00 hourly example and policy breakdown amounts;
verify tests do not depend on the host zone or current date.

**Confirmation gate:** report the policy behavior, boundary cases, and test
results; ask for confirmation before Stage D.

### Stage D — Best-fare calculation

Evaluate every policy, produce the detailed comparison, and select the lowest
valid fee. Apply the agreed equal-fee order: Early Bird, Night Owl, then
Standard Hourly. Reject stays over 24 hours for both specials.

Add tests for exhaustive evaluation, the documented $12.00 floating-hour
example, the Early Bird-versus-hourly scenario, equal-price tie behavior,
special exclusion over 24 hours, and calculation breakdowns.

**Tests and checks:** run focused best-fare tests, then `./mvnw test`; verify
all policies are evaluated and the selected quote, comparison, and tie-break
match the specified outcomes.

**Confirmation gate:** show representative comparisons and test results; ask
for confirmation before Stage E.

### Stage E — Parking application use cases

Implement entry/spot assignment, exit quote calculation, and exit completion
with simple in-memory stores. Use the agreed working rules: Compact for
Motorcycles/Cars, Large for Buses, first compatible spot in configured order,
explicit rejection when full, one facility, and no payment processing.

Keep quote calculation read-only; close the ticket and release its spot only
through the completion use case. Add application tests for successful entry
and exit, no-space failures, unknown/closed tickets, invalid timestamps, and
state consistency after failures. Include a test that failed entry/exit
operations do not leave partial ticket or occupancy changes.

**Tests and checks:** run focused application tests, then `./mvnw test`;
verify successful state changes, read-only quotes, and unchanged state after
each tested failure.

**Confirmation gate:** report the use cases, state-consistency results, and
test output; ask for confirmation before Stage F.

### Stage F — Final verification and documentation

- Run the full suite with `./mvnw test`; fix failures before reporting
  completion.
- Record Java/Maven versions, command, and test result.
- Update the README with setup/run commands, package structure, assumptions,
  trade-offs, and in-memory restart limitations.
- Keep [`ai-chat-history.md`](./ai-chat-history.md) current with prompts,
  decisions, generated stages, and validation results.

**Tests and checks:** run `./mvnw test` from a clean build; report the number
of tests, Java/Maven versions, command, and pass/fail result. Documentation
must give accurate setup and test commands.

**Confirmation gate:** provide the final implementation and verification
summary, then ask the user to confirm the completed plan of work.

## Test-first workflow

For each domain behavior, add or update the smallest focused JUnit test first,
implement the behavior, run the focused test, then run the complete suite at
the end of the stage. Do not write tests that only mirror trivial accessors.

## Explicit constraints and non-goals

- No application framework, database, REST layer, GUI, or external runtime
  service.
- Maven Wrapper is for reproducible builds; it does not change the runtime
  architecture.
- In-memory ticket and spot state is lost when the process restarts.
- The facility's actual time-zone ID is configuration; test fixtures may use
  an explicit zone but must not imply it is the real facility location.
- Each next implementation stage requires the user's explicit confirmation;
  approval of one stage does not authorize the next.
