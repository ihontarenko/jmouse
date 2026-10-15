# jmouse-scheduler and jmouse-scheduler-spring-boot

> **Agent orientation file.** Written so you do not have to read the module to use it. If something
> here disagrees with the code, the code wins — and fix this file in the same change.
>
> Modules: `jmouse-scheduler` (package `org.jmouse.scheduler`, no Spring) and
> `jmouse-spring/jmouse-scheduler-spring-boot` (package `org.jmouse.scheduler.spring`) · Java 21
> Depends on: `jackson-databind` (parameters as JSON in the JDBC store) and `slf4j-api`. Not `jmouse-core`,
> not `jmouse-jdbc`.
> Origin: the Kiwi note *A jMouse scheduler with handlers hung on it…* (`.kiwi/…-5bd3b5.md`).

## What it is — and what it is not

Durable **triggers** — *when* (a `Schedule`), *what* (a handler key plus parameters), *after downtime* (a
`MisfirePolicy`) and *on what condition* (an optional gate) — fired by **one polling loop** and **claimed
across processes** with a lease, so two healthy JVMs over one database never fire the same occurrence. One that
dies mid-dispatch has its occurrence re-dispatched under the same identity: **at-least-once**, with dispatchers
idempotent on `Occurrence.identity()`.

The library **dispatches and nothing more**. It owns no job queue: a fired occurrence goes to a
`TriggerDispatcher` the product implements, and a product with a durable queue enqueues there. That is the
note's recommendation — *triggers first, extract the queue when a second product needs one*.

## The model — package `org.jmouse.scheduler`

| Type | What |
|---|---|
| `Schedule` | sealed: `Cron(CronExpression, ZoneId)`, `Interval(Duration period, Instant anchor)`, `Once(Instant)`. Factories `Schedule.cron(text, zone)`, `every(period)` (anchored at the epoch, so hourly fires on the hour UTC), `every(period, anchor)`, `once(instant)`. `nextAfter(after)` is strictly after; `firstFiring(registeredAt)` is `nextAfter` except for `Once`, whose first firing is its instant **even when already past** (so it meets its misfire policy rather than silently never firing). Instants are kept to the millisecond, the store's resolution. |
| `CronExpression` | `parse(text)`, `nextAfter(instant, zone)`, `source()`. Equality by meaning (`@daily` equals `0 0 * * *`). |
| `MisfirePolicy` | sealed: `skip()` (**default**), `fireOnce()`, `catchUp(n)` with `1 ≤ n ≤ 1000`. |
| `Trigger` | record + `Trigger.builder("id").handler(key).parameters(map).schedule(…).misfirePolicy(…).gate(key).enabled(…).build()`. |
| `TriggerId` | `[a-z0-9][a-z0-9._-]{0,199}` — e.g. `nightly-vectorise`, `source-42.scan`. |
| `TriggerParameters` | JSON-shaped map; typed accessors `requireText/Long/Decimal/Boolean`, optional `text/longValue/decimal/booleanValue`. Integers are held as `Long`, decimals as `BigDecimal` with trailing zeros stripped (`5.0` reads as `5`, `2.50` as `2.5`). Non-JSON values are refused at construction. |
| `Occurrence` | one firing: trigger, handler key, parameters, `scheduledAt`, `firedAt`, `late`. `identity()` = `<trigger>@<epoch ms>` — **the idempotency key**. |
| `OccurrenceRecord` / `OccurrenceOutcome` | history line: `FIRED`, `SKIPPED` (misfire; one record may stand for many, `count`), `REFUSED` (gate, with its reason), `FAILED` (no handler here, or the dispatcher threw). Reason cut at 1000 chars. |
| `Scheduler` | administration: `register(trigger)`, `enable(id)`, `disable(id)`, `remove(id)`, `find(id)`, `triggers()`, `history(id, limit)`. Never dispatches. |
| `SchedulerRunner` | the loop: `start()`, `stop()`/`close()`, `running()`, and `pollOnce()` — one pass on the calling thread. |
| `SchedulerSettings` | `pollInterval` (5 s), `maximumJitter` (1 s), `misfireThreshold` (1 min), `batchSize` (100), `leaseDuration` (30 s). `DEFAULTS`. |
| `HandlerRegistry` / `GateRegistry` | immutable, by key; **two entries on one key fail construction**. `GateRegistry.empty()`. |
| `InProcessDispatcher` | runs the handler on a virtual thread; `AutoCloseable`. Not durable. |
| `ManualClock` | a `Clock` that moves on `advance(…)`/`set(…)` — for tests here and in products. |

