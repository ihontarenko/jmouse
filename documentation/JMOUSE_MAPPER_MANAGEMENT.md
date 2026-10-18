# jmouse-mapper-management

> Spring integration module · Maven artifact `org.jmouse:jmouse-mapper-management` · source: [module POM](../jmouse-spring/jmouse-mapper-management/pom.xml).

## Purpose and boundaries

Exposes the mapping builder as a reusable library endpoint: offers product-selected types, describes a type shape, converts builder rows into .jmm, and parses representable .jmm back into rows. The single mapping parser and translator remain in jmouse-mapper-el.

## How a product adopts it

Declare a MappableTypeSource with a class matcher and one or more package-root classes, then register MappingBuilderController with that bean using an explicit import or @Bean. The host decides which domain and response types users may browse and which permissions protect the builder.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-mapper-management/pom.xml). This integration module uses these library artifacts:

- `jmouse-mapper-el`.

## Runtime path

GET types scans through the supplied matcher and roots. GET shape inspects a named class. POST render passes a MappingDraft through MappingDrafts.toDocument and JmmSourceTranslator. POST parse uses JmmReader and MappingDrafts.toDraft, preserving the language implementation as the authority.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [MappableTypeSource](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappableTypeSource.java) | Which classes a product offers in the builder's two selects. |
| [MappingBuilderController](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderController.java) | The mapping builder's server half — the two selects, a pair's properties, and the two directions between rows and a document. |
| [MappingBuilderRoutes](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderRoutes.java) | Where these controllers answer. |

## Registration and activation

This artifact has no `AutoConfiguration.imports` resource. Its controller/configuration classes must be registered through the adoption path described above.

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| Explicit controller registration | The product supplies controller constructor dependencies; no automatic registration configuration is present here. |

## Configuration contract

jmouse.mapper.management.prefix falls back to the shared management root plus /mapper/api. No auto-configuration imports resource or enable flag is provided by this module. MappableTypeSource is a Java bean contract rather than a package pattern property.

The definitions that own these settings and route placeholders are:

- [MappingBuilderRoutes](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderRoutes.java).

Route constants are expressions resolved by their consumer; `${…}` text is a placeholder, not a literal URL. Shared `ManagementEndpoints.ROOT` may itself be configured by the host.

| Constant | Exact source expression |
|---|---|
| [MappingBuilderRoutes](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderRoutes.java).SEGMENT | `"/mapper/api"` |
| [MappingBuilderRoutes](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderRoutes.java).DEFAULT_PREFIX | `ManagementEndpoints.ROOT + SEGMENT` |
| [MappingBuilderRoutes](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderRoutes.java).PREFIX | `"${jmouse.mapper.management.prefix:" + DEFAULT_PREFIX + "}"` |

## HTTP and protocol surface

Read the class prefix and method mapping together. Constant names resolve through the route definitions above; method parameters and response DTOs are in the linked handler source.

| Verb | Class-level prefix | Method mapping | Handler |
|---|---|---|---|
| GET | `MappingBuilderRoutes.PREFIX` | `"/types"` | [MappingBuilderController](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderController.java).offered |
| GET | `MappingBuilderRoutes.PREFIX` | `"/shape"` | [MappingBuilderController](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderController.java).shape |
| POST | `MappingBuilderRoutes.PREFIX` | `"/render"` | [MappingBuilderController](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderController.java).render |
| POST | `MappingBuilderRoutes.PREFIX` | `"/parse"` | [MappingBuilderController](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderController.java).parse |

## Lifecycle and adoption gotchas

A broad type scan can disclose product types the UI should not offer; the product matcher is the boundary. An unrepresentable mapping raises UnshowableMappingException and yields HTTP 422 with a construct property, so a text editor can take over. Rendering is not saving: this controller does not own a mapping document store.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-mapper-management/pom.xml).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-mapper-management/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-mapper-el` | compile | no |
| `org.springframework:spring-web` | compile | no |
| `org.springframework:spring-context` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.mapper.management`

| Type | Responsibility / contract |
|---|---|
| [MappableTypeSource](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappableTypeSource.java) | Which classes a product offers in the builder's two selects. 📇 |
| [MappingBuilderController](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderController.java) | The mapping builder's server half — the two selects, a pair's properties, and the two directions between rows and a document. 🧾 |
| [MappingBuilderRoutes](../jmouse-spring/jmouse-mapper-management/src/main/java/org/jmouse/mapper/management/MappingBuilderRoutes.java) | Where these controllers answer. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
