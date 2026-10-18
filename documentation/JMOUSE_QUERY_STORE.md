# jmouse-query-store

The framework-neutral saved-query library and persistence ports. It retains named jMQ source text, ownership, visibility and listing criteria, while QueryLibrary validates a draft before asking a store to keep it. Authored source declarations have a separate text-storage port because they define what can be queried.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-query-store/pom.xml). Direct dependency declarations include `jmouse-query`, `slf4j-api`, `logback-classic`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [QueryLibrary](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryLibrary.java) | 📖 The saved queries of an installation — read, written, and never accepted broken. |
| [SavedQueries](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQueries.java) | 🗄️ Where saved queries are kept. ⚠️ Five methods, and no sixth Everything a product asks of saved queries is one of these; anything more specific is a SavedQueryCriteria. That is deliberate — this interface is implemented once per backend, and every method added to it is a method written again in each of them. ⚠️ It checks nothing Not the jMQ, not the source, not the lengths. A store that validated would validate slightly… |
| [SavedQuery](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQuery.java) | 💾 A query somebody kept: a name, where it belongs, and the jMQ behind it. An interface rather than a record, because the row is the caller's business A store backed by persistence hands back something managed, whose changes are written when the caller's transaction ends. A store backed by a map hands back a value. Both are "a saved query" to everything that reads one, and neither should have to be copied into the other's… |
| [SavedQueryDraft](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQueryDraft.java) | ✍️ What a caller supplies when saving — everything a query is, minus everything the store decides. No identifier, no timestamps: those belong to the act of saving rather than to the query, and a caller made to invent them would be inventing them differently in every product. |
| [SavedQueryCriteria](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQueryCriteria.java) | 🔍 What to list. ⚠️ One shape for every listing, rather than a method per question The interface would otherwise grow the same family in every implementation — listFor(owner), listFor(owner, source), listVisibleTo(owner, reader) — and each new question would be a method added to the contract and then written again everywhere behind it. Here a store answers one question, and a caller narrows it. ⚠️ reader is not… |
| [QueryOwner](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryOwner.java) | 🏷️ Where a saved query belongs — a board, a workspace, a person, or the installation itself. |
| [QueryForm](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryForm.java) | 📐 The two shapes a saved query comes in. Both are jMQ, read by one parser. The difference is how much of a query the text carries, and it decides only one thing: which entry point runs it. FILTER issue.status == 'open' and issue.assignee == currentUser() DOCUMENT view "Overdue" on issues { where issue.dueDate < now() order issue.priority desc columns key, summary, assignee } ⚠️ Derived, never stored A column recording the… |
| [SchemaCatalog](../jmouse-query-store/src/main/java/org/jmouse/query/store/SchemaCatalog.java) | 📚 What each described source lets a query say. ⚠️ The one seam that makes a single store serve every product A saved row carries the name of a source and nothing else — issues, inventory. What that name reaches is the product's business: its tables, its columns, its bags, its joins. This is how the store asks the product what a name means, without acquiring an opinion about any of it. A product that already builds an… |
| [MemorySavedQueries](../jmouse-query-store/src/main/java/org/jmouse/query/store/memory/MemorySavedQueries.java) | 🗃️ Saved queries kept in a map. ⚠️ Not a stub — the second implementation that keeps the first honest The same reason the query engine has an in-memory backend beside its SQL one: a contract with one implementation is not a contract, it is that implementation's behaviour written down twice. Two of them, answering the same listing identically, is what makes SavedQueryCriteria a rule rather than a description of whatever… |
| [AuthoredSource](../jmouse-query-store/src/main/java/org/jmouse/query/store/AuthoredSource.java) | A declaration somebody wrote and kept — the structure and mapping for one source. |
| [AuthoredSources](../jmouse-query-store/src/main/java/org/jmouse/query/store/AuthoredSources.java) | Where authored declarations are kept. ⚠️ It stores TEXT and validates nothing This port has no idea what jMQ is, cannot parse it, and must not learn to. Whether a body is well formed, whether its mapping names a table this installation is willing to publish, and whether the caller may write it at all are three different questions answered in three different places — by the language, by the allow-list, and by the subject. A… |
| [SourceOrigin](../jmouse-query-store/src/main/java/org/jmouse/query/store/SourceOrigin.java) | Where a source's declaration comes from — and therefore whether anybody may edit it. |
| [QueryStoreException](../jmouse-query-store/src/main/java/org/jmouse/query/store/exception/QueryStoreException.java) | 🚫 Something went wrong keeping a query, as opposed to reading or running one. Kept apart from a parse failure and from a schema refusal on purpose: those two are things a person can fix in the text box in front of them, and this one is not. |
| [SavedQueryNotFoundException](../jmouse-query-store/src/main/java/org/jmouse/query/store/exception/SavedQueryNotFoundException.java) | 🚫 A saved query nobody kept, or nobody kept any more. Its own type rather than an empty result, because the two callers want opposite things: a screen listing views wants "there are none", and a screen opening one by its identifier wants to say which one is gone. A method that returned empty for both would make every caller invent this sentence. |

## Main flow

- Build a draft with its name, source, owner and body plus the optional description, author and public visibility. Identifiers and stored timestamps belong to saving, not draft input. ([SavedQueryDraft](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQueryDraft.java))

- check verifies required fields, declared length bounds and the source schema. Save and update both pass through this validation. ([QueryLibrary](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryLibrary.java))

- For a document, parse it, require its explicit view target to equal the saved row source and run the schema checker. For an expression, use checkCondition because it is a row filter. ([QueryLibrary](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryLibrary.java))