## The seams — package `org.jmouse.scheduler.spi`

| Seam | Implemented by | Contract |
|---|---|---|
| `TriggerStore` | `InMemoryTriggerStore`, `JdbcTriggerStore`, or the product | `save`, `find`, `findAll`, `remove`, `findDue(now, limit)`, **`claim(id, expectedVersion, plannedAt, leaseDuration)`** → `Optional<TriggerLease>`, **`confirm(lease, nextFireAt)`**, `record`, `history`. Thread-safe. |
| `TriggerDispatcher` | the product (or `InProcessDispatcher`) | `dispatch(occurrence)`. Called on the polling thread — **enqueue, do not work**. |
| `TriggerHandler` | the product | `key()`, `validate(parameters)` (at registration), `handle(occurrence)`. |
| `TriggerGate` | the product | `key()`, `evaluate(occurrence)` → `GateVerdict.pass()` / `refuse(reason)`. |

## Cron — written for this module, not a dependency

Five fields `minute hour day-of-month month day-of-week`, or six with a leading `second`. Lists, ranges,
`*/n`, `a/n`, `a-b/n`, `JAN`–`DEC`, `SUN`–`SAT`, `0`/`7` = Sunday, `?`, `L` (last day of month, day-of-month
field only), macros `@yearly @annually @monthly @weekly @daily @midnight @hourly`. **Refused at parse time**:
`W`, `#`, `L` in day-of-week, years, `@reboot`.

- **Both day fields restricted → either matches** (Vixie cron): `0 0 13 * FRI` fires on the 13th *and* on Fridays.
- **DST gap**: a wall-clock time that does not exist fires **once, at the instant the clocks jump**; every slot
  inside one gap collapses into that one firing.
- **DST overlap**: a time that occurs twice fires **once, at its earlier occurrence** — so an hourly
  expression does not fire in the repeated hour.
- An expression that never matches within **10 years** (`0 0 30 2 *`) yields no next firing — the trigger is exhausted.

