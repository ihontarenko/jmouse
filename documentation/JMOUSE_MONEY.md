# jmouse-money

Currency-aware decimal amounts and conversion through a rate-table pivot, without persistence, scheduling or bank HTTP clients. It provides one place to parse supported currency input, reject mixed-currency arithmetic and apply a single final conversion rounding.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-money/pom.xml). Direct dependency declarations include `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [CurrencyCode](../jmouse-money/src/main/java/org/jmouse/money/CurrencyCode.java) | 💱 An ISO 4217 currency, recognised from whatever a person actually typed. |
| [Money](../jmouse-money/src/main/java/org/jmouse/money/Money.java) | 💰 An amount of one currency. ⚠️ Adding two currencies THROWS, and that is not a runtime condition Hryvnia plus dollars is not a number that is slightly wrong; it is not a number. Code that reaches #plus(Money) with mismatched currencies has a bug in it, and returning an empty result would let that bug travel as far as a report. So it throws CurrencyMismatchException, in the tradition of BigDecimal throwing on a division… |
| [CurrencyMismatchException](../jmouse-money/src/main/java/org/jmouse/money/exception/CurrencyMismatchException.java) | 🚫 Two amounts of different currencies were treated as one. |
| [ExchangeRates](../jmouse-money/src/main/java/org/jmouse/money/ExchangeRates.java) | 📈 Where rates come from, as far as this library is concerned. |
| [MoneyConverter](../jmouse-money/src/main/java/org/jmouse/money/MoneyConverter.java) | 🔄 One currency into another, through the pivot the rates are quoted against. |

## Main flow

- of accepts a known three-letter JDK currency code, an unambiguous supported symbol or a supported local spelling. required turns absence into an IllegalArgumentException. ([CurrencyCode](../jmouse-money/src/main/java/org/jmouse/money/CurrencyCode.java))

- For ambiguous symbols such as dollar, candidatesFor returns the possible currencies instead of guessing which one the user meant. ([CurrencyCode](../jmouse-money/src/main/java/org/jmouse/money/CurrencyCode.java))

- Construct a BigDecimal amount with CurrencyCode. plus requires the same currency; times scales an amount without changing its currency. ([Money](../jmouse-money/src/main/java/org/jmouse/money/Money.java))

- Provide the pivot currency and an optional rateToPivot for other currencies. The pivot is a property of the rate source rather than the product display currency. ([ExchangeRates](../jmouse-money/src/main/java/org/jmouse/money/ExchangeRates.java))

- For a changed currency, read source and target rates, substitute one for the pivot, multiply the source amount by the source rate and divide once by the target rate. ([MoneyConverter](../jmouse-money/src/main/java/org/jmouse/money/MoneyConverter.java))

- Return Optional.empty when either rate is missing or nonpositive. The convenience conversion uses scale four and HALF_UP; the overload accepts explicit rounding policy. ([MoneyConverter](../jmouse-money/src/main/java/org/jmouse/money/MoneyConverter.java))

## Adoption and extension points

- Implement the read port over configuration, an in-memory source or jmouse-money-jpa. The converter does not fetch or update rates. ([ExchangeRates](../jmouse-money/src/main/java/org/jmouse/money/ExchangeRates.java))

- Choose a display/business rounding scale at the application boundary when the default four decimal places are not suitable. ([MoneyConverter](../jmouse-money/src/main/java/org/jmouse/money/MoneyConverter.java))

- Use candidatesFor to drive an explicit user choice for an ambiguous unit label; code parsing itself does not choose a country. ([CurrencyCode](../jmouse-money/src/main/java/org/jmouse/money/CurrencyCode.java))

- Use zero(currency) for an accumulation identity; the currency is still required even when the amount is zero. ([Money](../jmouse-money/src/main/java/org/jmouse/money/Money.java))

## Constraints worth preserving

- Adding different currencies is a programming error; convert explicitly before aggregation. The exception retains both currencies. ([CurrencyMismatchException](../jmouse-money/src/main/java/org/jmouse/money/exception/CurrencyMismatchException.java))

- Converting to the same currency returns the original Money without applying a new scale. Use withScale when formatting or explicitly rescaling the same currency. ([MoneyConverter](../jmouse-money/src/main/java/org/jmouse/money/MoneyConverter.java))

- Dividing on both legs would round twice. The implementation multiplies first and divides once. ([MoneyConverter](../jmouse-money/src/main/java/org/jmouse/money/MoneyConverter.java))

- An unrecognized label is not an ISO code merely because it has three letters. Recognition uses the JDK currency catalogue. ([CurrencyCode](../jmouse-money/src/main/java/org/jmouse/money/CurrencyCode.java))

- Rates have no meaning without the pivot; do not treat a product base/display currency as an implicit replacement. ([ExchangeRates](../jmouse-money/src/main/java/org/jmouse/money/ExchangeRates.java))

## API shape

The amount constructor does not parse a currency-free number into money:

```java
CurrencyCode hryvnia = CurrencyCode.required("UAH");
Money amount = Money.of(new BigDecimal("125.50"), hryvnia);
Money doubled = amount.times(new BigDecimal("2"));
Optional<Money> dollars = converter.convert(
        doubled, CurrencyCode.required("USD"), 2, RoundingMode.HALF_UP);
```

Here converter is a MoneyConverter already wired to an ExchangeRates implementation. The optional conversion is handled before using or displaying the result.

## Source navigation

- [`org.jmouse.money`](../jmouse-money/src/main/java/org/jmouse/money/)
- [`org.jmouse.money.exception`](../jmouse-money/src/main/java/org/jmouse/money/exception/)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-money/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.money`

| Type | Responsibility / contract |
|---|---|
| [CurrencyCode](../jmouse-money/src/main/java/org/jmouse/money/CurrencyCode.java) | 💱 An ISO 4217 currency, recognised from whatever a person actually typed. |
| [ExchangeRates](../jmouse-money/src/main/java/org/jmouse/money/ExchangeRates.java) | 📈 Where rates come from, as far as this library is concerned. |
| [Money](../jmouse-money/src/main/java/org/jmouse/money/Money.java) | 💰 An amount of one currency. |
| [MoneyConverter](../jmouse-money/src/main/java/org/jmouse/money/MoneyConverter.java) | 🔄 One currency into another, through the pivot the rates are quoted against. |

#### `org.jmouse.money.exception`

| Type | Responsibility / contract |
|---|---|
| [CurrencyMismatchException](../jmouse-money/src/main/java/org/jmouse/money/exception/CurrencyMismatchException.java) | 🚫 Two amounts of different currencies were treated as one. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
