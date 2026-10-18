# jmouse-access-projection

Reverse translation from administrative access rows to a policy document and readable .jmp source. The purpose is to show authorization currently held in tables, including changes made after installation, rather than display the shipped seed as if it were the live state.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-access-projection/pom.xml). Direct dependency declarations include `jmouse-access-jpa`, `jmouse-access-policy`, `jmouse-access-el`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java) | The authorization as the tables hold it right now, as the document that describes it. |
| [EntitlementProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/EntitlementProjection.java) | What each of these places is entitled to — as the lines somebody wrote , not as the rows those lines became. |

## Main flow

- Start with PolicyProjection.of(policyName), supply permissions, scopes, role definitions and role/direct holdings, and optionally plans and instance naming. The builder consumes administrative views, not EntityManager. ([PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java))

- project creates a PolicyDocument. Permission descriptions and through redirects are supplied explicitly; role bundles and subject holdings are sorted for readable output. ([PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java))

- render prefixes the generated-from-tables banner and invokes PolicyWriter on the projected document. ([PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java))

- at(administration, places) reads the entitlement grant views at the requested places and emits policy entitlement lines. ([EntitlementProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/EntitlementProjection.java))

- Rows carrying PLAN or TRIAL provenance with a source reference are collapsed to one corresponding bundle line per place/source/reference. Ordinary grants remain allow/deny lines. ([EntitlementProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/EntitlementProjection.java))

- Return the lines ordered by scope, kind and subject, ready for composition into a larger policy presentation. ([EntitlementProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/EntitlementProjection.java))

## Adoption and extension points

- Use the naming function when identifiers have a useful authored spelling. If it returns null the projection preserves the original identifier. ([PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java))

- Supply permission redirects through the three-argument permissions overload so an exported vocabulary retains its through semantics. ([PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java))

- The projection takes the rows the caller supplies; selecting which subjects and holdings the administrator may disclose remains outside it. ([PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java))

- Request the places whose entitlements the output should describe. This helper does not enumerate an installation hierarchy itself. ([EntitlementProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/EntitlementProjection.java))

## Constraints worth preserving

- The generated banner explicitly distinguishes current table contents from policy/name.jmp, the installation seed. Pasting a projection over a seed is not a live mutation API. ([PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java))

- Assignment reasons and direct-grant reasons are copied back into the policy model. Dropping them would cause an editor round trip to erase existing explanations. ([PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java))

- The projected document has empty actions, variables, capability declarations and entitlement lists; this builder is not a lossless export of every policy vocabulary section. ([PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java))

- Validity instants are rendered as local dates in ZoneId.systemDefault. The helper does not preserve sub-day timestamps in its textual dates. ([EntitlementProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/EntitlementProjection.java))

## API shape

With administrative collections already selected by the caller:

```java
String source = PolicyProjection.of("in-force")
        .permissions(permissions, describePermission)
        .scopes(scopes)
        .roles(roles)
        .holdings(roleHoldings, directHoldings)
        .render();
```

This renders the supplied state. It does not query a database, install a policy or write a seed file.

## Source navigation

- [`org.jmouse.access.projection`](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-access-projection/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-access-jpa` | compile | no |
| `org.jmouse:jmouse-access-policy` | compile | no |
| `org.jmouse:jmouse-access-el` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.access.projection`

| Type | Responsibility / contract |
|---|---|
| [EntitlementProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/EntitlementProjection.java) | What each of these places is entitled to — as the lines somebody wrote , not as the rows those lines became. |
| [PolicyProjection](../jmouse-access-projection/src/main/java/org/jmouse/access/projection/PolicyProjection.java) | The authorization as the tables hold it right now, as the document that describes it. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
