# jmouse-access-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-access-spring-boot` · source: [module POM](../jmouse-spring/jmouse-access-spring-boot/pom.xml).

## Purpose and boundaries

Connects jMouse access declarations to Spring AOP, adds persisted grant and entitlement adapters, reads policy documents, and installs the access schema. Authorization semantics remain in the access engine and enforcement modules; the product supplies authentication and its scope vocabulary.

## How a product adopts it

Provide AccessEngine, ScopeCatalog, EngineRefusals, CurrentSubject, RefusalHandler and target resolvers appropriate to the application. Use @RequiresAccess and @AccessContext or contribute ExternalAccessRules for library controllers. Supply policy locations only when using declarative policy grants, and include access JPA entities in persistence scanning for stored grants.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-access-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-access`.
- `jmouse-access-enforcement`.
- `jmouse-access-jpa` (optional dependency).
- `jmouse-access-policy` (optional dependency).
- `jmouse-access-el` (optional dependency).

## Runtime path

AccessAutoConfiguration is ordinary @Configuration listed in the Boot imports resource and is conditional on an AccessEngine bean. Its infrastructure advisor invokes AccessAuthorizationInterceptor after earlier advisors. The pointcut includes access requirements, published contexts and externally declared methods. Policy configuration composes policy and stored grant sources; JPA configuration creates shared EntityManager adapters.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [AccessAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/AccessAutoConfiguration.java) | Turns RequiresAccess into advice, so an endpoint's declaration is enforced rather than merely documented. |
| [AccessAuthorizationInterceptor](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/AccessAuthorizationInterceptor.java) | Enforces RequiresAccess as Spring advice. |
| [AccessJpaAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/jpa/AccessJpaAutoConfiguration.java) | The engine's storage, wired. |
| [AccessPolicyAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/AccessPolicyAutoConfiguration.java) | Turns "there are .jmp files on the classpath" into a working GrantStore. |
| [AccessPolicyProperties](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/AccessPolicyProperties.java) | Where an application keeps its policy files, and what to call what they add up to. |
| [CompiledStoredConditions](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/jpa/CompiledStoredConditions.java) | A stored condition, compiled once per distinct expression rather than once per row. |
| [EnvironmentPlaceholders](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/EnvironmentPlaceholders.java) | ${…} in a policy file, filled from ordinary application configuration. |
| [OnPolicyLocationsCondition](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/OnPolicyLocationsCondition.java) | Whether this application configured any policy locations. |
| [SpringParameterNaming](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/SpringParameterNaming.java) | What Spring MVC calls a handler's parameters. |
| [AccessFlywayAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/migration/AccessFlywayAutoConfiguration.java) | 🚚 Wires the library's migrations in, and makes the product's own run after them. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-access-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.access.spring.AccessAutoConfiguration
org.jmouse.access.spring.policy.AccessPolicyAutoConfiguration
org.jmouse.access.spring.migration.AccessFlywayAutoConfiguration
org.jmouse.access.spring.jpa.AccessJpaAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [AccessAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/AccessAutoConfiguration.java) | `@Configuration(proxyBeanMethods = false)`<br>`@ConditionalOnBean(AccessEngine.class)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnMissingBean(name = "accessAuthorizationAdvisor")` |
| [AccessJpaAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/jpa/AccessJpaAutoConfiguration.java) | `@AutoConfiguration(afterName = { "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration", "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration" })`<br>`@ConditionalOnClass({EntityManager.class, JpaGrantStore.class})`<br>`@ConditionalOnMissingBean` |
| [AccessFlywayAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/migration/AccessFlywayAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({Flyway.class, AccessMigrations.class, DataSource.class})`<br>`@ConditionalOnProperty(name = "jmouse.access.migrations.enabled", havingValue = "true", matchIfMissing = true)`<br>`@ConditionalOnMissingBean(AccessFlywayMigrator.class)` |
| [AccessPolicyAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/AccessPolicyAutoConfiguration.java) | `@AutoConfiguration`<br>`@ConditionalOnClass({PolicyLoader.class, PolicyGrantStore.class})`<br>`@ConditionalOnBean(ScopeCatalog.class)`<br>`@Conditional(OnPolicyLocationsCondition.class)`<br>`@EnableConfigurationProperties(AccessPolicyProperties.class)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnMissingBean(name = COMPOSED_STORE)`<br>`@ConditionalOnMissingBean(name = POLICY_ENTITLEMENTS)`<br>`@ConditionalOnMissingBean(name = COMPOSED_ENTITLEMENTS)` |

