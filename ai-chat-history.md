# AI Chat History

This log records the prompts, clarifications, and assistant outputs used for
the architecture-planning work. It is being updated as the work progresses.
The attached PDF text is not duplicated here; the source documents are
`Assignment_Instructions.pdf` and `The_Smart_Parking_Lot_v2.pdf`.

## 2026-11-26 — Initial request and source documents

### User

> Give me a software architecture plan as an architect based on the attached
> specification documents. This should include specification, design, domain
> objects, and tests. Before proceeding give me a plan and ask questions; do
> not assume anything or explicitly call out assumptions to get clarification.
> Go step by step instead of one-shot generation.

### Documents provided

- `Assignment_Instructions.pdf`: emphasizes domain modelling, sensible SOLID,
  KISS/YAGNI, maintainability, tests and validation, standard-library-only
  implementation without heavy frameworks/REST/DBs, assumptions and trade-off
  documentation, and AI interaction history.
- `The_Smart_Parking_Lot_v2.pdf`: specifies Motorcycle/Car/Bus multipliers,
  Standard Hourly progressive rates and weekday peak windows, Early Bird and
  Night Owl discounted flat rates, minimum-valid-fare selection, and exclusion
  of both specials for stays over 24 hours.

### Assistant

> I’ve reviewed both PDFs. I’ll proceed in stages rather than generate a
> one-shot plan: (1) resolve specification ambiguities and confirm deliverable
> scope, (2) produce a traceable functional/non-functional specification,
> (3) propose the domain model and architecture with key decisions/trade-offs,
> then (4) outline test scenarios and review the plan with you before moving to
> any later stage. Before Stage 1, please clarify these points; I’ll explicitly
> label anything that remains undecided rather than silently assume it.

### User clarifications

- Deliverable: save Markdown files and pause after each stage.
- Early Bird and Night Owl: weekdays only; exclude public holidays.
- Holiday calendar: configurable, current year as default, US holidays.
- Vehicle multipliers: apply to all rates.
- Loyalty: no tier means no discount; unknown tier is invalid.
- Rounding: round each hourly segment to nearest cent, half-up.

### Assistant follow-up clarification

> I’ll treat the holiday calendar as configurable with the current year as the
> default, apply specials on weekdays only excluding holidays, apply vehicle
> multipliers to all policies, give an absent loyalty tier no discount, reject
> an unknown tier, and round each standard hourly segment half-up to cents.
> Please clarify the US holiday calendar basis, rounding for discounted flat
> specials, and whether an unknown tier rejects billing or follows a separate
> customer-data error path.

### User clarifications

- US federal holidays.
- Round a flat special's final total to nearest cent, half-up.
- Reject billing with a validation error for an unknown loyalty tier.

## Stage 1 — Specification

### Assistant plan

> Stage 1 is the traceable specification. It records confirmed rules and lists
> remaining decisions as open questions instead of selecting defaults. I will
> pause for review before designing the architecture.

### Assistant output

Created [`01-specification.md`](./01-specification.md), covering scope,
pricing inputs/outputs, the vehicle multipliers, all three policies, loyalty,
best-fare logic, technology constraints, user-confirmed decisions, and open
questions about Night Owl date eligibility, holiday observance and year
boundaries, time zone/DST, invalid session data, result details/ties, parking
operations scope, and programming language.

### Assistant review request

> Stage 1 is drafted in `01-specification.md`. Please review it and clarify the
> open points below. If you prefer, answer “leave open” for any item; I’ll carry
> it explicitly as unresolved into later stages and pause before designing
> around it.

The review questions asked for confirmation/corrections and rules for: whether
both Night Owl dates must be eligible weekdays, federal holiday observance and
year-crossing sessions, time zone and DST block behavior, invalid ticket and
timestamp handling, result breakdowns and equal-price ties, parking operations
scope, and language constraints. The first clarification request was
interrupted; the same questions were subsequently asked again and answered.

### User follow-up and answers

> Re run the previous skipped questions.

The user asked to see the specification and answered the review questions:

- Night Owl requires both entry and exit dates to be eligible weekdays.
- Use officially observed US federal holidays and resolve each local date
  against its calendar year.
- Time zone and elapsed-time versus wall-clock DST behavior are unspecified.
- Reject unknown tickets, missing timestamps, exit-before-entry, and equal
  entry/exit timestamps with validation errors.
- Return a detailed policy comparison/breakdown and require a deterministic
  policy tie-break.
- Include parking operations in scope, and keep the architecture
  language-neutral.

Updated [`01-specification.md`](./01-specification.md) with the clarified
requirements. The user then approved Stage 1 and clarified:

- Use the facility's local time zone and elapsed time across daylight-saving
  transitions.
- Resolve equal minimum fees in this order: Early Bird, Night Owl, then
  Standard Hourly.

The exact facility time-zone identifier remains deployment configuration.
The user approved in-memory state for the assignment and directed the
architecture to take the easiest approach for parking operations. The
architecture therefore records explicit working rules (Compact for
Motorcycles/Cars, Large for Buses, stable first-available compatible spot,
reject when full, one facility, and no payment processing) for review.

### Stage 2 — Architecture and domain design

The user approved Stage 2, accepted in-memory state for the assignment with
restart data loss documented, and requested the easiest operational approach.

[`02-architecture.md`](./02-architecture.md) proposes a language-neutral,
single-process modular design; describes domain objects, policy calculation,
application use cases, time handling, storage boundaries, errors, and
trade-offs. The resulting working operational rules are recorded for review.

### Stage 3 — Test plan

