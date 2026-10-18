# jmouse-notifier-jpa — a durable publication and delivery outbox

> **Agent orientation file.** The source wins.
> Module: `jmouse-notifier-jpa` · Package: `org.jmouse.notifier.jpa` · Java 21.

## What it is

The database `NotificationStore` for [notifier core](JMOUSE_NOTIFIER.md). Publications, idempotency keys,
delivery state and pending retries survive process restarts. This module does not deliver messages or
start a retry worker; the notifier and its channel implementations do that.

## The map

| Type | Responsibility |
|---|---|
| [JpaNotificationStore](../jmouse-notifier-jpa/src/main/java/org/jmouse/notifier/jpa/JpaNotificationStore.java) | Implement registration, lookup, delivery persistence and due-work claims |
| [NotifierPublication](../jmouse-notifier-jpa/src/main/java/org/jmouse/notifier/jpa/NotifierPublication.java) | Write-once publication row and unique publication idempotency key |
| [NotifierDelivery](../jmouse-notifier-jpa/src/main/java/org/jmouse/notifier/jpa/NotifierDelivery.java) | Mutable state for one publication/channel/address delivery |
| [NotifierDialect](../jmouse-notifier-jpa/src/main/java/org/jmouse/notifier/jpa/migration/NotifierDialect.java) | Resolve supported SQL dialect from the actual data source |
| [NotifierMigrations](../jmouse-notifier-jpa/src/main/java/org/jmouse/notifier/jpa/migration/NotifierMigrations.java) | `db/notifier` locations and `notifier_schema_history` identity |

## Wiring

Construct `JpaNotificationStore(entityManagerFactory)` and pass it to `Notifier`. Include
`org.jmouse.notifier.jpa` in entity scanning. Run the module's MySQL/PostgreSQL migration resources before
using it; [the notifier starter](JMOUSE_NOTIFIER_SPRING_BOOT.md) supplies the optional automatic wiring.
The tables are `notifier_publications` and `notifier_deliveries`, not product-owned notification tables.

`register(publication)` returns the existing publication for a repeated idempotency key, including a
concurrent-registration race resolved by the database unique constraint. `save(delivery)` stores/replaces
the state of one delivery. `deliveriesOf(publicationId)` reads its tracked addresses. `due(now, limit)`
claims due pending deliveries rather than merely listing them.

## Claim and transaction semantics

Each call opens its own entity manager and resource-local transaction. A message already sent cannot
be unsent by rolling back the caller's domain save. Publish after the product transaction commits if
notification must describe a committed change. The implementation requires a RESOURCE_LOCAL persistence
unit; under JTA, supply an integration appropriate to that transaction environment.

`due` compares the observed `next_attempt_at` before moving a candidate five minutes forward. Another
worker cannot claim the same observed row; a dead worker's lease becomes due again. Keep attempt duration
and retry scheduling consistent with this fixed `CLAIM` lease.

Delivery is **at least once**: if a channel sends successfully and the process dies before saving the
receipt, the pending attempt is retried. Publication idempotency does not provide exactly-once channel
delivery. Domain transactions, channel retries and publication deduplication are separate guarantees.
The store has no built-in retention scheduler; use the public store contract and the adopter's lifecycle policy.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-notifier-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-notifier` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.notifier.jpa`

| Type | Responsibility / contract |
|---|---|
| [JpaNotificationStore](../jmouse-notifier-jpa/src/main/java/org/jmouse/notifier/jpa/JpaNotificationStore.java) | Publications and their delivery outbox in the database — notifier_publications and notifier_deliveries. A pending retry survives a restart; an idempotency key survives it too. |
| [NotifierDelivery](../jmouse-notifier-jpa/src/main/java/org/jmouse/notifier/jpa/NotifierDelivery.java) | The state of one publication at one address — the outbox row. |
| [NotifierPublication](../jmouse-notifier-jpa/src/main/java/org/jmouse/notifier/jpa/NotifierPublication.java) | One publication, persisted — what was said, by whom, to whom, once. |

#### `org.jmouse.notifier.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [NotifierDialect](../jmouse-notifier-jpa/src/main/java/org/jmouse/notifier/jpa/migration/NotifierDialect.java) | 🗣️ The SQL dialects this library ships migrations for, and how one is chosen. |
| [NotifierMigrations](../jmouse-notifier-jpa/src/main/java/org/jmouse/notifier/jpa/migration/NotifierMigrations.java) | 🚚 Where this library's own schema lives, and under what history. |

### Packaged resources

- [db/notifier/mysql/V000001__create_notifier_outbox.sql](../jmouse-notifier-jpa/src/main/resources/db/notifier/mysql/V000001__create_notifier_outbox.sql).
- [db/notifier/postgresql/V000001__create_notifier_outbox.sql](../jmouse-notifier-jpa/src/main/resources/db/notifier/postgresql/V000001__create_notifier_outbox.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
