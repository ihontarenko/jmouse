# jmouse-query-spring-boot

> Spring integration module · Maven artifact `org.jmouse:jmouse-query-spring-boot` · source: [module POM](../jmouse-spring/jmouse-query-spring-boot/pom.xml).

## Purpose and boundaries

Integrates jMQ source catalogues and SQL compilation with a product DataSource. It adds eager query-interface proxies, builder/schema routes, reusable saved views, authored sources and an optional execution playground over the query core and JPA store.

## How a product adopts it

Publish source definitions at classpath:jmq/sources.jmq or set jmouse.query.sources. Supply a DataSource; create product query beans through QueryRepositories.create(interfaceClass) and annotate methods with @JmQuery and named @Parameter values. Publish QuerySubject entries and caller visibility adapters for web builders. Include query store entities when using persisted sources and views.

The dependency surface is declared in the [module POM](../jmouse-spring/jmouse-query-spring-boot/pom.xml). This integration module uses these library artifacts:

- `jmouse-query-sql`.
- `jmouse-query-store` (optional dependency).
- `jmouse-query-store-jpa` (optional dependency).

## Runtime path

QueryAutoConfiguration builds QueryEngine only when the sources resource and DataSource exist, detects a dialect unless explicitly named, and conditionally builds repositories and QueryRunner. QueryBuilderAutoConfiguration imports the builder controller in web applications. QueryStoreAutoConfiguration adds store migrations, persistence adapters and further controllers according to available ports and infrastructure.

## Key class responsibilities

| Type | Responsibility |
|---|---|
| [QueryAutoConfiguration](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryAutoConfiguration.java) | Wires jMQ into an application: the sources, the dialect, and the factory that makes repositories. |
| [QueryBuilderAutoConfiguration](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryBuilderAutoConfiguration.java) | Wires the shared filter builder — the composer, the registry, and the two addresses. |
| [QueryStoreAutoConfiguration](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryStoreAutoConfiguration.java) | The saved-query store, for a product that put jmouse-query-store-jpa on its classpath. |
| [QueryProperties](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryProperties.java) | What a product tells the bridge — and it is deliberately two things. |
| [QueryRepositories](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/QueryRepositories.java) | Turns an interface of JmQuery-annotated methods into something that runs them. |
| [JmQuery](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/JmQuery.java) | Declares the source, projection and query text compiled for a repository interface method. |
| [Parameter](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/Parameter.java) | The name a method argument answers to inside the query. |
| [QueryRunner](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryRunner.java) | Running a jMQ filter over a declared source — the half that is not about any one product's rows. |
| [QuerySubjects](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QuerySubjects.java) | Every subject a product registered, by name. |
| [QueryCallers](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryCallers.java) | Who is asking — the one thing this module cannot work out for itself. |
| [PublishedTables](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/PublishedTables.java) | The tables an authored declaration is allowed to name. |
| [QuerySources](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/QuerySources.java) | Which declaration a subject actually runs against. |

## Registration and activation

