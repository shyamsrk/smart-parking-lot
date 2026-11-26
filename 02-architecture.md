# Smart Parking Lot Rate Calculator — Architecture and Domain Design

## Status

Stage 2. This is a proposed, language-neutral design based on the approved
specification in [`01-specification.md`](./01-specification.md). It is not an
implementation. Recommendations and unresolved operational rules are called
out explicitly. The chat log is maintained in
[`ai-chat-history.md`](./ai-chat-history.md).

## Architecture goals

- Keep pricing rules deterministic and isolated from ticket/spot state.
- Make the three known pricing policies independently understandable while
  always evaluating every applicable policy.
- Keep operational state transitions explicit and testable.
- Respect the standard-library-only, no-database, no-REST constraints.
- Avoid a generic pricing-rule engine or framework for the three policies.

## Proposed shape

Use a small, single-process modular application with a domain core and thin
application and storage boundaries:

```text
Entry / exit boundary
        |
        v
Application use cases
  - issue ticket and assign spot
  - calculate exit quote
  - complete exit
        |
        v
Domain
  - facility, level, spot, ticket, parking session
  - hourly / early-bird / night-owl policies
  - best-fare selection and money calculations
        |
        +---- ports: ticket store, spot store, holiday calendar, time source
                         |
                         v
                Standard-library adapters
                - in-memory stores for assignment runtime
                - configured federal holiday calendar
```

Dependencies point inward: application use cases coordinate domain behavior;
domain objects do not depend on storage or user-interface details. The
assignment does not require a GUI, CLI, or network API, so this design leaves
the outer interaction adapter unspecified.

### Why a modular single process

The assignment has a small, fixed policy set and forbids heavy frameworks,
REST, and databases. A modular single-process design minimizes operational and
build complexity while preserving testable boundaries. Separate services,
message queues, a rule engine, and a plugin architecture are not justified by
the provided requirements.

## Domain model

### Value objects

- **`Money`** — USD amount represented exactly in cents. Multipliers and
  discounts must not use binary floating-point arithmetic; use exact decimal
  or rational arithmetic and apply the approved half-up rounding at the
  specified calculation boundary.
- **`VehicleClass`** — Motorcycle, Car, or Bus, with the corresponding
  multiplier.
- **`LoyaltyTier`** — Silver, Gold, Platinum, or absent. Parsing any other
  supplied tier is a validation failure.
- **`TicketId`, `SpotId`, `FacilityId`** — distinct identifiers to prevent
  accidental interchange of IDs.
- **`TimeInterval`** — validated entry and exit instants; entry must precede
  exit. Display, weekday, and pricing-window interpretation use the
  facility-configured local time zone.

### Entities and aggregates

- **`ParkingFacility`** — owns facility configuration, levels, and the spot
  inventory. It identifies the facility-local time zone and the holiday
  calendar configuration.
- **`ParkingLevel`** — groups spots and provides a level identifier.
- **`ParkingSpot`** — has a spot type and availability state; answers whether
  it can accommodate a vehicle class. As the simplest interpretation of the
  source examples, Compact supports Motorcycles and Cars; Large supports
  Buses.
- **`ParkingTicket`** — records ticket ID, vehicle class, entry instant,
  assigned spot, and lifecycle state. It owns the valid state transitions;
  callers cannot directly mutate its status.
- **`ParkingSession`** — immutable pricing input derived from an active ticket
  and exit instant, with vehicle class and optional loyalty tier. Keeping the
  pricing input independent of mutable spot state prevents pricing from
  changing when a spot is released.

### Pricing model

- **`RatePolicy`** — a small contract for determining applicability and
  producing a `PolicyQuote` for a `ParkingSession`. Keep the implementations
  concrete and limited to the policies in the specification.
- **`StandardHourlyPolicy`** — rounds elapsed duration upward to whole hours,
  creates floating one-hour segments from entry, determines peak overlap per
  segment in facility-local time, applies the progressive base rate and
  vehicle/peak multipliers, and returns segment details plus total.
- **`EarlyBirdPolicy`** — checks the same-local-date entry/exit windows and
  eligible weekday/non-holiday condition, then applies flat rate,
  vehicle multiplier, and loyalty discount.