**Why not cron-utils (or Quartz's `CronExpression`)?** cron-utils is not in this machine's repository, so
the offline build could not take it; but the stronger reason is that the semantics a scheduler's tests turn
on — Vixie day matching, what a DST gap and overlap mean, refusing rather than ignoring unsupported syntax —
are a few hundred lines that this module now states and tests, rather than a dependency's defaults to
discover. A cron dialect is a contract with every stored trigger, so it should be one this codebase owns.

## Misfires

An occurrence is **late** when found more than `misfireThreshold` after its scheduled time; anything inside
the threshold is **on time and always fires**. `SchedulerSettings` refuses a threshold that does not exceed
`pollInterval + maximumJitter`, or ordinary polling would misfire.

| Policy | Late occurrences | Notes |
|---|---|---|
| `SKIP` (default) | none fire | ⚠️ default on purpose: a trigger that spends money must not multiply its spend after an outage unless asked in writing |
| `FIRE_ONCE` | the most recent one fires | not even that one when an on-time occurrence fires in the same pass |
| `CATCH_UP(n)` | the most recent `n`, oldest first | `n ≤ 1000` |

Everything late that did not fire is **one** `SKIPPED` history record with the count and the time range. The
scan counts at most **100,000** late occurrences, then jumps to the present (the record then says "at least").

## Claim, dispatch, confirm — at-least-once

One pass per due trigger: **plan** (pure — `OccurrencePlan`) → **claim** a lease → record the dropped → for
each kept occurrence, oldest first: handler known? → gate → dispatch → record → **confirm**.

- **Claim** (`store.claim(id, version, plannedAt, leaseDuration)` → `Optional<TriggerLease>`) is a
  compare-and-set on the row's version that marks the occurrences **in flight** until the lease expires. It
  stores `plannedAt`, the instant the plan was made from. `nextFireAt` does **not** move. Exactly one runner per
  version gets the lease.
- **Confirm** (`store.confirm(lease, nextFireAt)`) runs after the dispatcher returns: it advances `nextFireAt`
  and releases the lease. It fails (and the runner logs a WARN) when the lease was lost — expired and taken over,
  or the trigger re-registered — in which case the occurrences are dispatched again by the new holder.
- **Recovery.** A runner that dies between claim and confirm leaves the lease to expire. `findDue` then returns
  the trigger with `plannedAt` set (`ScheduledTrigger.inFlight()`), and the next runner **replays the plan from
  `plannedAt`**, not from its own present. That gives the same occurrences, with the same `identity()`, and they
  stay on time. Re-planning from the present would let `SKIP` drop an occurrence that was on time when it was
  first claimed. Occurrences that fell due after `plannedAt` are ordinary misfires on the following pass.
- ⚠️ **At-least-once.** An occurrence is never lost; it may be dispatched more than once. History may show it
  twice.
- ⚠️ **Lease time is the store's time.** In JDBC the expiry is written and compared against **the database's
  current time** (`JdbcTriggerSchema.Dialect.currentTimeQuery()`, read just before each claim and due-query). A
  runner whose machine clock is wrong can neither steal a live lease nor hold an expired one. Only schedule time
  (`next_fire_at`, `planned_at`) comes from the runners' `Clock`. `InMemoryTriggerStore` takes its own store
  clock for the same purpose.
- The JDBC statements:
  ```sql
  -- claim
  UPDATE jmouse_scheduler_triggers SET planned_at = ?, lease_expires_at = <db now + lease>, version = version + 1, updated_at = ?
   WHERE trigger_id = ? AND version = ? AND (lease_expires_at IS NULL OR lease_expires_at <= <db now>)
  -- confirm
  UPDATE jmouse_scheduler_triggers SET next_fire_at = ?, planned_at = NULL, lease_expires_at = NULL, version = version + 1, updated_at = ?
   WHERE trigger_id = ? AND version = <the lease's version>
  ```
- **The lease must outlast gate + dispatch** (`lease-duration`, 30 s by default). If it is shorter, healthy
  runners duplicate each other.
- A refused or failed occurrence is **spent**: it is confirmed, and the trigger moves on. Only a runner that
  never returns causes a re-dispatch.
- **Fails closed**: a gate that throws, or a gate/handler key unknown *in the firing process*, refuses or fails.

### ⚠️ Dispatchers MUST be idempotent on `Occurrence.identity()`

`identity()` is `<trigger>@<scheduled epoch ms>`, identical on every re-dispatch. A queue-backed dispatcher puts
a unique constraint on it and treats a duplicate as already done:

```sql
-- in the product's queue table (MySQL)
ALTER TABLE background_jobs ADD COLUMN occurrence_identity VARCHAR(220) NULL,
    ADD CONSTRAINT background_jobs_occurrence_identity UNIQUE (occurrence_identity);
```

```java
@Bean
TriggerDispatcher backgroundJobsDispatcher(JdbcTemplate jdbc, ObjectMapper json) {
    return occurrence -> {
        try {
            jdbc.update("INSERT INTO background_jobs (kind, payload, occurrence_identity, status) VALUES (?, ?, ?, 'WAITING')",
                    occurrence.handlerKey(), json.writeValueAsString(occurrence.parameters().asMap()), occurrence.identity());
        } catch (DuplicateKeyException alreadyEnqueued) {
            // a re-dispatch after a lost lease: the job exists, which is the outcome we wanted
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    };
}
```

(PostgreSQL: `INSERT … ON CONFLICT (occurrence_identity) DO NOTHING` says the same thing in one statement.)
`InProcessDispatcher` is **not** idempotent: behind it, the handler itself must tolerate running twice.

## Registering is idempotent

Products register triggers at every start. Re-registering with an **unchanged schedule keeps the stored next
firing** (otherwise a restart would hide every misfire); an **exhausted `Once` stays exhausted**. A changed
schedule, or `disabled → enabled`, starts from the present — re-enabling is a decision to resume, not to replay
the pause. `register` refuses an unknown handler key, an unknown gate key, and parameters the handler's
`validate` rejects.

## The JDBC store — package `org.jmouse.scheduler.jdbc`

`new JdbcTriggerStore(dataSource, historyLimit[, clock])`. **Plain JDBC, not `jmouse-jdbc`**: that module
brings its own context, expression language and transaction manager; a scheduler inside a Spring application
must use the application's `DataSource` and nothing else, and every statement here is one auto-committed
statement. History is trimmed after each insert to `historyLimit` per trigger, portably (`setMaxRows`, no
`LIMIT`, no self-subquery MySQL would refuse).

Tables `jmouse_scheduler_triggers` and `jmouse_scheduler_occurrences`; **every instant is epoch milliseconds
in a `BIGINT`**, so no driver or session time zone can move it. Scripts on the classpath:

```
org/jmouse/scheduler/jdbc/schema-postgresql.sql   # PostgreSQL; H2 runs it too
org/jmouse/scheduler/jdbc/schema-mysql.sql        # MySQL 8 / MariaDB (indexes inline: no CREATE INDEX IF NOT EXISTS)
```

A product with Flyway **copies the script into a migration of its own** (preferred: its history then says when
the tables appeared). Otherwise `JdbcTriggerSchema.create(dataSource)` runs the right one (`IF NOT EXISTS`
throughout, safe at every start); it detects the dialect from the product name and refuses others.

## Spring Boot — `jmouse-spring/jmouse-scheduler-spring-boot`

```yaml
jmouse:
  scheduler:
    enabled: true                # ⚠️ default false — nothing is wired, no thread starts
    polling: true                # false: Scheduler bean, no loop (a process that only administers)
    poll-interval: 5s
    maximum-jitter: 1s
    misfire-threshold: 1m
    batch-size: 100
    lease-duration: 30s        # a claim in flight longer than this is re-dispatched by another runner
    store:
      type: jdbc                 # jdbc (default) | memory
      history-limit: 200
      initialize-schema: false   # true runs JdbcTriggerSchema.create at start-up
```

⚠️ These keys are a published contract. Bound with `Binder` into the plain record `SchedulerProperties`.

| Bean | Default | Replace it when |
|---|---|---|
| `SchedulerProperties` | bound from `jmouse.scheduler` | never, normally |
| `HandlerRegistry` | every `TriggerHandler` bean | never |
| `GateRegistry` | every `TriggerGate` bean | never |
| `TriggerStore` | `JdbcTriggerStore` over the single `DataSource`; `InMemoryTriggerStore` only for `store.type=memory` (logs a WARN) | you keep triggers elsewhere |
| `TriggerDispatcher` | `InProcessDispatcher` | ⚠️ you have a durable queue — Raccoon does |
| `Scheduler` | — | never |
| `SchedulerRunner` | `initMethod=start`, closed with the context; absent when `polling=false` | never |

All `@ConditionalOnMissingBean`. The `Clock` is the application's when it has exactly one (that is how a test
moves time), otherwise `Clock.systemUTC()`; no `Clock` bean is published. Ordered after both Boot 3's and
Boot 4's `DataSourceAutoConfiguration` by name. ⚠️ `store.type=jdbc` with no `DataSource` **fails the
start-up naming the property** — it never falls back to memory silently. Compiled against Boot 4.0.7.

