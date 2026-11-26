# Smart Parking Lot Rate Calculator — Test Plan

## Status

Stage 3. This test plan validates the approved pricing specification and the
working operational choices in
[`01-specification.md`](./01-specification.md) and
[`02-architecture.md`](./02-architecture.md). It describes tests; it does not
implement them.

## Test approach

- Use the language's standard test library; add no test framework dependency.
- Test domain policies and value objects directly, then use a small number of
  application-level tests for ticket/spot state changes and best-fare
  selection.
- Supply timestamps, facility zone, holiday calendar, and inventory order
  explicitly in fixtures. Never make tests depend on host-local time or the
  actual current date.
- Use a controlled clock when verifying the current-year holiday-calendar
  default.
- Assert amounts as integer cents and assert the policy breakdown, not just
  the winning total.
- Keep each test independent; reset in-memory stores between application
  scenarios.

## Unit tests

### Money and vehicle/loyalty values

| Case | Input | Expected |
|---|---|---|
| Vehicle multipliers | Motorcycle, Car, Bus | 0.8x, 1.0x, 2.0x |
| Loyalty discounts | absent, Silver, Gold, Platinum | 0%, 10%, 20%, 30% |
| Unknown loyalty tier | Unrecognized supplied value | Validation error; no quote |
| Half-up arithmetic boundary | An exact intermediate value of 1.5 cents | 2 cents |

The final case tests the shared rounding operation. The specified current base
rates and multipliers generally produce whole-cent results, so policy examples
alone may not exercise a half-cent tie.

### Standard Hourly policy

Use a configured facility zone and a holiday-calendar fixture. Unless stated
otherwise, test on a non-holiday weekday outside peak periods.

| Case | Session | Expected |
|---|---|---|
| Positive stay under one hour | 11:00–11:30, Car | One segment; $5.00 |
| Exactly one hour | 11:00–12:00, Car | One segment; $5.00 |
| Just over one hour | 11:00–12:00:01, Car | Two segments; $8.00 |
| Exactly two hours | 11:00–13:00, Car | Two segments; $8.00 |
| Just over two hours | 11:00–13:00:01, Car | Three segments; $10.00 |
| Floating peak example | Weekday 06:30–08:30, Car | $12.00; segment amounts $7.50, $4.50 |
| Ends exactly at peak start | Weekday 06:00–07:00, Car | $5.00; no peak overlap |
| Entire segment in peak | Weekday 09:00–10:00, Car | $7.50 |
| Peak end is exclusive | Weekday 10:00–11:00, Car | $5.00; no peak overlap |
| Segment begins at afternoon peak | Weekday 16:00–17:00, Car | $7.50 |
| Segment ends at afternoon peak end | Weekday 18:00–19:00, Car | $7.50 |
| Segment begins at afternoon peak end | Weekday 19:00–20:00, Car | $5.00 |
| Afternoon partial overlap | Weekday 15:30–16:30, Car | $7.50 |
| Weekend peak clock time | Saturday 09:00–10:00, Car | $5.00 |
| Holiday peak clock time | Configured holiday, 09:00–10:00, Car | $5.00 |
| Vehicle multiplier | Non-peak first hour, Motorcycle / Bus | $4.00 / $10.00 |
| Extended duration | Stay longer than 24 hours | Standard Hourly remains calculable; specials are excluded |

For intervals crossing local midnight, assert each segment's local date is
used to determine weekday and holiday eligibility. Add a daylight-saving test
using an explicit zone fixture: a one-hour block is 60 elapsed minutes even
when the local clock jumps or repeats.

### Early Bird policy

Use weekday, non-holiday fixtures with entry and exit on the same local date.

| Case | Session / tier | Expected |
|---|---|---|
| Inclusive entry and exit starts | 06:00–15:30, Car, no tier | Applicable; $15.00 |
| Entry before exclusive end | Entry 08:59, exit in window | Applicable |
| Entry at exclusive end | Entry 09:00 | Not applicable |
| Exit before inclusive start | Exit 15:29 | Not applicable |
| Exit at exclusive end | Exit 19:00 | Not applicable |
| Wrong calendar date | Entry and exit on different dates | Not applicable |
| Weekday holiday | Otherwise matching window on observed holiday | Not applicable |
| Loyalty reductions | Gold / Platinum, Car | $12.00 / $10.50 |
| Vehicle multiplier | Motorcycle / Bus, no tier | $12.00 / $30.00 |

### Night Owl policy

Use local timestamps on consecutive dates and explicit holiday fixtures.

| Case | Session / tier | Expected |
|---|---|---|
| Inclusive entry and exit starts | Eligible weekday 18:00 to next eligible day 05:00, Car | Applicable; $8.00 |
| Late entry | Eligible entry day 23:59 to next eligible day 05:00 | Applicable |
| Entry before start | Entry 17:59 | Not applicable |
| Exit at exclusive end | Exit 10:00 | Not applicable |
| Non-consecutive dates | Exit is not next local date | Not applicable |
| Ineligible entry date | Entry is weekend or observed holiday | Not applicable |
| Ineligible exit date | Exit date is weekend or observed holiday | Not applicable |
| Platinum discount | Car, Platinum | $5.60 |
| Vehicle multiplier | Motorcycle / Bus, no tier | $6.40 / $16.00 |

### Federal holiday calendar

