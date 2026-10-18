# jmouse-money-jpa

The exchange-rate table, registry and ExchangeRates adapter. Rate rows are qualified by the pivot and source so a product can distinguish manually decided values from synchronized ones. Fetching a bank feed and scheduling synchronization belong to the Spring integration, not this persistence module.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-money-jpa/pom.xml). Direct dependency declarations include `jmouse-money`, `jakarta.persistence-api`, `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [ExchangeRate](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRate.java) | 📈 One currency's rate against the pivot, as a row. ⚠️ The pivot is a COLUMN, not a constant A rate is meaningless without saying what it is a rate to . Rows quoted against the hryvnia and rows quoted against the euro are different numbers that look identical, so a table which did not record the pivot would be silently wrong the first time an installation pointed at a different feed — and would look entirely correct while… |
| [ExchangeRateRegistry](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRateRegistry.java) | 🗄️ Rows in and rows out. Nothing else. ⚠️ No transaction demarcation, and that is the library's side of a bargain. A library that opened its own transactions would either fight the product's or quietly commit half of something the product was still deciding about. Every method here assumes it is inside one the caller opened — which is the same contract ManagedFiles and FileBindings keep. ⚠️ It knows nothing about what a… |
| [ExchangeRateSource](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRateSource.java) | 🏷️ Who decided a rate, and therefore whether a sync may replace it. |
| [JpaExchangeRates](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/JpaExchangeRates.java) | 📈 The rate seam, answered from rows. ⚠️ The pivot is constructor state, and is not read from the table. Asking the rows what they are quoted against would let a stale row decide what every conversion means: swap the feed, forget to clear the old rows, and the converter silently starts routing through whatever the table happened to hold. The pivot is a decision, so it is supplied by whoever made it — and… |
| [MoneyMigrations](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/migration/MoneyMigrations.java) | 🚚 Where this library's schema lives, and under what history. Its own history table, separate from the product's and separate from every other library's — the same reasoning that gave access, storage, files and ai theirs. Products in this workspace number their migrations in ranges that do not fit together, and a shared history would force either a reserved range held forever by convention or somebody renumbering a schema… |
| [MoneyDialect](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/migration/MoneyDialect.java) | 🗣️ The SQL dialects this library ships migrations for, and how one is chosen. Chosen from the data source rather than from an application profile: a product may have no profile naming its database at all, or may name it differently from every other product. The connection always knows. |

## Main flow

- Represent one currency rate against a recorded pivot together with its source and update metadata. The pivot column prevents rows from different quotation systems being confused. ([ExchangeRate](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRate.java))

- Read and maintain rate rows through the neutral registry operations. This layer deliberately does not decide the business rules for replacing a manual rate during a sync. ([ExchangeRateRegistry](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRateRegistry.java))

- Construct the ExchangeRates adapter with a configured pivot and the registry, then hand it to MoneyConverter. ([JpaExchangeRates](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/JpaExchangeRates.java))

- Look up a rate against that constructor pivot rather than asking whichever table row happens to exist to define the conversion basis. ([JpaExchangeRates](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/JpaExchangeRates.java))

- Resolve the migration dialect from the data source metadata. ([MoneyDialect](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/migration/MoneyDialect.java))

- Expose the private money schema history and dialect migration locations to application initialization. ([MoneyMigrations](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/migration/MoneyMigrations.java))

## Adoption and extension points

- Use the same EntityManager/unit of work as the surrounding application operation. The registry does not open its own transactions. ([ExchangeRateRegistry](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRateRegistry.java))

- Keep the configured pivot consistent with the feed supplying rows and with the rate adapter the converter sees. ([JpaExchangeRates](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/JpaExchangeRates.java))

- Retain MANUAL versus provider origin when writing rates so a synchronization service can make its documented replacement decision. ([ExchangeRateSource](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRateSource.java))

- Include the entity package in the persistence unit and run the library migrations before querying the table. ([MoneyMigrations](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/migration/MoneyMigrations.java))

## Constraints worth preserving

- A stale row must not redefine the pivot after a feed configuration change. The adapter uses constructor state precisely to prevent that. ([JpaExchangeRates](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/JpaExchangeRates.java))

- No transaction demarcation is provided here; multiple product writes need the caller chosen transaction if they must commit together. ([ExchangeRateRegistry](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRateRegistry.java))

- A rate without its pivot is incomplete data. Keep both currency axes when copying/importing rows. ([ExchangeRate](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRate.java))

- Validation and sync policy belong above a rows-in/rows-out registry. Calling it directly is not equivalent to invoking the higher-level rate service. ([ExchangeRateRegistry](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRateRegistry.java))

## Adapter composition

The persistence adapter can be wired without a bank client or scheduler:

```java
ExchangeRateRegistry registry = new ExchangeRateRegistry(entityManager);
ExchangeRates rates = new JpaExchangeRates(
        registry, CurrencyCode.required("UAH"));
MoneyConverter converter = new MoneyConverter(rates);
```

The registry key is the currency identifier. Its find(currency, pivot) overload additionally filters the found row by pivot; it is not a composite-key table containing arbitrary simultaneous pivot rows for the same currency. See [ExchangeRateRegistry](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRateRegistry.java).

## Source navigation

- [`org.jmouse.money.jpa`](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/)
- [`org.jmouse.money.jpa.migration`](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/migration/)

Resource entry points:

- [V000001__create_exchange_rates.sql](../jmouse-money-jpa/src/main/resources/db/money/mysql/V000001__create_exchange_rates.sql)
- [V000001__create_exchange_rates.sql](../jmouse-money-jpa/src/main/resources/db/money/postgresql/V000001__create_exchange_rates.sql)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-money-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-money` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.money.jpa`

| Type | Responsibility / contract |
|---|---|
| [ExchangeRate](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRate.java) | 📈 One currency's rate against the pivot, as a row. |
| [ExchangeRateRegistry](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRateRegistry.java) | 🗄️ Rows in and rows out. Nothing else. |
| [ExchangeRateSource](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/ExchangeRateSource.java) | 🏷️ Who decided a rate, and therefore whether a sync may replace it. |
| [JpaExchangeRates](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/JpaExchangeRates.java) | 📈 The rate seam, answered from rows. |

#### `org.jmouse.money.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [MoneyDialect](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/migration/MoneyDialect.java) | 🗣️ The SQL dialects this library ships migrations for, and how one is chosen. |
| [MoneyMigrations](../jmouse-money-jpa/src/main/java/org/jmouse/money/jpa/migration/MoneyMigrations.java) | 🚚 Where this library's schema lives, and under what history. |

### Packaged resources

- [db/money/mysql/V000001__create_exchange_rates.sql](../jmouse-money-jpa/src/main/resources/db/money/mysql/V000001__create_exchange_rates.sql).
- [db/money/postgresql/V000001__create_exchange_rates.sql](../jmouse-money-jpa/src/main/resources/db/money/postgresql/V000001__create_exchange_rates.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
