# Smart Parking Lot Rate Calculator — Software Architecture Plan

## 1. Purpose and scope

Design a deterministic billing component for a multi-level parking facility. At exit, the system receives a ticket/session, evaluates all valid pricing policies, and returns the lowest valid fare with an explanation.

The plan addresses the rate calculator and a thin local application boundary. It does not design a payment processor, remote API, database, parking-space allocation system, or user interface. The source assignment explicitly rules out heavy frameworks, REST APIs, and databases.

### Quality goals

1. Correct, reproducible calculation at time-window boundaries.
2. Simple domain model that separates state from behavior.
3. Auditable decision: explain which rate was selected and why.
4. No binary floating-point or hidden policy ordering.
5. Testable policy logic using standard-library facilities only.

## 2. Functional requirements captured

- Vehicle classes: Motorcycle, Car, Bus. Their multipliers against Car rates are 0.8, 1.0, and 2.0.
- Spot classes exist (Compact and Large are examples) and must be compatible with vehicles, but assignment pricing inputs do not require a spot.
- A ticket records a precise entry timestamp; exit supplies a precise exit timestamp.
- Weekdays are Monday–Friday except dates in the facility's public-holiday calendar.
- Standard Hourly: ceil elapsed duration to full hours. Blocks are floating from entry; rates are $5 for block 1, $3 for block 2, and $2 for each later block. A block receives a 1.5 multiplier if it has any positive overlap with either weekday peak interval, 07:00–10:00 or 16:00–19:00. Window starts are inclusive and ends exclusive.
- Early Bird: entry in [06:00, 09:00), exit in [15:30, 19:00), same local calendar date. Car base fare $15.
- Night Owl: entry in [18:00, next local midnight), exit in [05:00, 10:00) on the next consecutive local date. Car base fare $8.
- Early Bird and Night Owl receive the same loyalty discount: Silver 10%, Gold 20%, Platinum 30%.
- For stays strictly longer than 24 hours, only Standard Hourly is eligible.
- Evaluate all eligible policies and select the lowest fare.
- All rates are USD. The selected final fare is rounded to cents using half-up rounding, as confirmed by the user.

## 3. Explicit assumptions and confirmed choices

1. **Time zone:** Configure an IANA time zone per facility. Convert instants to facility-local dates/times for calendars and schedule windows. Do not rely on the host machine's default zone.
2. **Holiday calendar:** Inject a facility-configured calendar of local dates. The source, editor, and update process are deployment decisions not specified by the assignment.
3. **Loyalty absent/unknown:** Apply no discount, as confirmed by the user.
4. **Rounding:** Keep candidate calculations exact and unrounded until the winning fare is selected; round that fare to USD cents, half-up.
5. **Vehicle multiplier:** Apply the vehicle multiplier to each policy's Car-based result, including flat rates and discounted flat rates. This follows the specification's statement that all rates are defined relative to the Car rate.
6. **No valid special rate:** Standard Hourly is always the fallback for a valid positive-duration stay. If the stay is longer than 24 hours, do not evaluate either special policy as eligible.
7. **Invalid input:** Reject missing or malformed ticket, unsupported vehicle or loyalty values where applicable, and exit instants not later than entry. Do not convert invalid input into a zero fare.
8. **Implementation language:** Not selected; keep this plan language-neutral. Use exact decimal/rational math and the platform's standard time-zone library.
9. **Operational scope:** A reliable ticket issuer and payment processor are external systems. The calculator accepts trusted ticket/session facts and returns a quote; payment capture and ticket authenticity are not implemented here.

## 4. Proposed architecture

Use a small layered, in-process design. Dependencies point inward: adapters call an application service, which orchestrates domain policies; domain code has no dependency on I/O.

```text
Ticket/Exit Adapter
        |
        v
CalculateFare Use Case ---- Facility Pricing Context
        |                     (zone, holiday calendar)
        v
PricingPolicy collection
  | StandardHourly | EarlyBird | NightOwl |
        |
        v
FareQuote (amount, all tied minimum policies, explanation)
        |
        v
Display/Payment Adapter (outside pricing scope)
```

### Components and responsibilities