- Verify weekend federal holidays use their officially observed date.
- Verify each local date is resolved using that date's calendar year, including
  a session spanning December/January.
- Verify the default year is derived from an injected clock, not the machine
  clock.
- Verify an explicitly configured year/calendar overrides the default.

## Best-fare and application tests

| Case | Setup | Expected |
|---|---|---|
| Exhaustive evaluation | Instrument all three policies for one calculation | Every policy is evaluated even if an earlier policy is cheaper |
| Early Bird wins | Car, weekday 06:30–16:00, Gold | Detailed comparison includes hourly and Early Bird; winner Early Bird at $12.00 |
| Tie-break precedence | Test quotes with equal minimum totals for multiple policies | Early Bird wins over Night Owl and Standard Hourly |
| More than 24 hours | Valid ticket with stay greater than 24 hours | Only Standard Hourly is eligible |
| Breakdown | Standard Hourly is evaluated | Every billed segment and its amount are present; sum equals policy total |
| Unknown ticket | Exit request references no stored ticket | Validation error; no fare and no spot mutation |
| Missing entry timestamp | Entry is absent | Validation error; no fare |
| Missing exit timestamp | Exit is absent | Validation error; no fare |
| Exit equals entry | Entry and exit are the same instant | Validation error; no fare |
| Exit precedes entry | Exit instant is earlier than entry | Validation error; no fare |
| Invalid loyalty value | Unknown tier supplied | Validation error before a fare is returned |
| Invalid vehicle class | Unknown vehicle class supplied | Validation error before spot allocation or fare calculation |

For the Early Bird winner case, the Standard Hourly segment totals for the
specified 06:30–16:00 stay should be $31.00; Gold Early Bird is $12.00.

## Parking-operation tests

These tests implement the simple working rules requested for the architecture
plan; revise them if the user changes those rules before implementation.

| Case | Setup | Expected |
|---|---|---|
| Motorcycle compatibility | Available Compact spot | Assignment succeeds |
| Car compatibility | Available Compact spot | Assignment succeeds |
| Bus compatibility | Available Large spot | Assignment succeeds |
| Incompatible spot skipped | Earlier spot cannot fit vehicle; later spot can | Later compatible spot assigned |
| Deterministic selection | Multiple compatible free spots in configured order | First compatible spot assigned |
| Lot full | No compatible free spot | Explicit no-compatible-spot result; no ticket/occupancy mutation |
| Entry operation | Successful allocation | Active ticket references assigned spot; spot becomes occupied |
| Quote is read-only | Calculate exit quote for active ticket | Ticket remains active and spot remains occupied |
| Complete exit | Complete a valid exit | Ticket closes and assigned spot becomes available |
| Invalid exit completion | Unknown or already closed ticket | Explicit domain/validation error; no unrelated spot is released |

## Negative and edge scenarios

These are explicit rejection and boundary checks, not just alternate happy
paths:

| Area | Scenario | Expected behavior |
|---|---|---|
| Session validation | Exit instant is earlier than entry | Reject with validation error; do not calculate or mutate state |
| Session validation | Exit instant equals entry | Reject with validation error; do not calculate or mutate state |
| Session validation | Entry timestamp absent | Reject with validation error |
| Session validation | Exit timestamp absent | Reject with validation error |
| Ticket lookup | Ticket ID is unknown | Reject with validation error |
| Loyalty validation | Tier is not one of the defined values | Reject with validation error; never substitute a discount |
| Vehicle validation | Vehicle class is unknown | Reject before assignment or pricing |
| Spot allocation | Lot has no compatible available spot | Reject entry; do not create an active ticket or occupy a spot |
| Ticket lifecycle | Completion is attempted twice | Reject the second completion; do not release or alter another spot |
| Duration boundary | Positive duration is shorter than one hour | Bill exactly one floating hourly segment |
| Duration boundary | Duration is exactly one or two hours | Do not add an extra segment |
| Duration boundary | Duration is one instant beyond a whole-hour boundary | Add exactly one next progressive segment |
| Peak boundary | Segment ends exactly at peak start | No peak surcharge |
| Peak boundary | Segment starts exactly at peak start | Peak surcharge applies |
| Peak boundary | Segment starts exactly at peak end | No peak surcharge |
| Early Bird boundary | Entry is 06:00 and exit is 15:30 | Policy is applicable |
| Early Bird boundary | Entry is 09:00 or exit is 19:00 | Policy is not applicable |
| Night Owl boundary | Entry is 18:00 or exit is 05:00 | Policy is applicable if both dates qualify |
| Night Owl boundary | Exit is 10:00, or either date is weekend/holiday | Policy is not applicable |
| Holiday/year boundary | Session crosses into a different calendar year | Resolve each relevant local date with its own year's holiday calendar |
| State consistency | Entry allocation fails after spot selection | Do not leave a partial ticket or occupied spot |

## Cross-cutting checks

- Repeated calculation with identical inputs/configuration returns identical
  policy totals, breakdown, and winner.
- A failed validation does not partially reserve/release a spot or change
  ticket state.
- In-memory ticket and spot stores remain consistent after successful entry
  and exit operations.
- Tests use only standard-library facilities and run without a database,
  network service, GUI, or third-party dependency.

## Validation evidence to record when implemented

Record the exact test command, runtime/language version, number of tests
executed, and pass/fail result. If any behavior remains intentionally
unimplemented, identify the corresponding open requirement rather than
reporting the test suite as complete.