Created [`03-test-plan.md`](./03-test-plan.md) with focused unit, policy,
best-fare, calendar, time-boundary, invalid-input, and parking-operation test
scenarios. On review, the user asked for more explicit edge and negative
coverage, specifically separate cases for exit-before-entry and equal
entry/exit timestamps. Updated the plan with those separate rejection cases,
and added a dedicated edge/negative matrix for validation, duration,
pricing-window, holiday, and state-consistency boundaries. The user approved
Stage 3 with no further feedback; the architecture plan is complete.

## Ongoing instruction

### User

> Save the chat history continuously in a separate `ai-chat-history.md` file.

The history will be updated at subsequent stage boundaries and when material
clarifications or decisions are made.

## 2026-11-26 — Implementation technology and planning request

### User

> Use Java 17, Maven, JUnit for the implementation choice. Let's plan the code
> generation plan step by step based technologies.

The user requests a staged code-generation plan using Java 17, Maven, and
JUnit.

### User clarifications and assistant output

- JUnit Jupiter 5.
- No package-name preference.
- Include Maven Wrapper.
- Pause for review after every implementation stage.
- The user confirmed the proposed package root `com.smartparkinglot`.
- The user asked to check software readiness; plan approval and whether to
  start code generation remain pending.

Created [`04-code-generation-plan.md`](./04-code-generation-plan.md). It lays
out project setup, domain, pricing, best-fare, parking operations,
verification, and documentation stages. Environment check: Java/Javac
17.0.21.1, Maven 3.9.9, JUnit Jupiter 5.11.4, Maven Compiler Plugin 3.13.0,
and Surefire 3.5.2 are available locally; Maven Central's wrapper-plugin
metadata is reachable, but the wrapper plugin and wrapper files are not yet
cached/generated. No dependencies were installed and no code has been
generated. The plan is awaiting review.

The user then asked: “Give me the stage names.” The stages are:

1. **Project foundation** — Maven skeleton and wrapper.
2. **Domain model and state** — core values, facility, spots, tickets.
3. **Pricing policies** — hourly, Early Bird, and Night Owl.
4. **Best-fare selection** — exhaustive evaluation, breakdown, and tie-break.
5. **Application use cases** — in-memory entry, quote, and exit flows.
6. **Final verification and documentation** — full Maven test run and README.

No code generation had started at the time the stage names were listed; the
plan was awaiting the user's direction.

The user clarified: “Ask for confirmation after each stage.” Updated
[`04-code-generation-plan.md`](./04-code-generation-plan.md) to require an
explicit confirmation before proceeding from each implementation stage,
including repeating the confirmation gate if revisions are requested. Added
stage-specific test/check criteria and confirmation checkpoints. No
implementation had started at that point.

The user approved beginning Stage A. The existing Maven skeleton and smoke
test were inspected and verified without replacing them. Confirmed Java 17,
JUnit Jupiter 5.11.4, Wrapper 3.3.4 configured for Maven 3.9.9, and both
platform entry-point scripts. Ran `./mvnw test`: 1 test passed, 0 failures,
0 errors. Stage A completed; the user confirmed proceeding to Stage B.

Stage B added the domain model and focused tests for money rounding, vehicle
and loyalty values, parking compatibility, facility/level/spot state, interval
validation, and ticket lifecycle transitions. The focused run passed 15 tests;
the full `./mvnw test` run passed 16 tests, with no failures or errors. Stage B
completed; the user confirmed proceeding to Stage C.

Stage C added policy quote/session types, Standard Hourly, Early Bird, and
Night Owl implementations, and a configurable observed US federal holiday
calendar. Focused pricing tests passed 12 tests; the full `./mvnw test` run
passed 28 tests, with no failures or errors. Test coverage includes exact
hourly block boundaries, floating peak overlaps, holiday and weekday
eligibility, loyalty and vehicle adjustments, and elapsed-time behavior across
both daylight-saving transitions. Stage C is complete; Stage D awaits the
user's confirmation.

The user confirmed proceeding to Stage D. Best-fare calculation now evaluates
all policies, reports each policy outcome, selects the lowest valid fare, and
applies the Early Bird > Night Owl > Standard Hourly tie order. It also
excludes specials for stays over 24 hours. Focused best-fare tests passed 5
tests; the full `./mvnw test` run passed 33 tests, with no failures or errors.
Stage D completed; the user confirmed proceeding to Stage E.

Stage E added in-memory ticket storage and entry, quote, and exit-completion
use cases. Tests cover first-compatible allocation, full-lot behavior,
read-only quoting, successful ticket/spot release, unknown/closed tickets,
invalid and missing timestamps, and state consistency after failures. Focused
application tests passed 7 tests; the full `./mvnw test` run passed 40 tests,
with no failures or errors. Stage E is complete; Stage F awaits the user's
confirmation.

The user confirmed proceeding to Stage F and authorized recreating the
worktree-deleted `README.md`. The README now documents setup, test/package
commands, packages, operating rules, and in-memory limitations. Final clean
verification used Amazon Corretto Java 17.0.21.1 and Maven Wrapper 3.3.4
configured for Maven 3.9.9. `./mvnw clean test` passed all 40 tests with no
failures or errors. All planned implementation stages are complete.

## Implementation publication

The user then requested publication to GitHub. GitHub CLI device authentication
succeeded for the `shyamsrk` account. The implementation passed `./mvnw test`
with 40 tests, 0 failures, and 0 errors. The implementation and project files
are being committed on `main` and pushed to the existing public
`shyamsrk/smart-parking-lot` repository, while preserving the existing README
baseline and `.gitignore`.