- **Exit adapter:** Parses an exit event and obtains trusted ticket, vehicle, and loyalty facts. Performs boundary-level syntax validation; it does not calculate prices.
- **CalculateFare use case:** Validates temporal invariants, builds a pricing context, calls every policy, filters ineligible results, selects the minimum exact amount, and returns a quote. It does not stop at the first applicable policy.
- **Pricing policies:** Independent domain rules implementing a small common contract such as `evaluate(session, context) -> optional quote`. An ineligible policy returns no quote; malformed input is an explicit validation error, not “ineligible.”
- **Time-window / calendar helpers:** Shared utilities for half-open intervals, weekday/holiday classification, local schedule checks, and overlap. Reuse these across Early Bird, Night Owl, and peak calculation.
- **Fare arithmetic:** Exact decimal/rational multiplication and discount calculation; final currency rounding happens once after minimum selection.
- **Quote output:** Immutable result with unrounded/rounded amount as appropriate, selected policy, vehicle multiplier, loyalty adjustment (when applied), and enough calculation facts to explain the result.
- **Configuration boundary:** Supplies facility time zone, holiday dates, and (if rates later become configurable) a validated pricing configuration. For the assignment, rate tables may be constants owned by the policy implementation.

### Calculation flow

1. Resolve the ticket and its entry instant and vehicle class; obtain exit instant and loyalty tier.
2. Validate required facts and require exit > entry.
3. Convert timestamps to instants; use the configured facility zone for local date/time predicates.
4. Compute elapsed duration from instants. Determine whether duration > 24 hours; if so, mark both specials ineligible.
5. Evaluate Standard Hourly and each special independently (subject to eligibility). Each eligible policy returns an exact candidate amount and explanation.
6. Select the least exact candidate amount. If multiple policies tie at that exact amount, include all tied policies in the quote; the charged amount is unchanged.
7. Round the selected total to cents, half-up, and return a quote. A payment adapter may then authorize/collect payment; that is outside this plan.

### Time and interval semantics

- Represent entry/exit as instants (or timestamps carrying offsets), not naive local timestamps.
- Derive local date/time via the facility's configured zone.
- Model all rate windows as half-open intervals `[start, end)`. A block `[blockStart, blockEnd)` overlaps a peak interval `[peakStart, peakEnd)` iff `blockStart < peakEnd && blockEnd > peakStart`; this correctly excludes mere touching at an end boundary.
- A floating hourly block begins at `entry + n * 1 elapsed hour`. The number of billed blocks is `ceil(elapsedSeconds / 3600)` (using exact duration units, not rounded floating-point values). A positive sub-hour stay is one block.
- For peak checks, enumerate local dates touched by the block and construct that date's peak windows in the facility zone. A date is a weekday only if Monday–Friday and absent from the holiday calendar.
- Special policies compare local calendar dates and local time-of-day boundaries exactly as specified. Night Owl's “next consecutive calendar day” means the next local date, not merely any later day.
- Invalid/nonexistent local times should be rejected by parsing at the adapter. Pricing windows are generated from configured local schedules using the standard zone rules.

## 5. Domain objects and value types

Prefer immutable values with validated construction; keep behavior on the object that owns the rule or state.

| Domain type | Essential state | Behavior / invariants |
|---|---|---|
| `VehicleType` | Motorcycle, Car, Bus | Supplies its exact multiplier. |
| `ParkingSpot` | Spot ID, level, spot type | Can determine compatibility with a vehicle; not needed by fare calculation. |
| `ParkingTicket` | Ticket ID, entry instant, vehicle type, optionally assigned spot/customer reference | Entry facts are immutable once issued; validates a nonblank identifier and valid timestamp. |
| `ParkingSession` | Ticket, exit instant, loyalty tier | Computes elapsed duration and local entry/exit values with a supplied facility zone; rejects exit <= entry. |
| `LoyaltyTier` | None/unknown, Silver, Gold, Platinum | Maps known tiers to exact discount fractions; missing/unknown maps to zero discount per confirmed decision. |
| `FacilityPricingContext` | Facility zone, holiday calendar, pricing configuration | Defines the calendar against which weekday and local-window rules are evaluated. |
| `PricingPolicy` | Policy identity/rate configuration | Determines applicability and produces a candidate quote without side effects. |
| `FareQuote` | All lowest-fare policy identities, exact candidate fare, final USD amount, explanation | Immutable record of the minimum valid result; final amount is cent-rounded. |
| `Money` / `Rate` | Exact amount and currency | Supports exact addition/multiplication/discount and final half-up rounding; rejects unsupported currencies and negative prices. |

