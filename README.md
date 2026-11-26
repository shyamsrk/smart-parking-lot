# Smart Parking Lot

A Java 17 parking-lot domain and application module that assigns parking spots
and calculates the lowest valid fare across Standard Hourly, Early Bird, and
Night Owl pricing policies.

## Requirements

- JDK 17 or newer.
- Maven Wrapper is included; the first run may download the configured Maven
  distribution.

## Build and test

Run the complete test suite:

```sh
./mvnw clean test
```

On Windows, run:

```bat
mvnw.cmd clean test
```

To package the project:

```sh
./mvnw package
```

The project has no standalone server, command-line UI, database, or payment
processor. The application use cases are exposed through Java classes.

## Project structure

- `com.smartparkinglot.domain` — exact-cent money, vehicle and loyalty values,
  facility/level/spot/ticket state, and validated parking intervals.
- `com.smartparkinglot.domain.pricing` — pricing policies, holiday calendar,
  policy quotes, and best-fare comparison.
- `com.smartparkinglot.application` — entry, exit quote, and exit completion
  operations with an in-memory ticket store.
- `src/test/java` — JUnit Jupiter tests for domain values, pricing rules,
  best-fare selection, and parking operations.

## Operational rules and limitations

- Compact spots fit Motorcycles and Cars; Large spots fit Buses.
- Entry selects the first available compatible spot in facility order and
  rejects entry if none is available.
- Exit quoting does not alter ticket or spot state. Exit completion closes the
  ticket and releases its assigned spot.
- The facility time zone and holiday calendar are explicit configuration.
  Pricing uses the facility's local time for date and window rules, and elapsed
  time for hourly blocks across daylight-saving changes.
- The federal holiday calendar supports configured years and observed dates;
  its current-year factory accepts an injected clock for deterministic setup.
- Ticket and spot state is held in memory and is lost when the process
  restarts. The design models one facility and does not include durability,
  concurrency across processes, payment processing, REST, or a GUI.