⚠️ **When it does not run, the symptom is silence.** The runner logs one line at start:

```
jmouse-scheduler started: store=JdbcTriggerStore, handlers=[scan-source, vectorise-waiting], gates=[budget-preflight], poll=PT5S + up to PT1S jitter, misfire threshold=PT1M
```

If it is absent with `enabled=true`, the auto-configuration did not register.

## How Raccoon plugs in (not done — follow-up after review)

1. **Dependency** `jmouse-scheduler-spring-boot`; `jmouse.scheduler.enabled=true`. Both JVMs register the same
   handlers and gates; the claim makes it safe for both to poll, or set `polling=false` on the one that should
   not. Copy `schema-mysql.sql` into a Raccoon migration (Raccoon's migrations are its own; do not set
   `initialize-schema`).
2. **A `TriggerDispatcher` bean over `background_jobs`** — this replaces `InProcessDispatcher`. It inserts a job
   of kind `occurrence.handlerKey()` with the parameters as its payload, keyed by `occurrence.identity()` under a
   unique constraint, and treats a duplicate as done — the pattern under *Dispatchers MUST be idempotent* above.
   The worker's existing `@Scheduled` polling picks the job up and calls
   `handlerRegistry.require(kind).handle(occurrence)` — so the work runs with Raccoon's retries, not on the
   scheduler's thread.