## Configuration contract

jmouse.access.policy.locations = empty list; without locations the policy activation condition does not match. jmouse.access.policy.name = policy. jmouse.access.migrations.enabled defaults to true. Stored grants use the stable bean name jpaGrantStore; composite beans in AccessPolicyAutoConfiguration should be read when replacing one source.

The definitions that own these settings and route placeholders are:

- [AccessPolicyProperties](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/AccessPolicyProperties.java).

## HTTP and protocol surface

This module declares no MVC controller routes. It provides services, configuration, lifecycle or view integration to the host. Any endpoints described by a dependent domain library or a separate management artifact are not implicitly owned by this adapter.

## Lifecycle and adoption gotchas

The default AccessContextScope is thread-bound. A host moving checks to another thread must provide a context scope that preserves publication there. Declare access rules by controller and method rather than rewriting authorization when routes move. Multiple ambient and external contributors are aggregated, while required product catalogs are deliberately not guessed. Without a ConditionCompiler stored conditions use StoredConditions.none().

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-access-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-access-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-access-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-access` | compile | no |
| `org.jmouse:jmouse-access-enforcement` | compile | no |
| `org.jmouse:jmouse-access-jpa` | compile | yes |
| `org.jmouse:jmouse-access-policy` | compile | yes |
| `org.jmouse:jmouse-access-el` | compile | yes |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.springframework:spring-context` | compile | yes |
| `org.springframework:spring-aop` | compile | yes |
| `org.springframework:spring-web` | compile | yes |
| `org.flywaydb:flyway-core` | compile | yes |
| `org.springframework:spring-tx` | compile | yes |
| `org.springframework:spring-orm` | compile | yes |
| `jakarta.persistence:jakarta.persistence-api` | compile | yes |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.access.spring`

| Type | Responsibility / contract |
|---|---|
| [AccessAuthorizationInterceptor](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/AccessAuthorizationInterceptor.java) | Enforces RequiresAccess as Spring advice. |
| [AccessAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/AccessAutoConfiguration.java) | Turns RequiresAccess into advice, so an endpoint's declaration is enforced rather than merely documented. |
| [SpringParameterNaming](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/SpringParameterNaming.java) | What Spring MVC calls a handler's parameters. |

#### `org.jmouse.access.spring.jpa`

| Type | Responsibility / contract |
|---|---|
| [AccessJpaAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/jpa/AccessJpaAutoConfiguration.java) | The engine's storage, wired. |
| [CompiledStoredConditions](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/jpa/CompiledStoredConditions.java) | A stored condition, compiled once per distinct expression rather than once per row. |

#### `org.jmouse.access.spring.migration`

| Type | Responsibility / contract |
|---|---|
| [AccessFlywayAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/migration/AccessFlywayAutoConfiguration.java) | 🚚 Wires the library's migrations in, and makes the product's own run after them. |
| [AccessFlywayMigrator](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/migration/AccessFlywayMigrator.java) | 🚚 Runs the access engine's own migrations, against its own history table. |

#### `org.jmouse.access.spring.policy`

| Type | Responsibility / contract |
|---|---|
| [AccessPolicyAutoConfiguration](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/AccessPolicyAutoConfiguration.java) | Turns "there are .jmp files on the classpath" into a working GrantStore. |
| [AccessPolicyProperties](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/AccessPolicyProperties.java) | Where an application keeps its policy files, and what to call what they add up to. |
| [EnvironmentPlaceholders](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/EnvironmentPlaceholders.java) | ${…} in a policy file, filled from ordinary application configuration. |
| [OnPolicyLocationsCondition](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/OnPolicyLocationsCondition.java) | Whether this application configured any policy locations. |
| [SpringPolicySources](../jmouse-spring/jmouse-access-spring-boot/src/main/java/org/jmouse/access/spring/policy/SpringPolicySources.java) | Policy files as Spring resources — classpath:, file:, and the patterns that go with them. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-access-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
