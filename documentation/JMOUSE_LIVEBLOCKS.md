# jmouse-liveblocks

> Spring integration module · Maven artifact `org.jmouse:jmouse-liveblocks` · source: [module POM](../jmouse-spring/jmouse-liveblocks/pom.xml).

## Purpose and boundaries

Defines the shared directive-resolution contract through which one product embeds another product live row in a document. It standardizes resolution, suggestion and answer metadata while each product implements its own DirectiveResolver.

## How a product adopts it

Implement and register DirectiveResolver beans for the directive families the product can serve, add the artifact, and set jmouse.liveblocks.enabled=true. Configure authentication and authorization for the route surface in the product; a resolver remains responsible for the visibility rules of its own data.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-liveblocks/pom.xml). This integration module uses these library artifacts:


## Runtime path

LiveBlocksAutoConfiguration aggregates registered resolvers in DirectiveResolution and mounts DirectiveResolveController. Requests carry typed directives rather than product controller knowledge. Resolution returns a status and resolved content; suggestions use the same resolver registry. The answers route exposes the available resolver answers through the shared controller.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [LiveBlocksAutoConfiguration](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/spring/LiveBlocksAutoConfiguration.java) | The route, mounted only when a product says so. |
| [DirectiveResolver](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/DirectiveResolver.java) | How this product answers one kind of directive. |
| [DirectiveResolution](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/DirectiveResolution.java) | Every directive a document contains, answered in one pass. |
| [Directive](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/Directive.java) | One ::: line, as a document wrote it. |
| [DirectiveStatus](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/DirectiveStatus.java) | How one directive turned out. |
| [ResolvedDirective](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/ResolvedDirective.java) | One directive, answered — and the shape a consumer draws without understanding it. |
| [DirectiveSuggestion](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/DirectiveSuggestion.java) | One thing a document could refer to — an answer to "what is there?" rather than "what is this?" . |
| [DirectiveResolveController](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/DirectiveResolveController.java) | POST /api/blocks/resolve — the one address every product that can be quoted serves. |
| [ResolveDirectivesRequest](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/ResolveDirectivesRequest.java) | Every directive one document contains, in one request. |
| [SuggestDirectivesRequest](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/SuggestDirectivesRequest.java) | What a picker is asking: one namespace, and however much has been typed so far. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-liveblocks/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.liveblocks.spring.LiveBlocksAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [LiveBlocksAutoConfiguration](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/spring/LiveBlocksAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass(DirectiveResolveController.class)`<br>`@ConditionalOnProperty(name = "jmouse.liveblocks.enabled", havingValue = "true")`<br>`@ConditionalOnMissingBean` |

## Configuration contract

jmouse.liveblocks.enabled is opt-in true. LiveBlockRoutes.PREFIX is the fixed /jmouse/blocks/api, with /resolve and /suggest constants derived from it. This module does not expose a configurable prefix or an independent persistence configuration.

The definitions that own these settings and route placeholders are:

- [LiveBlockRoutes](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/LiveBlockRoutes.java).

Route constants are expressions resolved by their consumer; `${…}` text is a placeholder, not a literal URL. Shared `ManagementEndpoints.ROOT` may itself be configured by the host.

| Constant | Exact source expression |
|---|---|
| [LiveBlockRoutes](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/LiveBlockRoutes.java).PREFIX | `"/jmouse/blocks/api"` |
| [LiveBlockRoutes](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/LiveBlockRoutes.java).RESOLVE | `PREFIX + "/resolve"` |
| [LiveBlockRoutes](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/LiveBlockRoutes.java).SUGGEST | `PREFIX + "/suggest"` |

## HTTP and protocol surface

Read the class prefix and method mapping together. Constant names resolve through the route definitions above; method parameters and response DTOs are in the linked handler source.

| Verb | Class-level prefix | Method mapping | Handler |
|---|---|---|---|
| POST | `LiveBlockRoutes.PREFIX` | `"/resolve"` | [DirectiveResolveController](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/DirectiveResolveController.java).resolve |
| POST | `LiveBlockRoutes.PREFIX` | `"/suggest"` | [DirectiveResolveController](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/DirectiveResolveController.java).suggest |
| GET | `LiveBlockRoutes.PREFIX` | `"/answers"` | [DirectiveResolveController](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/DirectiveResolveController.java).answers |

## Lifecycle and adoption gotchas

Resolvers are extension points, not a universal database lookup. Duplicate normalized directive names fail construction. Unknown directives return UNKNOWN_DIRECTIVE; a resolver failure or null resolution is logged or normalized to NOT_FOUND so the document can still render. Resolve accepts at most 100 directives. Suggest defaults to 10 results and caps at 50. Since the routes are enabled through a property, the host must align its proxy and security route matching with the fixed prefix.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-liveblocks/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-liveblocks/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-liveblocks/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.springframework:spring-web` | compile | no |
| `org.springframework:spring-context` | compile | no |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.liveblocks`

| Type | Responsibility / contract |
|---|---|
| [Directive](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/Directive.java) | One ::: line, as a document wrote it. |
| [DirectiveResolution](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/DirectiveResolution.java) | Every directive a document contains, answered in one pass. |
| [DirectiveResolver](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/DirectiveResolver.java) | How this product answers one kind of directive. |
| [DirectiveStatus](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/DirectiveStatus.java) | How one directive turned out. |
| [DirectiveSuggestion](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/DirectiveSuggestion.java) | One thing a document could refer to — an answer to "what is there?" rather than "what is this?" . |
| [ResolvedDirective](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/ResolvedDirective.java) | One directive, answered — and the shape a consumer draws without understanding it. |

#### `org.jmouse.liveblocks.spring`

| Type | Responsibility / contract |
|---|---|
| [LiveBlocksAutoConfiguration](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/spring/LiveBlocksAutoConfiguration.java) | The route, mounted only when a product says so. |

#### `org.jmouse.liveblocks.web`

| Type | Responsibility / contract |
|---|---|
| [DirectiveResolveController](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/DirectiveResolveController.java) | POST /api/blocks/resolve — the one address every product that can be quoted serves. |
| [LiveBlockRoutes](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/LiveBlockRoutes.java) | Where the contract lives on the wire. |
| [ResolveDirectivesRequest](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/ResolveDirectivesRequest.java) | Every directive one document contains, in one request. |
| [SuggestDirectivesRequest](../jmouse-spring/jmouse-liveblocks/src/main/java/org/jmouse/liveblocks/web/SuggestDirectivesRequest.java) | What a picker is asking: one namespace, and however much has been typed so far. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-liveblocks/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
