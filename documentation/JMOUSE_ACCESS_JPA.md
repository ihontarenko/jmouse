# jmouse-access-jpa

Jakarta Persistence adapters and schema for authorization grants, role definitions, entitlement grants, switches, consumption counters, plans and editable policy revisions. Read ports serve the engine; administration and disclosure ports answer different questions and expose different surfaces.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-access-jpa/pom.xml). Direct dependency declarations include `jmouse-access`, `jakarta.persistence-api`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [JpaGrantStore](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaGrantStore.java) | The engine's grants, out of the engine's own tables. ⚠️ This class used to live in a product, and that was the defect Stage 2 moved the schema into this library and left the store — and the @Entity classes it reads — on the other side of the seam. What that produced was a product mapping classes onto a schema it does not own, kept honest by ddl-auto: validate, which is a hope rather than a contract. Whoever owns the table… |
| [JpaEntitlementStore](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaEntitlementStore.java) | The engine's capability grants, out of the engine's own table. |
| [AccessAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/AccessAdministration.java) | Everything that changes authorization — the half the engine implied and never had. |
| [JpaAccessAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaAccessAdministration.java) | Changing authorization, against the engine's own tables. |
| [AccessDisclosure](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/AccessDisclosure.java) | The question the runtime never asks: who holds this , rather than does this person hold it . |
| [JpaAccessDisclosure](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaAccessDisclosure.java) | Who holds what, read straight off the engine's tables. ⚠️ Two queries and a map, never one query per row Roles are loaded once into a name-and-bundle map and the assignments are joined against it in memory. The obvious shape — entityManager.find(AccessRole.class, …) per assignment — is an N+1 over the largest table in the schema, on the one screen somebody opens when they are already having a bad day. |
| [EntitlementAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/EntitlementAdministration.java) | Everything that changes the entitlement axis — the counterpart of AccessAdministration for what a place may have rather than for what a person may do. |
| [JpaEntitlementAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaEntitlementAdministration.java) | Changing the entitlement axis, against the engine's own tables. |
| [JpaConsumptionCounters](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaConsumptionCounters.java) | Counters out of the engine's own table — both halves, behind two interfaces. It implements the read ConsumptionCounters and the write ConsumptionRecording separately so that whoever is handed one cannot reach the other. One class because they are one table; two interfaces because a decision may read and must not write. |
| [ConsumptionRecording](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/ConsumptionRecording.java) | Adding to a counter — the write half, kept away from the engine on purpose. The same split EntitlementAdministration makes against EntitlementStore, and for the same reason: the engine gets an interface it can only read through, so no amount of convenience later turns a decision into a write. Recording is the product's act, made after its own transaction committed, from the code that knows what was actually consumed. |
| [PlanTemplates](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/PlanTemplates.java) | The tier catalogue — which tiers exist and what each one includes . |
| [JpaPlanTemplates](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaPlanTemplates.java) | The tier catalogue, out of the engine's own tables. |
| [PolicyRevisions](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/PolicyRevisions.java) | The editable policy document's history — append-only, read from its newest end. |
| [JpaPolicyRevisions](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaPolicyRevisions.java) | The policy history, against the engine's own table. Plain JPA and JPQL, for the reason every store in this module gives: jakarta.persistence and nothing else, so adopting the engine's storage does not also mean adopting a repository framework. |
| [StoredConditions](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/StoredConditions.java) | Condition source out of a column, as something the engine can ask. |
| [ProjectedAccessTargetResolver](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/ProjectedAccessTargetResolver.java) | A resolver that answers by projecting two columns , never by loading a row. Most of the answer is the same shape everywhere: an owner identifier and a place identifier, read off one table. Writing that fourteen times would be fourteen chances to load an aggregate by accident on the security path — which is the one path where "it is only one extra query" is charged on every request rather than on one screen. A subclass says… |
| [AccessMigrations](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/migration/AccessMigrations.java) | 🚚 Where the access engine's own schema lives, and under what history. The library migrates itself, against a history table of its own — the arrangement jmouse-storage-jpa already proves, and for the identical reason. Products in one workspace number their migration ranges incompatibly, so a shared history table would force either a reserved range held forever by convention, or somebody renumbering a schema that has… |
| [AccessDialect](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/migration/AccessDialect.java) | 🗣️ The SQL dialects the access engine ships migrations for, and how one is chosen. Chosen from the data source rather than from an application profile, and that is deliberate: a product may have no profile naming its database at all, or may name it differently from every other product. The connection always knows. |