3. **Handlers** (`TriggerHandler` beans): `scan-source {sourceId}`; `vectorise-waiting {profile,
   maximumDocuments, budget}` — "the next N waiting documents" as the unit of a portion, with `validate`
   refusing a missing profile or a non-positive `maximumDocuments` at registration.
4. **The budget gate** (`TriggerGate`, key `budget-preflight`): runs the existing vectorisation preflight
   (tokens, cost, budget verdict) for the occurrence's parameters and returns `refuse(<the verdict's words>)`
   when it would overrun. A refusal is a `REFUSED` line in history, visible to whoever wonders why nothing
   was vectorised last night. Vectorise triggers keep the default `SKIP` misfire policy — the note's
   observation that a catch-up would multiply unattended spend.
5. **Screens** read `Scheduler.triggers()` and `history(id, n)`; editing a schedule is `register(…)`.

## Build

```bash
cd Git/jmouse
mvn -o install -pl jmouse-scheduler,jmouse-spring/jmouse-scheduler-spring-boot -am -Dgpg.skip=true
mvn -o install -pl jmouse-dependencies -Dgpg.skip=true   # ⚠️ -am does not rebuild an imported BOM
```

Tests: `jmouse-scheduler` 82 and `jmouse-scheduler-spring-boot` 8 (context runner), none touching the network.
- Cron: grammar, month ends, leap years, DST gap and overlap; schedules.
- Every misfire policy and the scan ceiling; gates; failures; bounded history; idempotent registration; the loop.
- A claim race: 8 threads × 40 triggers × 25 rounds on the in-memory store, and 4 store instances × 20 × 10 on
  H2. Each round, every occurrence is dispatched and confirmed exactly once.
- Lease recovery: a runner dies after dispatch and before confirm. On the store's clock (runner clocks racing
  ahead do not expire the lease), the survivor re-dispatches with the same identity, still on time even when
  ten minutes late. On H2, the lease expires by the database's clock while the runners' clock is frozen.

## Do not

- **Do not change the default misfire policy from `SKIP`.**
- **Do not dispatch before the claim, or confirm before the dispatcher returns** — that ordering is the at-least-once guarantee.
- **Do not measure a lease on a runner's clock** — the store's (the database's) time decides expiry.
- **Do not re-plan a recovered lease from the present** — replay from `plannedAt`, or `SKIP` can lose an on-time occurrence.
- **Do not let a gate fail open** — an exception or an unknown key must refuse.
- **Do not fall back to the in-memory store** when the `DataSource` is missing.
- **Do not store instants as `TIMESTAMP`** — epoch milliseconds is what keeps two drivers in agreement.
- **Do not add cron syntax silently**: anything new is parsed and tested, or refused.
