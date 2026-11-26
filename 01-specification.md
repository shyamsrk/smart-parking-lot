# Smart Parking Lot Rate Calculator — Specification

## Status

Stage 1 of the architecture plan. This document captures requirements from the
provided assignment and parking-lot specification, plus decisions confirmed by
the user. Items in **Open questions** are intentionally unresolved; they must
not be silently decided during design or implementation.

## Purpose and scope

Calculate the lowest valid parking fee when a ticket is presented at exit.
Pricing is based on the vehicle class, entry and exit timestamps, applicable
pricing policies, and (where supplied) the customer's loyalty tier.

The plan includes the pricing engine and parking operations covering spot
assignment, vehicle/spot compatibility, levels, and ticket lifecycle, as
confirmed by the user. The source mentions levels and compatible spot sizes
but does not define allocation, occupancy, or ticket lifecycle rules; those
operational details must be specified during design rather than inferred here.

## Domain inputs and outputs

### Inputs required by the pricing rules

- Entry timestamp, recorded when the ticket is issued.
- Exit timestamp, supplied when the ticket is presented for billing.
- Vehicle class: Motorcycle, Car, or Bus.
- Loyalty tier: Silver, Gold, or Platinum; an absent tier receives no
  discount.
- Holiday calendar configuration, based on US federal holidays, configurable
  and defaulting to the current year.

An unrecognized loyalty tier must reject the billing calculation with a
validation error. Other invalid session cases are specified under FR-6.

### Output

- The lowest valid fee in USD among all applicable policies.
- The policy that produced that fee, so the charge can be explained.
- If no special policy applies, the Standard Hourly policy is the applicable
  policy.
- A detailed comparison of applicable policy totals and an hourly breakdown
  for Standard Hourly.

Equal minimum totals require a deterministic policy tie-break. The exact
precedence is Early Bird, then Night Owl, then Standard Hourly.

## Functional requirements

### FR-1: Vehicle classes and multipliers

All policy rates are expressed relative to the Car rate. Apply the vehicle
multiplier to every policy, including flat-rate specials.

| Vehicle | Multiplier |
|---|---:|
| Motorcycle | 0.8 |
| Car | 1.0 |
| Bus | 2.0 |

### FR-2: Standard Hourly policy

This policy is evaluated for all parking sessions.

- Bill duration rounded upward to a whole number of hours.
- Hourly blocks are floating blocks: block 1 begins at entry time; each
  following block begins where the preceding block ends.
- Car base rate is $5.00 for the first block, $3.00 for the second, and $2.00
  for each later block.
- A block incurs a 1.5 peak multiplier if any portion of it overlaps a peak
  window on a weekday that is not a public holiday.
- Peak windows are 07:00 inclusive to 10:00 exclusive, and 16:00 inclusive to
  19:00 exclusive.
- Round each hourly segment to the nearest cent using half-up rounding, after
  applying the applicable vehicle and peak multipliers.
- The total Standard Hourly fee is the sum of its rounded hourly segments.

The policy example in the source specification produces $12.00 for a Car
entering at 06:30 and leaving at 08:30 on a weekday: $7.50 for the first
floating block and $4.50 for the second.

### FR-3: Early Bird Special

Available only on weekdays that are not public holidays. The entry and exit
conditions are both required and refer to the same calendar day:

- Entry: 06:00 inclusive to 09:00 exclusive.
- Exit: 15:30 inclusive to 19:00 exclusive.
- Car base fee: $15.00.
- Apply the applicable loyalty discount and vehicle multiplier.
- Round the final special fee to the nearest cent using half-up rounding.

### FR-4: Night Owl Special

Available only when the entry date and the next consecutive exit date are both
weekdays and neither date is a public holiday:

- Entry: 18:00 inclusive through the end of that calendar day.
- Exit: 05:00 inclusive to 10:00 exclusive on the next calendar day.
- Car base fee: $8.00.
- Apply the applicable loyalty discount and vehicle multiplier.
- Round the final special fee to the nearest cent using half-up rounding.

The weekday/holiday restriction for both dates reflects the user's
clarification.

### FR-5: Loyalty discounts

Apply the same discount schedule to Early Bird and Night Owl:

| Tier | Discount |
|---|---:|
| No tier supplied | 0% |
| Silver | 10% |
| Gold | 20% |
| Platinum | 30% |

An unknown tier is invalid and rejects the billing calculation with a
validation error.

### FR-6: Invalid session data

Reject billing with a validation error for an unknown ticket, missing
timestamps, exit before entry, or equal entry and exit timestamps.

### FR-7: Best valid fare

Evaluate every applicable policy and charge the minimum resulting fee. Do not
stop at the first matching policy. For a stay longer than 24 hours, Early Bird
and Night Owl are invalid; evaluate Standard Hourly exclusively.

## Non-functional and technology constraints

- Deterministic results for the same inputs and configuration.
- Keep pricing behavior understandable, maintainable, and testable.
- Use a platform-agnostic language and standard libraries only; do not use
  heavy frameworks, REST APIs, or databases.
- Currency is USD; no currency conversion is required.
- Document design decisions, assumptions, trade-offs, and alternatives in the
  eventual project documentation.
- Provide focused tests and a reproducible way to run them.

## User-confirmed decisions

- Deliver Markdown files in the workspace and pause for review after each
  stage.
- Early Bird and Night Owl are weekday-only; public holidays are excluded.
- The holiday calendar is configurable, uses US federal holidays, and defaults
  to the current year.
- Use officially observed federal holiday dates; resolve each local date
  against the calendar year of that date.
- Use the parking facility's local time zone for date and pricing-window
  interpretation; hourly blocks measure elapsed time across daylight-saving
  transitions. The facility's exact time-zone identifier is configuration.
- Night Owl requires both the entry date and consecutive exit date to be
  weekdays and non-holidays.
- Vehicle multipliers apply to every rate.
- An absent loyalty tier means no discount; an unknown tier rejects billing
  with a validation error.
- Round each Standard Hourly segment to cents using half-up rounding.
- Round a discounted flat special's final total to cents using half-up
  rounding.
- Reject unknown tickets, missing timestamps, exit-before-entry timestamps,
  and equal entry/exit timestamps with validation errors.
- Return the detailed policy comparison and hourly breakdown; use a
  deterministic tie-break when minimum fees are equal.
- Resolve equal minimum fees in this order: Early Bird, then Night Owl, then
  Standard Hourly.
- Include parking operations such as spot assignment, vehicle/spot
  compatibility, levels, and ticket lifecycle; keep the architecture
  language-neutral.

## Parking-operation design choices

The source does not fully specify parking operations. In response to the
user's instruction to take the easiest approach, the architecture plan uses
these explicit working rules, subject to review before implementation:

- Compact spots accommodate Motorcycles and Cars; Large spots accommodate
  Buses.
- Assign the first compatible available spot in a stable configured order.
- Reject entry with an explicit no-compatible-spot result when the lot is
  full; do not queue or overbook.
- Model one facility. A ticket is active while parked and closes after exit
  completion; closing releases its spot. Payment processing is outside this
  assignment's defined scope.

## Source documents

- `Assignment_Instructions.pdf` — engineering, testing, technology, and
  documentation expectations.
- `The_Smart_Parking_Lot_v2.pdf` — parking-rate domain and pricing rules.