- Persist through the store port. saveAs allows a product-minted identifier, useful when another row must refer to the query in the same caller-controlled unit of work. ([SavedQueries](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQueries.java))

- List through one criteria model shared by memory and persistence adapters, rather than adding a new repository method for every owner/source/visibility combination. ([SavedQueryCriteria](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQueryCriteria.java))

- Distinguish a condition from a whole document using the language. canonical renders documents when explicitly requested; it does not automatically rewrite text on save. ([QueryForm](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryForm.java))

## Adoption and extension points

- Supply the allowed schema for each described source, using the same product language/functions that the runtime will use. ([SchemaCatalog](../jmouse-query-store/src/main/java/org/jmouse/query/store/SchemaCatalog.java))

- Use the map implementation when persistence is unnecessary or as a reference for store behavior. It is an implementation of the same port, not a separate API. ([MemorySavedQueries](../jmouse-query-store/src/main/java/org/jmouse/query/store/memory/MemorySavedQueries.java))

- Provide storage for source declarations separately from saved questions. Parse/bind and table publication validation belong in the caller before this text-only port. ([AuthoredSources](../jmouse-query-store/src/main/java/org/jmouse/query/store/AuthoredSources.java))

- Use declared source origin to distinguish editable authored mappings from structures derived by the product. ([SourceOrigin](../jmouse-query-store/src/main/java/org/jmouse/query/store/SourceOrigin.java))

## Constraints worth preserving

- A view checked against one source and run against another could pass validation while querying different data; the explicit target consistency check prevents this. ([QueryLibrary](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryLibrary.java))

- Canonical rewriting loses comments, so normalization is offered to callers rather than applied implicitly while saving. ([QueryLibrary](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryLibrary.java))

- Owner kind and identifier are product vocabulary. The library does not hard-code a scope enumeration or foreign-key to a product entity. ([QueryOwner](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryOwner.java))

- Persisting a body does not prove it is safe to run. A table allow-list and authorization for source editing must be supplied by the integration. ([AuthoredSources](../jmouse-query-store/src/main/java/org/jmouse/query/store/AuthoredSources.java))

- Transaction demarcation belongs to the caller. A successful text check does not create a transaction for surrounding product rows. ([QueryLibrary](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryLibrary.java))


## Source navigation

- [`org.jmouse.query.store`](../jmouse-query-store/src/main/java/org/jmouse/query/store/)
- [`org.jmouse.query.store.exception`](../jmouse-query-store/src/main/java/org/jmouse/query/store/exception/)
- [`org.jmouse.query.store.memory`](../jmouse-query-store/src/main/java/org/jmouse/query/store/memory/)

Executable examples are present in the module source tree. They are navigation aids; this document does not report running them:

- [StoreSmoke](../jmouse-query-store/src/main/java/org/jmouse/query/store/smoke/StoreSmoke.java)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-query-store/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-query` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.query.store`

| Type | Responsibility / contract |
|---|---|
| [AuthoredSource](../jmouse-query-store/src/main/java/org/jmouse/query/store/AuthoredSource.java) | A declaration somebody wrote and kept — the structure and mapping for one source. |
| [AuthoredSources](../jmouse-query-store/src/main/java/org/jmouse/query/store/AuthoredSources.java) | Where authored declarations are kept. |
| [QueryForm](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryForm.java) | 📐 The two shapes a saved query comes in. |
| [QueryLibrary](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryLibrary.java) | 📖 The saved queries of an installation — read, written, and never accepted broken. |
| [QueryOwner](../jmouse-query-store/src/main/java/org/jmouse/query/store/QueryOwner.java) | 🏷️ Where a saved query belongs — a board, a workspace, a person, or the installation itself. |
| [SavedQueries](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQueries.java) | 🗄️ Where saved queries are kept. |
| [SavedQuery](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQuery.java) | 💾 A query somebody kept: a name, where it belongs, and the jMQ behind it. |
| [SavedQueryCriteria](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQueryCriteria.java) | Value record: `(QueryOwner owner, String source, String reader)`. |
| [SavedQueryDraft](../jmouse-query-store/src/main/java/org/jmouse/query/store/SavedQueryDraft.java) | ✍️ What a caller supplies when saving — everything a query is, minus everything the store decides. |
| [SchemaCatalog](../jmouse-query-store/src/main/java/org/jmouse/query/store/SchemaCatalog.java) | 📚 What each described source lets a query say. |
| [SourceOrigin](../jmouse-query-store/src/main/java/org/jmouse/query/store/SourceOrigin.java) | Where a source's declaration comes from — and therefore whether anybody may edit it. |

#### `org.jmouse.query.store.exception`

| Type | Responsibility / contract |
|---|---|
| [QueryStoreException](../jmouse-query-store/src/main/java/org/jmouse/query/store/exception/QueryStoreException.java) | 🚫 Something went wrong keeping a query, as opposed to reading or running one. |
| [SavedQueryNotFoundException](../jmouse-query-store/src/main/java/org/jmouse/query/store/exception/SavedQueryNotFoundException.java) | 🚫 A saved query nobody kept, or nobody kept any more. |

#### `org.jmouse.query.store.memory`

| Type | Responsibility / contract |
|---|---|
| [MemorySavedQueries](../jmouse-query-store/src/main/java/org/jmouse/query/store/memory/MemorySavedQueries.java) | 🗃️ Saved queries kept in a map. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [StoreSmoke.java](../jmouse-query-store/src/main/java/org/jmouse/query/store/smoke/StoreSmoke.java).
