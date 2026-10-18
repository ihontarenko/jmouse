# jmouse-search-jpa

A small Jakarta Criteria candidate predicate adapter for the core search vocabulary. SearchPredicates finds rows with any term in any declared textual attribute; exact all-term acceptance and ranking remain in the relevance layer.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-search-jpa/pom.xml). Direct dependency declarations include `jmouse-search`, `slf4j-api`, `jakarta.persistence-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java) | 🗄️ Turning words into the loose query that finds the candidates. |

## Main flow

- Pass a CriteriaBuilder, entity root, SearchTerms and either a collection or varargs of textual attribute paths to anyTermIn. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- Resolve a dotted attribute path by walking Path.get for each component. The library does not need a product entity metamodel class. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- For each term and attribute, form lower(column) LIKE percent-term-percent. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- Combine attributes with OR within each term, then combine the term predicates with OR. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- Compose the resulting candidate predicate with the product tenant/visibility predicates before executing the Criteria query. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- Hand the bounded candidates to the product Relevance weighting or document ranking step to obtain final ordered matches. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

## Adoption and extension points

- Use it where Criteria is already the repository query mechanism. This module declares no repository interface or persistence entity. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- Supply paths that resolve to text columns. The path helper casts to a text Path after traversal; it does not validate a schema for arbitrary names. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- Select the candidate bound and ordering in product code. The helper returns a Predicate only and never runs the query. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- Keep the same SearchTerms instance for candidate discovery and subsequent relevance evaluation. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

## Constraints worth preserving

- Empty terms or no attributes returns builder.disjunction, a false predicate, rather than selecting every row. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- The predicate deliberately asks for any term, not all terms. Treating its result set as the final answer can expose partial matches as complete ones. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- LIKE metacharacters percent and underscore are not escaped by this implementation. User text containing them broadens the candidate match pattern. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- Dotted path traversal is Path.get, not an explicit join builder. Check the entity mapping when a query would need collection joins. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

- This is a text candidate filter rather than full-text indexing. Case behavior and execution cost still depend on the database expression and schema. ([SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java))

## Criteria composition

Within an existing product Criteria query, the candidate predicate can be one part of the restriction:

```java
Predicate words = SearchPredicates.anyTermIn(
        builder, root, terms, "title", "description");
query.where(builder.and(visibleRows, words));
```

The example assumes visibleRows is the product authorization predicate. SearchPredicates supplies neither it nor the candidate query execution. Use actual textual attribute names from the mapped entity instead of these example names.

## Source navigation

- [`org.jmouse.search.jpa`](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-search-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-search` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.search.jpa`

| Type | Responsibility / contract |
|---|---|
| [SearchPredicates](../jmouse-search-jpa/src/main/java/org/jmouse/search/jpa/SearchPredicates.java) | 🗄️ Turning words into the loose query that finds the candidates. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
