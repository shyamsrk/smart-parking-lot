# smart-parking-lot

Smart parking Lot Assignment

## Rate Calculator Architecture

This project currently contains the architecture plan for the rate calculator described in the supplied assignment. It does not yet contain an implementation.

## Documentation

- [Architecture plan](architecture-plan.md) — proposed design, domain model, behavior, testing, performance, and security.
- [AI interaction history](ai-chat-history.md) — prompts, specification text, and clarification decisions recorded for this work.

## Confirmed assumptions and decisions

- **Facility clock:** Each facility has an explicitly configured IANA time zone. Entry and exit are precise instants; weekday, holiday, and pricing-window checks use the facility's local calendar.
- **Holidays:** A configurable facility holiday calendar determines which otherwise-weekday dates are excluded from weekday pricing. The owner and update process for this calendar remain to be decided before production.
- **Unknown loyalty tier:** Missing or unrecognized loyalty tier receives no discount (the undiscounted rate).
- **Currency:** USD only. Use exact decimal arithmetic and round the selected final fare to cents using half-up rounding; do not round intermediate candidate calculations.
- **Policy selection:** Evaluate every applicable policy and charge the minimum valid fare. A stay longer than 24 hours is eligible only for Standard Hourly.
- **Exact fare ties:** Return all policies tied at the lowest exact fare in the quote; they represent the same charge.
- **Night Owl entry cutoff:** Accept entry from 18:00 up to, but not including, the next local midnight.
- **Scope:** This plan covers the rate-calculation domain and its local application boundary, not payment processing, a network API, database, or a full parking-operations platform.
- **Technology:** Follow the assignment constraint: use a platform-agnostic language and standard library only; do not introduce a heavy framework or database. The implementation language is not selected in this plan.

## Trade-offs and alternatives

- A small, in-process calculator with explicit policy objects is preferred over a service/distributed architecture: there are only three policies, and the assignment excludes heavy frameworks and databases.
- Holiday rules and facility time zone are dependencies supplied to the pricing domain, rather than global constants. A third-party holiday service is not assumed; it would add availability and reproducibility concerns.
- Decimal or rational arithmetic is preferred over binary floating point for fare calculations. Rounding only the final selected fare avoids order-dependent intermediate rounding.
- The calculator should return a quote containing its selected policy and calculation details, rather than only a number, to support explanation and audit without coupling the domain to logging or payment.
- The initial design favors straightforward hourly evaluation. If measured workloads include exceptionally long stays, optimize peak-block counting without changing the pricing contract; do not add speculative caching or persistence.

See the architecture plan for open decisions that need confirmation before a production deployment.