Boot discovery file: [AutoConfiguration.imports](../jmouse-spring/jmouse-query-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Its exact registered classes are:

```text
org.jmouse.query.spring.autoconfigure.QueryAutoConfiguration
org.jmouse.query.spring.autoconfigure.QueryBuilderAutoConfiguration
org.jmouse.query.spring.autoconfigure.QueryStoreAutoConfiguration
```

Conditions below are copied from the linked source. They include configuration-level, nested-configuration and individual bean checks; the row is an inventory, not one conjunction applied to every bean. A classpath match alone does not provide the beans required by a constructor.

| Configuration source | Declared activation, ordering and backoff |
|---|---|
| [QueryAutoConfiguration](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryAutoConfiguration.java) | `@AutoConfiguration(afterName = { "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration", "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration" })`<br>`@ConditionalOnClass(QueryEngine.class)`<br>`@EnableConfigurationProperties(QueryProperties.class)`<br>`@ConditionalOnBean(DataSource.class)`<br>`@ConditionalOnMissingBean`<br>`@ConditionalOnResource(resources = "${jmouse.query.sources:classpath:jmq/sources.jmq}")`<br>`@Configuration(proxyBeanMethods = false)`<br>`@ConditionalOnBean(QueryEngine.class)`<br>`@ConditionalOnClass(JdbcTemplate.class)` |
| [QueryBuilderAutoConfiguration](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryBuilderAutoConfiguration.java) | `@AutoConfiguration(after = QueryAutoConfiguration.class)`<br>`@ConditionalOnClass(RestController.class)`<br>`@ConditionalOnWebApplication`<br>`@Import(QueryBuilderController.class)`<br>`@ConditionalOnMissingBean` |
| [QueryStoreAutoConfiguration](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryStoreAutoConfiguration.java) | `@AutoConfiguration(after = {QueryAutoConfiguration.class, QueryBuilderAutoConfiguration.class}, afterName = { "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration", "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration", "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration", "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration" })`<br>`@ConditionalOnClass({JpaSavedQueries.class, EntityManager.class})`<br>`@ConditionalOnBean(DataSource.class)`<br>`@ConditionalOnMissingBean(QueryStoreFlywayMigrator.class)`<br>`@ConditionalOnBean(EntityManagerFactory.class)`<br>`@ConditionalOnMissingBean(SavedQueries.class)`<br>`@ConditionalOnMissingBean(QueryLanguage.class)`<br>`@ConditionalOnBean({SavedQueries.class, SchemaCatalog.class})`<br>`@ConditionalOnMissingBean(QueryLibrary.class)`<br>`@Configuration(proxyBeanMethods = false)`<br>`@ConditionalOnClass(RestController.class)`<br>`@ConditionalOnWebApplication`<br>`@ConditionalOnMissingBean(SavedQueryController.class)`<br>`@ConditionalOnMissingBean(SourceController.class)`<br>`@ConditionalOnBean(QueryRunner.class)`<br>`@ConditionalOnMissingBean(PlaygroundController.class)`<br>`@ConditionalOnMissingBean(AuthoredSources.class)`<br>`@ConditionalOnMissingBean(PublishedTables.class)`<br>`@ConditionalOnMissingBean(QuerySources.class)` |

## Configuration contract

jmouse.query.sources = classpath:jmq/sources.jmq; jmouse.query.dialect = empty, meaning resolve from the DataSource. jmouse.query.published-tables defaults to an empty set. The route prefix is a placeholder in QueryRoutes, reproduced below. Store migration construction is conditional on DataSource availability rather than a separate universal migrations.enabled setting.

The definitions that own these settings and route placeholders are:

- [QueryProperties](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryProperties.java).
- [QueryRoutes](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryRoutes.java).

Route constants are expressions resolved by their consumer; `${…}` text is a placeholder, not a literal URL. Shared `ManagementEndpoints.ROOT` may itself be configured by the host.

| Constant | Exact source expression |
|---|---|
| [QueryRoutes](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryRoutes.java).PREFIX | `"${jmouse.query.management.prefix:" + ManagementEndpoints.ROOT + "/query/api}"` |

## HTTP and protocol surface

Read the class prefix and method mapping together. Constant names resolve through the route definitions above; method parameters and response DTOs are in the linked handler source.

| Verb | Class-level prefix | Method mapping | Handler |
|---|---|---|---|
| GET | `QueryRoutes.PREFIX` | `"/{subject}/schema"` | [QueryBuilderController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryBuilderController.java).schema |
| POST | `QueryRoutes.PREFIX` | `"/{subject}/translate"` | [QueryBuilderController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryBuilderController.java).translate |
| GET | `QueryRoutes.PREFIX` | `"/{subject}/views"` | [SavedQueryController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/SavedQueryController.java).list |
| POST | `QueryRoutes.PREFIX` | `"/views/batch"` | [SavedQueryController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/SavedQueryController.java).listMany |
| GET | `QueryRoutes.PREFIX` | `"/{subject}/projection"` | [SavedQueryController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/SavedQueryController.java).projection |
| POST | `QueryRoutes.PREFIX` | `"/{subject}/views"` | [SavedQueryController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/SavedQueryController.java).save |
| PUT | `QueryRoutes.PREFIX` | `"/{subject}/views/{identifier}"` | [SavedQueryController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/SavedQueryController.java).update |
| DELETE | `QueryRoutes.PREFIX` | `"/{subject}/views/{identifier}"` | [SavedQueryController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/SavedQueryController.java).remove |
| POST | `QueryRoutes.PREFIX` | `"/{subject}/playground"` | [PlaygroundController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/playground/PlaygroundController.java).compile |
| GET | `QueryRoutes.PREFIX` | `"/{subject}/source"` | [SourceController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/SourceController.java).declaration |
| POST | `QueryRoutes.PREFIX` | `"/sources/batch"` | [SourceController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/SourceController.java).declarationMany |
| PUT | `QueryRoutes.PREFIX` | `"/{subject}/source"` | [SourceController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/SourceController.java).rewrite |
| DELETE | `QueryRoutes.PREFIX` | `"/{subject}/source"` | [SourceController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/SourceController.java).revert |
| POST | `QueryRoutes.PREFIX` | `"/{subject}/source/validate"` | [SourceController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/SourceController.java).validate |
| POST | `QueryRoutes.PREFIX` | `"/{subject}/source/compose"` | [SourceController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/SourceController.java).compose |

## Lifecycle and adoption gotchas

Repository methods compile when create() builds the proxy, so bad source or parameters fail early. This is not Spring Data: there are no derived method names or implicit Pageable semantics. QueryRunner and the playground need deliberate product access control. PublishedTables is the exposure boundary for authored sources. Builder controllers can mount automatically in web apps; do not assume every management artifact is explicit-import only.

## Source and related reading

- [Build and dependencies](../jmouse-spring/jmouse-query-spring-boot/pom.xml).
- [Boot registration metadata](../jmouse-spring/jmouse-query-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).
- [Underlying library manual](JMOUSE_QUERY.md).
- [Spring integration family](JMOUSE_SPRING.md).

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spring/jmouse-query-spring-boot/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-query-sql` | compile | no |
| `org.jmouse:jmouse-query-store` | compile | yes |
| `org.jmouse:jmouse-query-store-jpa` | compile | yes |
| `org.flywaydb:flyway-core` | compile | yes |
| `jakarta.persistence:jakarta.persistence-api` | compile | yes |
| `org.springframework.boot:spring-boot-autoconfigure` | compile | yes |
| `org.springframework:spring-core` | compile | yes |
| `org.springframework:spring-web` | compile | yes |
| `org.springframework:spring-jdbc` | compile | yes |
| `org.springframework:spring-webmvc` | compile | yes |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.query.spring`

| Type | Responsibility / contract |
|---|---|
| [JmQuery](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/JmQuery.java) | jMQ on a repository method — the face JPQL has, over a language a person can also type into a URL. |
| [Parameter](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/Parameter.java) | The name a method argument answers to inside the query. |
| [QueryRepositories](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/QueryRepositories.java) | Turns an interface of JmQuery-annotated methods into something that runs them. |

#### `org.jmouse.query.spring.autoconfigure`

| Type | Responsibility / contract |
|---|---|
| [QueryAutoConfiguration](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryAutoConfiguration.java) | Wires jMQ into an application: the sources, the dialect, and the factory that makes repositories. |
| [QueryBuilderAutoConfiguration](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryBuilderAutoConfiguration.java) | Wires the shared filter builder — the composer, the registry, and the two addresses. |
| [QueryProperties](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryProperties.java) | What a product tells the bridge — and it is deliberately two things. |
| [QueryStoreAutoConfiguration](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/autoconfigure/QueryStoreAutoConfiguration.java) | The saved-query store, for a product that put jmouse-query-store-jpa on its classpath. |

#### `org.jmouse.query.spring.builder`

| Type | Responsibility / contract |
|---|---|
| [QueryBuilderController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryBuilderController.java) | The two addresses a filter builder talks to, for every subject in the product. |
| [QueryBuilders](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryBuilders.java) | The builder's two questions, answered for any subject. |
| [QueryCallers](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryCallers.java) | Who is asking — the one thing this module cannot work out for itself. |
| [QueryRequest](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryRequest.java) | What arrived with a builder request — everything a subject needs to answer, and nothing else. |
| [QueryRoutes](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryRoutes.java) | Where the shared builder answers. |
| [QueryRunner](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryRunner.java) | Running a jMQ filter over a declared source — the half that is not about any one product's rows. |
| [QuerySubject](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QuerySubject.java) | One thing a product lets people write queries about — its whole contribution to the shared builder. |
| [QuerySubjects](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QuerySubjects.java) | Every subject a product registered, by name. |
| [QueryViews](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/QueryViews.java) | What travels between a builder and the server. |
| [SavedQueryController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/builder/SavedQueryController.java) | The questions somebody kept — listed, saved, renamed and discarded, for every product at once. |

#### `org.jmouse.query.spring.playground`

| Type | Responsibility / contract |
|---|---|
| [PlaygroundController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/playground/PlaygroundController.java) | What a query compiles to — the statement, never the rows. |

#### `org.jmouse.query.spring.source`

| Type | Responsibility / contract |
|---|---|
| [PublishedTables](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/PublishedTables.java) | The tables an authored declaration is allowed to name. |
| [QuerySources](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/QuerySources.java) | Which declaration a subject actually runs against. |
| [SourceController](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/SourceController.java) | Reading and rewriting what a listing IS. |
| [UnpublishedTableException](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/source/UnpublishedTableException.java) | A declaration named a table this installation does not publish. |

#### `org.jmouse.query.spring.store`

| Type | Responsibility / contract |
|---|---|
| [QueryStoreFlywayMigrator](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/store/QueryStoreFlywayMigrator.java) | Creates the saved-query table, in the library's own history, before the product's Flyway runs. |

### Packaged resources

- [META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](../jmouse-spring/jmouse-query-spring-boot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports).

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [QueryAutoConfigurationTest.java](../jmouse-spring/jmouse-query-spring-boot/src/test/java/org/jmouse/query/spring/autoconfigure/QueryAutoConfigurationTest.java).
- [BridgeSmoke.java](../jmouse-spring/jmouse-query-spring-boot/src/main/java/org/jmouse/query/spring/smoke/BridgeSmoke.java).