## Main flow

- Read role assignments and direct permissions from the library tables as core RoleGrant and DirectGrant values. The engine remains independent of JPA entities. ([JpaGrantStore](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaGrantStore.java))

- Reconstitute condition predicates from stored source through this seam. The persistence module depends on the access core rather than owning an EL compiler. ([StoredConditions](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/StoredConditions.java))

- Change role definitions, assignments and direct permissions through the administration port. Changes are returned for the product to record or present. ([JpaAccessAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaAccessAdministration.java))

- Answer who holds a permission with shared role loading and assignment mapping. This is an administrative disclosure operation rather than the runtime does-this-subject-hold-it lookup. ([JpaAccessDisclosure](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaAccessDisclosure.java))

- Return entitlement rows including expired rows. The resolver, with its clock and validity semantics, decides whether they currently apply. ([JpaEntitlementStore](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaEntitlementStore.java))

- Expose separate consumption read and write interfaces over the same counter implementation. The authorization decision path should depend only on the read half. ([JpaConsumptionCounters](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaConsumptionCounters.java))

## Adoption and extension points

- Use the library migration locations and access history descriptor when wiring schema initialization. Migration execution is supplied by the adopting integration. ([AccessMigrations](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/migration/AccessMigrations.java))

- Resolve the SQL dialect from the data source rather than inferring it from a product profile name. ([AccessDialect](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/migration/AccessDialect.java))

- Project the owner/place columns needed for authorization rather than loading an entire domain object just to find its target. ([ProjectedAccessTargetResolver](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/ProjectedAccessTargetResolver.java))

- Keep editable policy history through the append-oriented revision port. Plans remain templates; the actual entitlement grants are the facts the engine reads. ([JpaPolicyRevisions](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaPolicyRevisions.java))

## Constraints worth preserving

- Subject identifiers are scalar product identifiers, not JPA associations into an application account table. ([AccessRoleAssignment](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessRoleAssignment.java))

- A role permission carries a scope kind, while an assignment names the concrete place at which the role is held. ([AccessRolePermission](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessRolePermission.java))

- This adapter does not audit writes on the product behalf. Preserve its Change result when implementing an audit trail. ([JpaAccessAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaAccessAdministration.java))

- Entitlement administration likewise reports changes without inventing a product audit policy. ([JpaEntitlementAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaEntitlementAdministration.java))

- A revision stores full source. Treat it as historical authorization source rather than a diff that requires reconstructing a chain. ([AccessPolicyRevision](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessPolicyRevision.java))


## Source navigation

- [`org.jmouse.access.jpa`](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/)
- [`org.jmouse.access.jpa.entity`](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/)
- [`org.jmouse.access.jpa.migration`](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/migration/)

Resource entry points:

- [V000001__create_access_tables.sql](../jmouse-access-jpa/src/main/resources/db/access/mysql/V000001__create_access_tables.sql)
- [V000002__create_consumption_counters.sql](../jmouse-access-jpa/src/main/resources/db/access/mysql/V000002__create_consumption_counters.sql)
- [V000003__add_role_assignment_reason.sql](../jmouse-access-jpa/src/main/resources/db/access/mysql/V000003__add_role_assignment_reason.sql)
- [V000001__create_access_tables.sql](../jmouse-access-jpa/src/main/resources/db/access/postgresql/V000001__create_access_tables.sql)
- [V000002__create_consumption_counters.sql](../jmouse-access-jpa/src/main/resources/db/access/postgresql/V000002__create_consumption_counters.sql)
- [V000003__add_role_assignment_reason.sql](../jmouse-access-jpa/src/main/resources/db/access/postgresql/V000003__add_role_assignment_reason.sql)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-access-jpa/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-access` | compile | no |
| `jakarta.persistence:jakarta.persistence-api` | provided | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.access.jpa`