Avoid introducing persistence entities, repositories, event buses, dependency injection frameworks, or a generic rules engine for three fixed policies. Add such structures only if requirements grow.

## 6. Fare calculation details

### Standard Hourly

For elapsed duration `D > 0`, let `N = ceil(D / 1 hour)`. For block `i` (1-based), use base car rate `$5` for `i=1`, `$3` for `i=2`, `$2` otherwise. Multiply a peak-overlapping block by `1.5`, then apply the vehicle multiplier. Sum exact values.

Use each actual floating block `[entry + (i-1)h, min(entry + ih, exit)]` for overlap checks. The final partial block is still charged at its block's full base rate, per the ceil rule. The surcharge applies to the whole block if it overlaps any qualifying peak window for positive duration.

### Early Bird / Night Owl

First evaluate eligibility using local dates and half-open time ranges. If eligible, compute:

`car base fare × (1 − loyalty discount) × vehicle multiplier`

The special policy can be evaluated only when elapsed duration is at most 24 hours. An absent/unknown tier has a 0% discount.

### Best-value selection

Candidate policy evaluation is exhaustive. Selection compares exact amounts before final rounding; choosing based on rounded fares could cause a tie that is not present in the exact results. If there are no eligible candidates, return a domain error; in normal valid sessions Standard Hourly provides a candidate.

## 7. Testing and verification plan

Use the platform's standard testing library; no external test framework is required. Keep pricing policies pure so they can be unit tested without a database, network, clock, or payment system. Inject the clock/calendar context; do not call the system clock inside the domain.

### Unit tests

1. **Validation and duration:** exit before/equal to entry, zero-length and invalid timestamp input; exact 60-minute and one-nanosecond-over durations; sub-hour rounding upward.
2. **Progressive hourly rate:** durations of one, two, and three hours; motorcycle and bus multipliers; verify example 06:30–08:30 produces $12.00 for a weekday Car.
3. **Peak boundaries:** block starts just before 07:00 and overlaps; starts exactly at 07:00; ends exactly at 10:00 (no overlap beyond); starts at 10:00 (no overlap); equivalent 16:00/19:00 cases; non-weekday and public-holiday dates do not surcharge.
4. **Floating blocks and crossing dates:** partial overlap charges the entire segment; overnight block evaluates both local dates; tests around zone/daylight-saving transitions.
5. **Early Bird:** inclusive 06:00 and 15:30; exclusive 09:00 and 19:00; same-date requirement; Silver/Gold/Platinum/none discounts; all vehicle multipliers.
6. **Night Owl:** 18:00 inclusive and next local midnight exclusive entry; 05:00 inclusive / 10:00 exclusive exit; next-date requirement; loyalty discounts and vehicle multipliers.
7. **Eligibility cutoff:** 24 hours versus more than 24 hours; for >24 hours assert neither special is considered even when a constructed session might otherwise appear eligible.
8. **Best value:** a session eligible for both hourly and Early Bird selects the lower quote; verify all policies are evaluated and every policy tied at the exact minimum is reported.
9. **Money:** final half-up cent rounding, no intermediate rounding, exact calculations with fractional multipliers.
10. **Regression/property checks:** total hourly fare is nondecreasing as duration increases; changing to a larger vehicle multiplier cannot reduce a candidate fare; adding a valid discount cannot increase a special fare.

### Component / end-to-end tests

- Feed a representative exit event through the adapter and use case, asserting parsed facts, selected policy, final cents, and explanation.
- Verify invalid or unknown ticket input fails explicitly and no payment action is triggered.
- Verify quote generation is deterministic for the same session and facility configuration.

### Test data and confidence

Use a fixed facility zone and explicit holiday dates in tests; avoid dependence on host locale, current time, or external holiday services. Preserve each specification example as a regression test. Add mutation-focused boundary cases for every inclusive/exclusive edge. No implementation tests have been run because this deliverable is currently a design plan, not code.

