# jmouse-search

Explainable text relevance and optional federation across searchable kinds. A result retains per-field match reasons and weights, so callers can explain its place in the answer. This module does not query a database, index documents or decide which rows the caller may read.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-search/pom.xml). Direct dependency declarations include `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [SearchTerms](../jmouse-search/src/main/java/org/jmouse/search/SearchTerms.java) | 🔤 What somebody typed, as the words a search actually asks about. |
| [Scorer](../jmouse-search/src/main/java/org/jmouse/search/Scorer.java) | 🎚️ How well one text answers the query — the seam this whole library exists to provide . |
| [LadderScorer](../jmouse-search/src/main/java/org/jmouse/search/LadderScorer.java) | 🪜 The default Scorer: a ladder of rungs, each answering a different question a person asks. |
| [MatchKind](../jmouse-search/src/main/java/org/jmouse/search/MatchKind.java) | 🎯 How a field answered the query — the half of a match a number cannot carry. |
| [FieldMatch](../jmouse-search/src/main/java/org/jmouse/search/FieldMatch.java) | 📐 One field's answer to the query — named, weighed, and able to say how it matched. |
| [Relevance](../jmouse-search/src/main/java/org/jmouse/search/Relevance.java) | 🧮 How well one thing answers a search — field by field, and able to say so. |
| [Weights](../jmouse-search/src/main/java/org/jmouse/search/Weights.java) | ⚖️ How much each kind of field is worth when something is scored. |
| [SearchHit](../jmouse-search/src/main/java/org/jmouse/search/SearchHit.java) | 🔎 One thing a search found, what kind of thing it is, and why it is in the answer. |
| [SearchType](../jmouse-search/src/main/java/org/jmouse/search/SearchType.java) | 🏷️ A searchable kind, as an interface's type filter describes it. |
| [SearchProvider](../jmouse-search/src/main/java/org/jmouse/search/SearchProvider.java) | 🗂️ One kind of thing that can be found — the federation half of the library. |
| [SearchDirectory](../jmouse-search/src/main/java/org/jmouse/search/SearchDirectory.java) | 🧭 Every provider, asked at once and merged into one ranked answer. |

## Main flow

- Normalize user text by trimming, lowercasing with Locale.ROOT and collapsing whitespace. Distinct terms are retained up to the declared eight-term bound, while phrase preserves the normalized query. ([SearchTerms](../jmouse-search/src/main/java/org/jmouse/search/SearchTerms.java))

- Score a single textual representation into a FieldScore that carries match kind, strength and occurrence count. LadderScorer supplies the default implementation. ([Scorer](../jmouse-search/src/main/java/org/jmouse/search/Scorer.java))

- Weigh named fields, with alternate representations as needed, and add explicit boosts. The result retains the field match and bonus breakdown. ([Relevance](../jmouse-search/src/main/java/org/jmouse/search/Relevance.java))

- Wrap a found entity identity, kind, presentation data and Relevance. score and explain delegate to that retained relevance calculation. ([SearchHit](../jmouse-search/src/main/java/org/jmouse/search/SearchHit.java))

- Expose one searchable kind and return its permitted candidates for the supplied query/context/limit. Context is product-defined. ([SearchProvider](../jmouse-search/src/main/java/org/jmouse/search/SearchProvider.java))

- Filter providers by requested kinds, ask each selected provider for the full limit, merge in memory, sort with the supplied comparator and apply the final limit. ([SearchDirectory](../jmouse-search/src/main/java/org/jmouse/search/SearchDirectory.java))

## Adoption and extension points

- Replace the default formula without replacing the relevance explanation model. A product-specific ranking rule belongs behind this seam. ([Scorer](../jmouse-search/src/main/java/org/jmouse/search/Scorer.java))

- Use the shared scale when ranking across providers. Critical, primary, secondary, supporting and contextual fields carry weights 8, 4, 2, 1 and 0.5. ([Weights](../jmouse-search/src/main/java/org/jmouse/search/Weights.java))

- Only implement federation when a product searches several kinds. Single-kind search can use SearchTerms and Relevance directly. ([SearchProvider](../jmouse-search/src/main/java/org/jmouse/search/SearchProvider.java))

- Pass a context carrying whatever authorization/tenant information providers need and provide a comparator when the product needs a stable secondary ordering. ([SearchDirectory](../jmouse-search/src/main/java/org/jmouse/search/SearchDirectory.java))

## Constraints worth preserving

- The federation merges in memory and invokes the stream providers sequentially in this implementation. It is not a distributed index or a global database pagination engine. ([SearchDirectory](../jmouse-search/src/main/java/org/jmouse/search/SearchDirectory.java))

- Dividing the limit between providers would prematurely discard good answers; each provider receives the full limit. ([SearchDirectory](../jmouse-search/src/main/java/org/jmouse/search/SearchDirectory.java))

- matchedBy requires every term somewhere across the supplied representations; touchedBy asks only whether any term appears. Use the intended question at each candidate/ranking stage. ([SearchTerms](../jmouse-search/src/main/java/org/jmouse/search/SearchTerms.java))

- Field contributions are summed. Taking only the best field would make a document matching several important fields look like one matching a single field. ([Relevance](../jmouse-search/src/main/java/org/jmouse/search/Relevance.java))

- A relevance score never grants visibility. Provider candidate retrieval must enforce the product access rules. ([SearchProvider](../jmouse-search/src/main/java/org/jmouse/search/SearchProvider.java))


## Source navigation

- [`org.jmouse.search`](../jmouse-search/src/main/java/org/jmouse/search/)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-search/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.search`

| Type | Responsibility / contract |
|---|---|
| [FieldMatch](../jmouse-search/src/main/java/org/jmouse/search/FieldMatch.java) | 📐 One field's answer to the query — named, weighed, and able to say how it matched. |
| [LadderScorer](../jmouse-search/src/main/java/org/jmouse/search/LadderScorer.java) | 🪜 The default Scorer: a ladder of rungs, each answering a different question a person asks. |
| [MatchKind](../jmouse-search/src/main/java/org/jmouse/search/MatchKind.java) | 🎯 How a field answered the query — the half of a match a number cannot carry. |
| [Relevance](../jmouse-search/src/main/java/org/jmouse/search/Relevance.java) | 🧮 How well one thing answers a search — field by field, and able to say so. |
| [Scorer](../jmouse-search/src/main/java/org/jmouse/search/Scorer.java) | 🎚️ How well one text answers the query — the seam this whole library exists to provide . |
| [SearchDirectory](../jmouse-search/src/main/java/org/jmouse/search/SearchDirectory.java) | 🧭 Every provider, asked at once and merged into one ranked answer. |
| [SearchHit](../jmouse-search/src/main/java/org/jmouse/search/SearchHit.java) | 🔎 One thing a search found, what kind of thing it is, and why it is in the answer. |
| [SearchProvider](../jmouse-search/src/main/java/org/jmouse/search/SearchProvider.java) | 🗂️ One kind of thing that can be found — the federation half of the library. |
| [SearchTerms](../jmouse-search/src/main/java/org/jmouse/search/SearchTerms.java) | 🔤 What somebody typed, as the words a search actually asks about. |
| [SearchType](../jmouse-search/src/main/java/org/jmouse/search/SearchType.java) | 🏷️ A searchable kind, as an interface's type filter describes it. |
| [Weights](../jmouse-search/src/main/java/org/jmouse/search/Weights.java) | ⚖️ How much each kind of field is worth when something is scored. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
