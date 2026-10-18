# jmouse-search-text

Two reusable pieces for document search: rank a bounded candidate collection using explainable Relevance, and extract passages around the terms that matched. The product still retrieves authorized candidates, decides field weights and builds its search response.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-search-text/pom.xml). Direct dependency declarations include `jmouse-search`, `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [DocumentRanking](../jmouse-search-text/src/main/java/org/jmouse/search/text/DocumentRanking.java) | 📊 Candidates in, ranked answers out — the six lines every document search writes identically. |
| [Snippets](../jmouse-search-text/src/main/java/org/jmouse/search/text/Snippets.java) | ✂️ The passage a search matched, taken out of the document around it. |

## Main flow

- Call best(candidates, weigh, tieBreak, limit) with a function that computes the product Relevance for each candidate. ([DocumentRanking](../jmouse-search-text/src/main/java/org/jmouse/search/text/DocumentRanking.java))

- Compute a Ranked wrapper once per candidate and discard entries whose relevance does not match the full query. ([DocumentRanking](../jmouse-search-text/src/main/java/org/jmouse/search/text/DocumentRanking.java))

- Sort descending by relevance score, apply the caller tie-break comparator and return at most the requested limit. ([DocumentRanking](../jmouse-search-text/src/main/java/org/jmouse/search/text/DocumentRanking.java))

- Call from(markdown, terms) for the default lightweight markdown cleaner, or supply a custom UnaryOperator cleaner for another text format. ([Snippets](../jmouse-search-text/src/main/java/org/jmouse/search/text/Snippets.java))

- Inspect the first occurrence of each term, retaining at most three sufficiently separated passages and sorting them by source position. ([Snippets](../jmouse-search-text/src/main/java/org/jmouse/search/text/Snippets.java))

- Cut word-bounded windows, clean their text, collapse whitespace and add ellipses where text was omitted. ([Snippets](../jmouse-search-text/src/main/java/org/jmouse/search/text/Snippets.java))

## Adoption and extension points

- Keep authorization and database candidate bounds before this in-memory step. The helper receives an already materialized candidate collection. ([DocumentRanking](../jmouse-search-text/src/main/java/org/jmouse/search/text/DocumentRanking.java))

- Use a stable tieBreak appropriate to the domain, such as a caller-selected date or identifier comparator. ([DocumentRanking](../jmouse-search-text/src/main/java/org/jmouse/search/text/DocumentRanking.java))

- Supply Snippets.asItIs when the candidate body is ordinary text or provide the product cleaner through the overload. ([Snippets](../jmouse-search-text/src/main/java/org/jmouse/search/text/Snippets.java))

- Build excerpts from the matched body rather than always returning its opening summary. The helper is presentation support, not another scorer. ([Snippets](../jmouse-search-text/src/main/java/org/jmouse/search/text/Snippets.java))

## Constraints worth preserving

- A nonpositive limit or empty candidates returns an empty list; it does not mean unlimited results. ([DocumentRanking](../jmouse-search-text/src/main/java/org/jmouse/search/text/DocumentRanking.java))

- The cleaner removes image/link/format syntax by regular expression. It is not a full markdown parser or an HTML sanitizer. ([Snippets](../jmouse-search-text/src/main/java/org/jmouse/search/text/Snippets.java))

- The current window constants are eighty characters before and one hundred forty after a match, with at most three passages. They are implementation choices rather than caller configuration. ([Snippets](../jmouse-search-text/src/main/java/org/jmouse/search/text/Snippets.java))

- The helper chooses term-by-term first occurrences, not the globally highest-scoring passage. Multiple nearby term matches collapse into one passage. ([Snippets](../jmouse-search-text/src/main/java/org/jmouse/search/text/Snippets.java))

## API shape

With candidates, a domain weighting function and a stable comparator supplied by the product:

```java
var ranked = DocumentRanking.best(candidates, weigh, tieBreak, 20);
var passages = Snippets.from(markdownBody, terms);
var plainPassages = Snippets.from(plainBody, terms, Snippets::asItIs);
```

Each ranked entry retains both subject and Relevance, so response construction can use the explanation already computed. Snippets uses the same normalized terms but does not mutate the ranking or the document.

## Source navigation

- [`org.jmouse.search.text`](../jmouse-search-text/src/main/java/org/jmouse/search/text/)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-search-text/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-search` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.search.text`

| Type | Responsibility / contract |
|---|---|
| [DocumentRanking](../jmouse-search-text/src/main/java/org/jmouse/search/text/DocumentRanking.java) | 📊 Candidates in, ranked answers out — the six lines every document search writes identically. |
| [Snippets](../jmouse-search-text/src/main/java/org/jmouse/search/text/Snippets.java) | ✂️ The passage a search matched, taken out of the document around it. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