| Type | Responsibility / contract |
|---|---|
| [AccessAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/AccessAdministration.java) | Everything that changes authorization — the half the engine implied and never had. |
| [AccessDisclosure](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/AccessDisclosure.java) | The question the runtime never asks: who holds this , rather than does this person hold it . |
| [ConsumptionRecording](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/ConsumptionRecording.java) | Adding to a counter — the write half, kept away from the engine on purpose. |
| [EntitlementAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/EntitlementAdministration.java) | Everything that changes the entitlement axis — the counterpart of AccessAdministration for what a place may have rather than for what a person may do. |
| [JpaAccessAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaAccessAdministration.java) | Changing authorization, against the engine's own tables. |
| [JpaAccessDisclosure](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaAccessDisclosure.java) | Who holds what, read straight off the engine's tables. |
| [JpaConsumptionCounters](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaConsumptionCounters.java) | Counters out of the engine's own table — both halves, behind two interfaces. |
| [JpaEntitlementAdministration](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaEntitlementAdministration.java) | Changing the entitlement axis, against the engine's own tables. |
| [JpaEntitlementStore](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaEntitlementStore.java) | The engine's capability grants, out of the engine's own table. |
| [JpaGrantStore](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaGrantStore.java) | The engine's grants, out of the engine's own tables. |
| [JpaPlanTemplates](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaPlanTemplates.java) | The tier catalogue, out of the engine's own tables. |
| [JpaPolicyRevisions](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/JpaPolicyRevisions.java) | The policy history, against the engine's own table. |
| [PlanTemplates](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/PlanTemplates.java) | The tier catalogue — which tiers exist and what each one includes . |
| [PolicyRevisions](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/PolicyRevisions.java) | The editable policy document's history — append-only, read from its newest end. |
| [ProjectedAccessTargetResolver](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/ProjectedAccessTargetResolver.java) | A resolver that answers by projecting two columns , never by loading a row. |
| [StoredConditions](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/StoredConditions.java) | Condition source out of a column, as something the engine can ask. |

#### `org.jmouse.access.jpa.entity`

| Type | Responsibility / contract |
|---|---|
| [AccessCapabilityGrant](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessCapabilityGrant.java) | What a place has been granted, by whom, and until when — the entitlement axis. |
| [AccessCapabilitySwitch](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessCapabilitySwitch.java) | What a place has turned off — axis 4, and it is a preference rather than a grant. |
| [AccessConsumptionCounter](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessConsumptionCounter.java) | How much one subject has used of one meter, in one window. |
| [AccessPlan](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessPlan.java) | One tier: a named template of capabilities somebody may be put on. |
| [AccessPlanGrant](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessPlanGrant.java) | One capability a tier includes, with its ceiling where it has one. |
| [AccessPolicyRevision](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessPolicyRevision.java) | One saved version of the editable policy document — append-only, read from its newest end. |
| [AccessRole](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessRole.java) | A named bundle of permissions — and nothing else . |
| [AccessRoleAssignment](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessRoleAssignment.java) | Who holds a role, and where. |
| [AccessRolePermission](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessRolePermission.java) | One permission inside a role's bundle, and how far the role carries it. |
| [AccessSubjectPermission](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/entity/AccessSubjectPermission.java) | One permission handed to, or taken from, one subject directly . |

#### `org.jmouse.access.jpa.migration`

| Type | Responsibility / contract |
|---|---|
| [AccessDialect](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/migration/AccessDialect.java) | 🗣️ The SQL dialects the access engine ships migrations for, and how one is chosen. |
| [AccessMigrations](../jmouse-access-jpa/src/main/java/org/jmouse/access/jpa/migration/AccessMigrations.java) | 🚚 Where the access engine's own schema lives, and under what history. |

### Packaged resources

- [db/access/mysql/V000001__create_access_tables.sql](../jmouse-access-jpa/src/main/resources/db/access/mysql/V000001__create_access_tables.sql).
- [db/access/mysql/V000002__create_consumption_counters.sql](../jmouse-access-jpa/src/main/resources/db/access/mysql/V000002__create_consumption_counters.sql).
- [db/access/mysql/V000003__add_role_assignment_reason.sql](../jmouse-access-jpa/src/main/resources/db/access/mysql/V000003__add_role_assignment_reason.sql).
- [db/access/postgresql/V000001__create_access_tables.sql](../jmouse-access-jpa/src/main/resources/db/access/postgresql/V000001__create_access_tables.sql).
- [db/access/postgresql/V000002__create_consumption_counters.sql](../jmouse-access-jpa/src/main/resources/db/access/postgresql/V000002__create_consumption_counters.sql).
- [db/access/postgresql/V000003__add_role_assignment_reason.sql](../jmouse-access-jpa/src/main/resources/db/access/postgresql/V000003__add_role_assignment_reason.sql).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
