# jmouse-jdbc

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-jdbc`. Build/dependency declaration: [pom.xml](../jmouse-jdbc/pom.xml).

## What it is

JDBC operations built around explicit statement binding, row/result extraction, connection providers and an interception chain. It includes named SQL parameters, datasource routing, exception translation, bulk execution and integration with jmouse-transaction.

JdbcTemplate is the operation facade; JdbcExecutor executes calls; ConnectionProvider owns acquisition/release; transaction sessions manage transactional connection state. Row mapping belongs to RowMapper/ResultSetExtractor rather than the executor.

## The map

| Type | Responsibility |
| --- | --- |
| [JdbcTemplate](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/JdbcTemplate.java) | High-level query, single-row, update, batch, generated-key and callable operations. |
| [NamedTemplate](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/NamedTemplate.java) | Named-parameter facade accepting ParameterSource, maps or beans. |
| [JdbcExecutor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/JdbcExecutor.java) | Lower-level execution contract used by templates. |
| [InterceptableJdbcExecutor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/InterceptableJdbcExecutor.java) | Dispatches JDBC calls through the configured interception chain. |
| [ConnectionProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/ConnectionProvider.java) | Acquisition/release seam independent of datasource selection. |
| [TransactionAwareConnectionProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/TransactionAwareConnectionProvider.java) | Reuses a transaction-bound connection and avoids premature release. |
| [RoutingConnectionProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/RoutingConnectionProvider.java) | Resolves a connection provider for routed datasource use. |
| [RowMapper](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/RowMapper.java) | Converts a ResultSet row into a typed value. |
| [ResultSetExtractor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/ResultSetExtractor.java) | Consumes the result set as a whole, including streaming/single-result policies. |
| [NamedSqlPreparedExecutionFactory](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/named/NamedSqlPreparedExecutionFactory.java) | Compiles named SQL and creates parameter binders/prepared execution data. |
| [JdbcTransactionSession](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/transaction/JdbcTransactionSession.java) | Acquires, configures, binds, commits/rolls back and restores one transactional connection. |
| [SQLStateSQLExceptionTranslator](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/SQLStateSQLExceptionTranslator.java) | Converts SQLState failures into the JDBC exception hierarchy. |

## Execution and ownership

1. Configure datasource/provider, executor chain and JdbcTemplate.Default or NamedTemplate.Default.
2. Describe statement parameters with StatementBinder or named ParameterSource and choose a RowMapper/ResultSetExtractor.
3. The executor acquires the connection, creates/configures the statement and executes the call through configured links/handlers.
4. Result extraction and exception translation finish the operation; the provider releases a non-transactional connection while transaction-owned connections remain bound until completion.

## Public API and behavior

querySingle returns Optional; queryOne expresses required single-result semantics. Choose deliberately for missing/multiple rows.

NamedTemplate compiles named SQL to positional SQL plus a binder. Batch named execution compiles the common SQL once for its parameter sets.

StatementConfigurer supplies options such as fetch size; StatementHandler adds execution-time observations/behavior.

JdbcTransactionSession implements SavepointSupport and restores connection state on close. It is scoped to one transaction/thread.

## Wiring and extension points

- Implement ConnectionProvider or DataSourceFactory for another acquisition mechanism.
- Add JdbcChainContributor/chain links for execution-wide behavior; use StatementHandler for statement-level hooks.
- Implement RowMapper, ResultSetExtractor, KeyExtractor or ParameterSource for application-specific data shapes.
- Enable SQLSafetyGuardLink with an explicit SQLGuardPolicy when statement scanning is part of the desired execution policy.

## Pitfalls

- A transactional provider must wrap the intended underlying provider; otherwise a template can acquire an unrelated connection.
- Do not share JdbcTransactionSession across threads or concurrent transactions.
- Streaming extraction requires consumption inside the JDBC resource lifetime; do not return a live ResultSet as an ordinary detached value.
- Smoke programs include MySQL-specific operations and may require a database; documentation generation does not imply those programs were executed.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-jdbc/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Existing executable examples/smokes: [SmokeA](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/smoke/SmokeA.java), [SmokeB](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/smoke/SmokeB.java), [SmokeNamedQueryMySql](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/smoke/SmokeNamedQueryMySql.java), [SmokeSeedMySql](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/smoke/SmokeSeedMySql.java).

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-jdbc/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.jmouse:jmouse-el` | compile | no |
| `org.jmouse:jmouse-context` | compile | no |
| `org.jmouse:jmouse-transaction` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |
| `com.mysql:mysql-connector-j` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.jdbc`

| Type | Responsibility / contract |
|---|---|
| [EnableJdbc](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/EnableJdbc.java) | 🗄️ Enables JDBC infrastructure for jMouse. |
| [InterceptableJdbcExecutor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/InterceptableJdbcExecutor.java) | JdbcExecutor implementation that supports interception via a Chain of JdbcExecutionContext execution steps. |
| [JdbcExecutor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/JdbcExecutor.java) | Low-level JDBC execution contract for jMouse. |
| [JdbcOperation](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/JdbcOperation.java) | Logical classification of JDBC operations executed by jMouse. |
| [JdbcSupport](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/JdbcSupport.java) | Operations: `closeQuietly`, `ensureDriver`. |
| [JdbcTemplate](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/JdbcTemplate.java) | High-level JDBC operations facade. |
| [NamedTemplate](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/NamedTemplate.java) | Extension of JdbcTemplate for SQL with named parameters. |
| [SQLExecutor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/SQLExecutor.java) | Default JdbcExecutor implementation performing direct JDBC execution. |

#### `org.jmouse.jdbc.bulk`

| Type | Responsibility / contract |
|---|---|
| [BulkBatchResult](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/bulk/BulkBatchResult.java) | Value record: `( int chunks, long affectedRows, List errors )`. |
| [BulkExecutionException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/bulk/BulkExecutionException.java) | Implements/extends `RuntimeException`. |
| [BulkExecutionPolicy](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/bulk/BulkExecutionPolicy.java) | Value record: `( int chunkSize, int parallelism, int maxInFlight, boolean failFast )`. |
| [ParallelBatchUpdater](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/bulk/ParallelBatchUpdater.java) | Operations: `execute`. |

#### `org.jmouse.jdbc.configuration`

| Type | Responsibility / contract |
|---|---|
| [JdbcConnectionProviderConfiguration](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/configuration/JdbcConnectionProviderConfiguration.java) | Operations: `dataSourceConnectionProvider`, `primaryConnectionProvider`. |
| [JdbcDataSourceConfiguration](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/configuration/JdbcDataSourceConfiguration.java) | Operations: `defaultDataSource`, `dataSourceSpecificationRegistry`, `dataSourceRegistry`, `dataSourceFactoryRegistry`, `dataSourceFactory`, `dataSourceResolver`, `dataSourceKey`. |
| [JdbcExecutorConfiguration](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/configuration/JdbcExecutorConfiguration.java) | 📦 JDBC executor + interception chain configuration. |
| [JdbcTemplateConfiguration](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/configuration/JdbcTemplateConfiguration.java) | 📦 JDBC template & named SQL infrastructure configuration. |
| [JdbcTransactionConfiguration](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/configuration/JdbcTransactionConfiguration.java) | 🧩 JDBC-based transaction configuration. |
| [MissingDataSourceConfiguration](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/configuration/MissingDataSourceConfiguration.java) | Fail-fast guard for JDBC bootstrap: ensures that a DataSource bean exists. |

#### `org.jmouse.jdbc.connection`

| Type | Responsibility / contract |
|---|---|
| [ConnectionProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/ConnectionProvider.java) | Strategy interface for providing and releasing JDBC Connection connections. |
| [DataSourceConnectionProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/DataSourceConnectionProvider.java) | ConnectionProvider backed by a DataSource. |
| [RoutingConnectionProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/RoutingConnectionProvider.java) | Implements/extends `AbstractConnectionProvider`. Operations: `getConnection`. |
| [TransactionAwareConnectionProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/TransactionAwareConnectionProvider.java) | Implements/extends `ConnectionProvider`. Operations: `getConnection`, `release`. |

#### `org.jmouse.jdbc.connection.datasource`

| Type | Responsibility / contract |
|---|---|
| [DataSourceContributor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DataSourceContributor.java) | Operations: `contribute`. |
| [DataSourceFactory](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DataSourceFactory.java) | Operations: `supports`, `create`. |
| [DataSourceFactoryRegistry](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DataSourceFactoryRegistry.java) | Operations: `register`, `supports`, `create`. |
| [DataSourceKey](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DataSourceKey.java) | Abstraction representing a logical key used to resolve a javax.sql.DataSource. |
| [DataSourceKeyHolder](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DataSourceKeyHolder.java) | Thread-bound holder for the current DataSourceKey. |
| [DataSourceRegistry](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DataSourceRegistry.java) | Operations: `register`, `obtain`, `require`, `snapshot`. |
| [DataSourceResolver](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DataSourceResolver.java) | Operations: `resolve`. |
| [DataSourceSpecification](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DataSourceSpecification.java) | Value record: `( String name, String driverClassName, String url, String username, String password, String catalog, String schema, Pool pool, // optional Properties properties )`. |
| [DataSourceSpecificationRegistry](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DataSourceSpecificationRegistry.java) | Operations: `register`, `obtain`, `require`, `snapshot`. |
| [DriverInstanceDataSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DriverInstanceDataSource.java) | Implements/extends `AbstractDriverBasedDataSource`. |
| [DriverInstanceDataSourceFactory](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DriverInstanceDataSourceFactory.java) | Implements/extends `DataSourceFactory`. Operations: `supports`, `create`. |
| [DriverManagerDataSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DriverManagerDataSource.java) | Implements/extends `AbstractDriverBasedDataSource`. |
| [DriverManagerDataSourceFactory](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/DriverManagerDataSourceFactory.java) | Implements/extends `DataSourceFactory`. Operations: `supports`, `create`, `priority`. |
| [ResolvableDataSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/ResolvableDataSource.java) | Implements/extends `AbstractDataSource`. Operations: `getConnection`. |
| [SimpleDataSourceResolver](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/SimpleDataSourceResolver.java) | Implements/extends `DataSourceResolver`. Operations: `resolve`. |

#### `org.jmouse.jdbc.connection.datasource.support`

| Type | Responsibility / contract |
|---|---|
| [AbstractDataSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/support/AbstractDataSource.java) | Implements/extends `DataSource`. Operations: `getLoginTimeout`, `setLoginTimeout`, `getLogWriter`, `setLogWriter`, `getParentLogger`, `unwrap`, `isWrapperFor`. |
| [AbstractDriverBasedDataSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/datasource/support/AbstractDriverBasedDataSource.java) | Base javax.sql.DataSource for driver-based implementations. |

#### `org.jmouse.jdbc.connection.support`

| Type | Responsibility / contract |
|---|---|
| [AbstractConnectionProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/connection/support/AbstractConnectionProvider.java) | Implements/extends `ConnectionProvider`. Operations: `release`. |

#### `org.jmouse.jdbc.dialect`

| Type | Responsibility / contract |
|---|---|
| [Dialect](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/dialect/Dialect.java) | The handful of places two databases disagree about how to write the same idea. |
| [Dialects](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/dialect/Dialects.java) | Which database this is — asked, never guessed. |
| [MySqlDialect](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/dialect/MySqlDialect.java) | MySQL — backticks, SIGNED, and a case-insensitive collation by default. |
| [PostgreSqlDialect](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/dialect/PostgreSqlDialect.java) | PostgreSQL — double quotes, ILIKE, &#124;&#124;, and a cast that raises rather than guessing. |

#### `org.jmouse.jdbc.exception`

| Type | Responsibility / contract |
|---|---|
| [BadSQLGrammarException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/BadSQLGrammarException.java) | Implements/extends `DataAccessException`. |
| [DataAccessException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/DataAccessException.java) | Implements/extends `RuntimeException`. |
| [DataIntegrityViolationException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/DataIntegrityViolationException.java) | Implements/extends `DataAccessException`. |
| [DuplicateKeyException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/DuplicateKeyException.java) | Implements/extends `DataAccessException`. |
| [EmptyResultException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/EmptyResultException.java) | Implements/extends `IncorrectResultSizeException`. |
| [IncorrectResultSizeException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/IncorrectResultSizeException.java) | Implements/extends `DataAccessException`. Operations: `getExpectedSize`, `getActualSize`. |
| [JdbcAccessException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/JdbcAccessException.java) | Implements/extends `RuntimeException`. Operations: `getCause`. |
| [NonUniqueResultException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/NonUniqueResultException.java) | Implements/extends `IncorrectResultSizeException`. |
| [QueryTimeoutException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/QueryTimeoutException.java) | Implements/extends `DataAccessException`. |
| [ResultSetAccessException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/ResultSetAccessException.java) | Implements/extends `DataAccessException`. |
| [SQLExceptionTranslator](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/SQLExceptionTranslator.java) | Operations: `translate`. |
| [SQLStateSQLExceptionTranslator](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/SQLStateSQLExceptionTranslator.java) | Implements/extends `SQLExceptionTranslator`. Operations: `translate`. |
| [UncategorizedSQLException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/exception/UncategorizedSQLException.java) | Implements/extends `DataAccessException`. |

#### `org.jmouse.jdbc.execution`

| Type | Responsibility / contract |
|---|---|
| [ExecutionDescriptor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/execution/ExecutionDescriptor.java) | 📦 Runtime execution descriptor. |
| [GetUsers](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/execution/GetUsers.java) | Implements/extends `ExecutionDescriptor`. Operations: `operation`, `rowMapper`. |
| [JdbcDescriptorOperations](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/execution/JdbcDescriptorOperations.java) | Implements/extends `NamedTemplate, JdbcTemplate`. Operations: `query`. |
| [SelfAnnotatedExecutionDescriptor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/execution/SelfAnnotatedExecutionDescriptor.java) | Implements/extends `ExecutionDescriptor`. |

#### `org.jmouse.jdbc.intercept`

| Type | Responsibility / contract |
|---|---|
| [JdbcBatchUpdateCall](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/JdbcBatchUpdateCall.java) | JdbcCall descriptor for executing a JDBC batch update operation. |
| [JdbcCall](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/JdbcCall.java) | Immutable descriptor of a single JDBC invocation. |
| [JdbcCallableCall](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/JdbcCallableCall.java) | JdbcCall descriptor for executing a JDBC callable statement (stored procedure or database function). |
| [JdbcChainContributor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/JdbcChainContributor.java) | Functional contract for contributing JDBC interception logic to a Chain. |
| [JdbcChainFactory](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/JdbcChainFactory.java) | Factory for building a fully-initialized JDBC interception Chain. |
| [JdbcExecutionContext](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/JdbcExecutionContext.java) | Value record: `(JdbcExecutor executor)`. |
| [JdbcKeyUpdateCall](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/JdbcKeyUpdateCall.java) | Value record: `( String sql, StatementBinder binder, StatementConfigurer configurer, StatementHandler handler, KeyUpdateCallback callback ) implements JdbcCall`. |
| [JdbcQueryCall](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/JdbcQueryCall.java) | JdbcCall descriptor for executing a query that produces a ResultSet. |
| [JdbcUpdateCall](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/JdbcUpdateCall.java) | JdbcCall descriptor for executing a JDBC update operation (INSERT / UPDATE / DELETE) that returns an update count. |
| [QueryTimeoutSupport](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/QueryTimeoutSupport.java) | Helper for applying transaction-scoped query timeouts to JDBC statements. |

#### `org.jmouse.jdbc.intercept.guard`

| Type | Responsibility / contract |
|---|---|
| [SQLGuardPolicy](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/guard/SQLGuardPolicy.java) | Value record: `( boolean multiStatement, boolean ddl, boolean udWithoutWhere )`. |
| [SQLGuardViolationException](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/guard/SQLGuardViolationException.java) | Implements/extends `IllegalStateException`. |
| [SQLSafetyGuardLink](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/guard/SQLSafetyGuardLink.java) | Guard link that validates SQL before execution. |
| [SQLScan](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/guard/SQLScan.java) | Value record: `( boolean hasStatementSeparator, boolean isDDL, boolean isUD, boolean hasWhere )`. |
| [SQLScanner](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/guard/SQLScanner.java) | Lightweight SQL analyzer used for safety checks. |

#### `org.jmouse.jdbc.intercept.link`

| Type | Responsibility / contract |
|---|---|
| [JdbcCallExecutorLink](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/link/JdbcCallExecutorLink.java) | Terminal chain link that executes a JdbcCall against the underlying JdbcExecutor. |
| [JdbcExceptionTranslationLink](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/link/JdbcExceptionTranslationLink.java) | Interceptor link responsible for translating SQLException SQLExceptions into jMouse DataAccessException hierarchy. |
| [QueryTimeoutInterceptor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/intercept/link/QueryTimeoutInterceptor.java) | Interceptor link that propagates the transaction-level query timeout into JDBC statement execution. |

#### `org.jmouse.jdbc.mapping`

| Type | Responsibility / contract |
|---|---|
| [BeanRowMapper](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/BeanRowMapper.java) | RowMapper implementation that maps a JDBC ResultSet row into a Java bean using the jMouse core Binder infrastructure. |
| [ColumnRowMapper](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/ColumnRowMapper.java) | RowMapper decorator that delegates mapping to another mapperProvider while forcing a specific column index. |
| [CompositeKeyExtractor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/CompositeKeyExtractor.java) | KeyExtractor implementation for composite (multi-column) generated keys. |
| [KeyExtractor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/KeyExtractor.java) | Strategy interface for extracting generated keys from a JDBC operation. |
| [ListResultSetExtractor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/ListResultSetExtractor.java) | ResultSetExtractor implementation that maps all rows of a ResultSet into a List. |
| [MapRowMapper](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/MapRowMapper.java) | RowMapper implementation that maps a single ResultSet row into a Map keyed by column names. |
| [NameStrategy](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/NameStrategy.java) | Strategy interface for transforming column labels or names into target property names. |
| [OptionalRowMapper](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/OptionalRowMapper.java) | RowMapper decorator that wraps the mapped value into an Optional. |
| [ResultSetExtractor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/ResultSetExtractor.java) | Strategy interface for extracting an arbitrary result from a JDBC ResultSet. |
| [ResultSetRowMetadata](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/ResultSetRowMetadata.java) | RowMetadata implementation backed by JDBC ResultSetMetaData. |
| [RowMapper](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/RowMapper.java) | Strategy interface for mapping a single row of a JDBC ResultSet into a domain object. |
| [RowMappers](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/RowMappers.java) | Factory class providing common RowMapper implementations for single-column scalar results. |
| [RowMetadata](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/RowMetadata.java) | Abstraction representing metadata of a single JDBC result row. |
| [SingleLongKeyExtractor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/SingleLongKeyExtractor.java) | KeyExtractor implementation for extracting a single Long generated key. |
| [SingleResultSetExtractor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/SingleResultSetExtractor.java) | ResultSetExtractor that returns the first row of a result set, or null if no rows are present. |
| [StreamingResultSetExtractor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/StreamingResultSetExtractor.java) | ResultSetExtractor implementation for streaming row-by-row processing. |
| [StrictSingleResultSetExtractor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/StrictSingleResultSetExtractor.java) | ResultSetExtractor that enforces exactly one result row. |

#### `org.jmouse.jdbc.mapping.bind`

| Type | Responsibility / contract |
|---|---|
| [JdbcAccessorWrapper](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/bind/JdbcAccessorWrapper.java) | JDBC-specific AccessorWrapper implementation. |
| [ResultSetAccessor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/bind/ResultSetAccessor.java) | ObjectAccessor implementation over a JDBC ResultSet. |
| [ResultSetAccessorProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/mapping/bind/ResultSetAccessorProvider.java) | ObjectAccessorProvider implementation for JDBC ResultSet. |

#### `org.jmouse.jdbc.parameters`

| Type | Responsibility / contract |
|---|---|
| [ArrayParameterSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/ArrayParameterSource.java) | ParameterSource implementation backed by a positional Object array. |
| [BeanParameterSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/BeanParameterSource.java) | Implements/extends `ParameterSource`. Operations: `hasValue`, `getValue`. |
| [CompositeParameterSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/CompositeParameterSource.java) | Implements/extends `ParameterSource`. Operations: `of`, `hasValue`, `getValue`. |
| [EmptyParameterSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/EmptyParameterSource.java) | Empty ParameterSource implementation. |
| [ListParameterSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/ListParameterSource.java) | ParameterSource implementation backed by a List of positional values. |
| [MapParameterSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/MapParameterSource.java) | ParameterSource implementation backed by a Map of named values. |
| [MissingParameterPolicy](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/MissingParameterPolicy.java) | Policy defining how missing SQL parameters should be handled during prepared-statement binding. |
| [ParameterSource](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/ParameterSource.java) | Operations: `hasValue`, `getValue`. |
| [SQLCompiled](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/SQLCompiled.java) | Value record: `(SQLPlan plan)`. |
| [SQLExpressionLanguage](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/SQLExpressionLanguage.java) | Implements/extends `ExpressionLanguage`. |
| [SQLParameterProcessor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/SQLParameterProcessor.java) | High-level processor for SQL parameter handling. |
| [SQLParameterToken](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/SQLParameterToken.java) | Enum values: `T_TEXT`, `T_NAMED_PARAMETER`, `T_POSITIONAL_PARAMETER`, `T_STRING`, `T_LINE_COMMENT`, `T_BLOCK_COMMENT`. |
| [SQLParsed](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/SQLParsed.java) | Value record: `(String name, StringSource source, List tokens)`. |
| [SQLPlan](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/SQLPlan.java) | Represents a prepared SQL parameterization plan. |

#### `org.jmouse.jdbc.parameters.bind`

| Type | Responsibility / contract |
|---|---|
| [SQLPlanPreparedStatementBinder](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/bind/SQLPlanPreparedStatementBinder.java) | StatementBinder implementation that binds parameters according to a precompiled SQLPlan. |

#### `org.jmouse.jdbc.parameters.compile`

| Type | Responsibility / contract |
|---|---|
| [SQLPlanCompiler](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/compile/SQLPlanCompiler.java) | Compiles tokenized SQL parameter sources into an executable SQLPlan. |

#### `org.jmouse.jdbc.parameters.lexer`

| Type | Responsibility / contract |
|---|---|
| [SQLParameterSplitter](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/lexer/SQLParameterSplitter.java) | Implements/extends `Splitter , TokenizableSource>`. Operations: `split`. |
| [SQLParameterTokenizer](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/lexer/SQLParameterTokenizer.java) | Implements/extends `Tokenizer`. Operations: `source`, `tokenize`. |

#### `org.jmouse.jdbc.parameters.named`

| Type | Responsibility / contract |
|---|---|
| [NamedSqlPreparedExecution](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/named/NamedSqlPreparedExecution.java) | Prepared SQL execution descriptor. |
| [NamedSqlPreparedExecutionFactory](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/parameters/named/NamedSqlPreparedExecutionFactory.java) | Factory for preparing executable SQL from named SQL + parameters. |

#### `org.jmouse.jdbc.statement`

| Type | Responsibility / contract |
|---|---|
| [AuditKeyCallback](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/AuditKeyCallback.java) | Implements/extends `KeyUpdateCallback`. Operations: `doStatementExecute`, `affectedRows`. |
| [BinderFactories](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/BinderFactories.java) | Operations: `tuple`, `columns`, `bean`, `map`, `bindString`, `bindInteger`, `bindObject`, `string`, `columnNames`, `parameterCount`, `binderFor`, `columnName`. |
| [BinderFactory](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/BinderFactory.java) | Operations: `binderFor`. |
| [Binders](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/Binders.java) | Operations: `empty`, `forEach`, `ofObjects`, `bindString`, `bindLong`, `bindInteger`, `bindBoolean`, `bindInstant`, `composeBinders`, `countedOf`, `bind`, `countOfParameters`. |
| [CallableCallback](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/CallableCallback.java) | Executes caller-defined work against a CallableStatement and returns a typed result while allowing SQLException propagation. |
| [CallableStatementBinder](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/CallableStatementBinder.java) | Binds parameters on a CallableStatement; provides NOOP when no binding is required. |
| [CountedStatementBinder](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/CountedStatementBinder.java) | Implements/extends `StatementBinder`. Operations: `bind`, `countOfParameters`, `toString`. |
| [InsertThenFetchCallback](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/InsertThenFetchCallback.java) | Implements/extends `KeyUpdateCallback`. Operations: `doStatementExecute`. |
| [KeyUpdateCallback](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/KeyUpdateCallback.java) | Produces a typed result from a PreparedStatement and its generated-keys ResultSet. |
| [ParameterBinder](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/ParameterBinder.java) | Operations: `nullBinder`, `return`. |
| [QueryStatementHandlerProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/QueryStatementHandlerProvider.java) | Provides a custom query StatementHandler for a SQL operation. |
| [StatementBinder](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/StatementBinder.java) | Operations: `bind`, `toString`. |
| [StatementCallback](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/StatementCallback.java) | PreparedStatement execution callback with predefined query, update and batch operations. |
| [StatementConfigurer](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/StatementConfigurer.java) | Functional contract for configuring a JDBC Statement prior to execution. |
| [StatementConfigurerProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/StatementConfigurerProvider.java) | Provides a custom StatementConfigurer for a SQL operation. |
| [StatementExecutor](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/StatementExecutor.java) | Generic execution function from a specific JDBC Statement subtype to a typed result, allowing SQLException propagation. |
| [StatementHandler](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/StatementHandler.java) | Interceptor-style hook for wrapping JDBC Statement execution. |
| [StatementOptions](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/StatementOptions.java) | Resolved statement execution options. |
| [StatementWarning](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/StatementWarning.java) | Value record: `(String message, String sqlState, SQLWarning warning)`. |
| [TupleBinder](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/TupleBinder.java) | Binds one typed input value onto a PreparedStatement, separating item-to-parameter logic from statement execution. |
| [UpdateStatementHandlerProvider](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/UpdateStatementHandlerProvider.java) | Provides a custom update StatementHandler for a SQL operation. |

#### `org.jmouse.jdbc.statement.handler`

| Type | Responsibility / contract |
|---|---|
| [FetchSizeStatementHandler](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/handler/FetchSizeStatementHandler.java) | Implements/extends `StatementHandler`. Operations: `handle`. |
| [JustTimingStatementHandler](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/handler/JustTimingStatementHandler.java) | ⏱️ Simple timing handler for JDBC statement execution. |
| [SlowQueryDetectorHandler](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/handler/SlowQueryDetectorHandler.java) | Implements/extends `StatementHandler`. Operations: `handle`. |
| [TimingSinkStatementHandler](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/handler/TimingSinkStatementHandler.java) | Implements/extends `StatementHandler`. Operations: `handle`, `Record`, `records`. |
| [TransactionDeadlineFailFastHandler](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/handler/TransactionDeadlineFailFastHandler.java) | Implements/extends `StatementHandler`. Operations: `handle`. |
| [UpdateCountSinkHandler](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/handler/UpdateCountSinkHandler.java) | Implements/extends `StatementHandler`. Operations: `handle`. |
| [WarningCollectorStatementHandler](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/statement/handler/WarningCollectorStatementHandler.java) | Implements/extends `StatementHandler`. Operations: `handle`. |

#### `org.jmouse.jdbc.transaction`

| Type | Responsibility / contract |
|---|---|
| [CompositeConnectionCustomizer](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/transaction/CompositeConnectionCustomizer.java) | Implements/extends `ConnectionCustomizer`. Operations: `apply`. |
| [ConnectionCustomizer](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/transaction/ConnectionCustomizer.java) | Applies TransactionDefinition settings to a Connection and returns a restoration action; restoration actions can be composed with guaranteed subsequent cleanup. |
| [ConnectionState](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/transaction/ConnectionState.java) | Value record: `(boolean autoCommit, boolean readOnly, int isolation)`. |
| [IsolationReadOnlyConnectionCustomizer](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/transaction/IsolationReadOnlyConnectionCustomizer.java) | Implements/extends `ConnectionCustomizer`. Operations: `apply`. |
| [JdbcIsolation](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/transaction/JdbcIsolation.java) | Operations: `toJdbc`, `isDefault`, `isNone`. |
| [JdbcResourceHolder](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/transaction/JdbcResourceHolder.java) | Operations: `getConnection`, `isSynchronized`, `setSynchronized`. |
| [JdbcTransactionCoordinator](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/transaction/JdbcTransactionCoordinator.java) | Implements/extends `AbstractTransactionCoordinator`. |
| [JdbcTransactionSession](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/transaction/JdbcTransactionSession.java) | JDBC-backed TransactionSession implementation. |
| [JdbcTransactionSessionFactory](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/transaction/JdbcTransactionSessionFactory.java) | Implements/extends `TransactionSessionFactory`. Operations: `openSession`. |

### Packaged resources

See [packaged resources](../jmouse-jdbc/src/main/resources/) for configuration/templates owned by this module.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [SmokeA.java](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/smoke/SmokeA.java).
- [SmokeB.java](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/smoke/SmokeB.java).
- [SmokeNamedQueryMySql.java](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/smoke/SmokeNamedQueryMySql.java).
- [SmokeSeedMySql.java](../jmouse-jdbc/src/main/java/org/jmouse/jdbc/smoke/SmokeSeedMySql.java).