- **`NightOwlPolicy`** — checks the overnight windows and consecutive eligible
  local dates, then applies flat rate, vehicle multiplier, and loyalty
  discount.
- **`PolicyQuote`** — policy identifier, total `Money`, applicability result,
  and an explanation/breakdown. Inapplicable policies should not be represented
  as zero-price quotes.
- **`FareComparison`** — contains every evaluated policy outcome, the winning
  policy/quote, and tie-break outcome.
- **`BestFareCalculator`** — evaluates all policies, filters to applicable
  quotes, and chooses the lowest amount; for equal amounts, applies the
  approved precedence: Early Bird, Night Owl, Standard Hourly.
- **`HolidayCalendar`** — answers whether a facility-local date is an
  officially observed US federal holiday. Resolve each date using its own
  calendar year; the default calendar year is the current year.

The `RatePolicy` seam is useful for keeping each named policy's rule and
breakdown separate. It should not grow into a configurable rule engine or
support arbitrary user-authored rates unless new requirements justify that.

## Application use cases

### Issue ticket and assign spot

1. Validate vehicle class and entry timestamp.
2. Select the first available compatible spot in a stable configured order.
3. Create a ticket containing its entry time, vehicle, and assigned spot.
4. Persist the ticket and mark the spot occupied as one logical operation.
5. If no compatible spot is available, return an explicit no-compatible-spot
   result; do not queue or overbook.

### Calculate exit quote

1. Find and validate the active ticket.
2. Validate exit time and loyalty tier.
3. Create an immutable `ParkingSession`.
4. Ask each policy for an applicable quote or a not-applicable result.
5. Build a detailed comparison and select the minimum according to the
   approved tie-break.
6. Return the quote and breakdown without changing ticket or spot state.

### Complete exit

Separate quote calculation from ticket/spot mutation. After the outer
interaction confirms exit completion, transition the ticket to its terminal
state and release its spot together. Payment collection and payment failure
are outside the defined scope; the outer interaction is responsible for
calling completion only when appropriate.

## Pricing boundaries and time handling

- Represent timestamps as instants plus the configured facility time zone.
  Do not depend on the machine's default zone.
- Build hourly segments as elapsed one-hour intervals starting at entry.
  The last segment may extend beyond the actual exit because duration is
  rounded up, as required.
- Convert each interval to facility-local time to test overlap with the
  weekday peak windows; peak end is exclusive and start is inclusive.
- Resolve holiday eligibility for each relevant local date with that date's
  year-specific observed federal calendar.
- Standard hourly segment totals are rounded half-up to cents individually.
  Flat specials are rounded half-up after the vehicle multiplier and loyalty
  discount are applied.
- Keep the candidate evaluation order separate from policy precedence: all
  policies must be checked before the winner is selected.

## State and storage

The user accepted standard-library in-memory stores for active tickets and
spot occupancy in the assignment. All state is lost when the process restarts;
this is an explicit submission/demo limitation, not durable production
storage.

Keep the application-facing store contracts small (for example, find/save
ticket and reserve/release spot); avoid adding repository layers for immutable
objects or creating a general persistence framework. If restart recovery or
multiple processes are required, the current technology constraints conflict
with that need and must be revisited rather than hidden behind an in-memory
implementation.

## Error behavior

Use explicit validation/domain errors for unknown tickets, missing or
inconsistent timestamps, invalid vehicle/tier data, no compatible spot, and
invalid ticket state. Do not return a zero fee or a success-shaped response
when input validation or state transitions fail.

## Trade-offs and open operational rules

- **In-memory state:** simplest under the no-database requirement, but not
  durable or safe across multiple application processes.
- **Fixed policies:** three direct policy implementations are clearer than a
  generic rules DSL; adding a policy later requires an explicit code change.
- **Pricing versus exit completion:** separating quote generation from ticket
  closure prevents a failed outer billing/exit flow from silently freeing a
  spot. Payment itself is not modeled.
- **Working operational rules:** Compact supports Motorcycles and Cars; Large
  supports Buses; choose the first compatible free spot in stable configured
  order; reject entry when no spot is available; model one facility. These
  simple rules follow the user's direction to take the easiest approach and
  should be confirmed before implementation.
- **Still deployment-specific:** the facility's configured time-zone
  identifier.

These are decisions to resolve before implementation, not defaults embedded
in the model.