## 8. Performance and reliability

- Policy count is constant (three). Policy selection is `O(P)` with `P=3`.
- A direct Standard Hourly calculation is `O(H)` in billed hourly blocks and `O(1)` auxiliary space. For ordinary parking sessions this is negligible. The assignment sets no maximum stay; if very long sessions are a real workload, benchmark first and replace the hourly loop with exact counting of peak-overlapping block indices per local date. Preserve the same boundary behavior and regression tests.
- Avoid repeated time-zone conversions by computing the facility-local date range once per block/window where practical. Do not cache quotes without an explicit cache key including ticket facts, facility zone, holiday-calendar version, and rate version.
- The calculator is deterministic and stateless; it requires no network calls. Holiday/calendar and rate configuration should be immutable for the duration of a calculation.
- Return explicit validation and calculation errors. Do not silently substitute default time zones, holiday calendars, or zero fares.
- Production callers that need exactly-once billing must own transaction/idempotency behavior. The pure calculator alone cannot prevent duplicate payment capture.

## 9. Security and privacy

### Trust boundaries

- Ticket issuer / exit adapter supplies ticket identity, entry time, vehicle class, and customer loyalty facts.
- The calculator treats those facts as untrusted until validated, but ticket authenticity and loyalty entitlement require an upstream trusted source.
- A payment system, if later integrated, is outside the calculator boundary.

### Controls

- Validate ticket identifier length/format, enumerated types, timestamp parsing, positive duration, and configuration values before pricing.
- Do not accept client-supplied calculated fare or vehicle multiplier; derive price from trusted domain/configuration.
- Keep pricing deterministic and return a quote explaining the chosen policy; optionally retain an audit record in a future owning application, with access controls and retention limits.
- Minimize personal data: pricing only needs ticket/session facts and loyalty tier; do not put names, payment-card details, or credentials into the quote or chat history.
- Do not log full ticket identifiers or customer data by default. If operational correlation is required, use a protected reference or appropriately redacted identifier.
- Treat holiday/rate configuration as trusted operational data: validate it, restrict write access in any future configuration interface, and record version/effective date.
- Use standard-library parsers and bounded input lengths; avoid dynamic expression evaluation for rates.
- Keep payment credentials out of this project. A future payment adapter must use its provider's secure tokenization and must not store card data here.

### Threats and limits

Potential concerns include ticket tampering/replay, incorrect loyalty claims, stale holiday or rate configuration, duplicate exit events, and fare disputes. The pure calculator mitigates invalid-input and calculation errors but cannot authenticate tickets, authorize operators, secure a remote transport, or guarantee exactly-once payment. These controls belong to the ticketing, configuration, and payment systems and must be designed if those integrations enter scope.

## 10. Deployment and technology constraints

- Implement as a small library plus a thin command-line or host-application adapter if an executable entry point is required.
- Use only standard-library language/runtime features: exact decimal arithmetic, zone-aware time, and unit-test tooling. No Spring Boot, Django, REST layer, or database.
- Keep rates and vehicle multipliers as validated constants for the assignment unless configuration requirements are confirmed.
- Build and test should be reproducible with documented commands once the implementation language is selected.

## 11. Decisions still requiring confirmation before production

These are not silently assumed as final architecture decisions:

1. **Holiday-calendar operations:** Who owns the authoritative calendar, how are changes loaded, and what effective date/version applies to an active session?
2. **Rate changes:** Are the supplied rates fixed for the life of a parking session, or can rates change while a vehicle is parked? If rates can change, which effective-time rule applies?
3. **Ticket lifecycle / repeated exits:** Does the surrounding system enforce one successful exit/payment per ticket, and how should a duplicate exit request behave?
4. **Language and runtime:** Select an implementation language and supported runtime before turning this plan into code.
5. **Operational controls:** If this becomes a deployed service, define authentication/authorization, audit retention, monitoring, and payment-provider integration at that system boundary.

The confirmed timezone, holiday-calendar approach, loyalty fallback, rounding convention, tie behavior, and Night Owl cutoff are summarized in [README.md](README.md).
