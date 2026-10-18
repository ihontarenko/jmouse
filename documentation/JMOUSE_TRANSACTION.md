# jmouse-transaction

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-transaction`. Build/dependency declaration: [pom.xml](../jmouse-transaction/pom.xml).

## What it is

Resource-independent transaction coordination with propagation, isolation definitions, callbacks, resource/context binding and synchronization hooks. Concrete resource sessions are supplied by integrations such as jmouse-jdbc.

The coordinator owns transaction lifecycle; TransactionSession performs resource operations; the holder binds state to the execution thread; synchronization callbacks observe completion. This module does not itself acquire a JDBC connection.

## The map

| Type | Responsibility |
| --- | --- |
| [TransactionManager](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionManager.java) | begin/commit/rollback coordination contract. |
| [TransactionDefinition](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionDefinition.java) | Requested propagation, isolation, read-only and timeout attributes. |
| [DefaultTransactionDefinition](../jmouse-transaction/src/main/java/org/jmouse/transaction/DefaultTransactionDefinition.java) | Mutable/configurable definition implementation. |
| [TransactionTemplate](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionTemplate.java) | Executes Work inside begin/commit and attempts rollback on failure. |
| [TransactionSession](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionSession.java) | Concrete resource transaction lifecycle contract. |
| [SavepointSupport](../jmouse-transaction/src/main/java/org/jmouse/transaction/SavepointSupport.java) | Optional capability required for nested savepoint transactions. |
| [AbstractTransactionCoordinator](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/AbstractTransactionCoordinator.java) | Shared propagation decisions, suspension, synchronization and cleanup. |
| [TransactionContextHolder](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/TransactionContextHolder.java) | Binds transaction state and associated resources. |
| [ThreadBoundTransactionContextHolder](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/thread/ThreadBoundTransactionContextHolder.java) | Thread-associated context/resource implementation. |
| [JoinTransactionValidator](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/JoinTransactionValidator.java) | Checks requested attributes against an existing transaction. |
| [TransactionSynchronization](../jmouse-transaction/src/main/java/org/jmouse/transaction/synchronization/TransactionSynchronization.java) | Callbacks around begin, commit, rollback and completion. |
| [CompositeTransactionCoordinator](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/CompositeTransactionCoordinator.java) | Sequentially delegates to multiple managers; does not implement distributed recovery. |

## Execution and ownership

1. Configure a TransactionSessionFactory, transaction/synchronization holders and join validation policy.
2. begin inspects the current context and requested propagation, then joins, suspends, rejects or opens a session.
3. New sessions are configured and begun; effective attributes/deadlines and synchronization context are bound.
4. commit/rollback run resource operations and synchronization callbacks; cleanup unbinds context, closes new sessions and resumes suspended resources where applicable.

## Public API and behavior

TransactionTemplate.inTransaction(definition, work) returns the work result after commit.

Template rollback failures are deliberately suppressed so the original thrown failure survives; checked failures are wrapped as RuntimeException.

MANDATORY without an existing transaction and NEVER with an existing transaction are rejected.

NESTED requires the session to implement SavepointSupport. Timeout deadlines are based on System.nanoTime.

## Wiring and extension points

- Implement a TransactionSession plus TransactionSessionFactory for another transactional resource.
- Subclass AbstractTransactionCoordinator to bind the concrete begin/commit/rollback and suspend/resume operations.
- Register TransactionSynchronization for completion behavior that should follow the transaction outcome.

## Pitfalls

- A thread-bound holder does not automatically propagate state to worker threads.
- Do not advertise CompositeTransactionCoordinator as XA or atomic distributed commit: it begins delegates sequentially and passes the returned status to each delegate.
- Nested/savepoint support depends on the actual session, not merely choosing a propagation enum.
- When changing propagation behavior, review participation, suspension and cleanup together; they share status/context ownership.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-transaction/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-transaction/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.jmouse:jmouse-beans` | compile | no |
| `jakarta.transaction:jakarta.transaction-api` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.transaction`

| Type | Responsibility / contract |
|---|---|
| [DefaultTransactionDefinition](../jmouse-transaction/src/main/java/org/jmouse/transaction/DefaultTransactionDefinition.java) | Implements/extends `TransactionDefinition`. Operations: `getPropagation`, `getIsolation`, `getTimeoutSeconds`, `isReadOnly`, `getName`, `builder`, `propagation`, `isolation`, `timeoutSeconds`, `readOnly`, `name`, `build`. |
| [SavepointSupport](../jmouse-transaction/src/main/java/org/jmouse/transaction/SavepointSupport.java) | Optional capability for savepoint-based transaction control. |
| [TransactionCallback](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionCallback.java) | Operations: `inTransaction`. |
| [TransactionDefinition](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionDefinition.java) | Describes transactional requirements. |
| [TransactionIsolation](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionIsolation.java) | Defines isolation level semantics for transactional execution. |
| [TransactionManager](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionManager.java) | Central transaction manager abstraction. |
| [TransactionOperation](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionOperation.java) | Operations: `required`, `requiresNew`. |
| [TransactionPropagation](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionPropagation.java) | Defines how transactional boundaries propagate when a transactional operation is invoked. |
| [TransactionSession](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionSession.java) | Represents a low-level transactional session bound to a resource. |
| [TransactionStatus](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionStatus.java) | Represents the runtime status of an active transaction. |
| [TransactionTemplate](../jmouse-transaction/src/main/java/org/jmouse/transaction/TransactionTemplate.java) | TransactionTemplate is a programmatic transaction boundary helper. |

#### `org.jmouse.transaction.configuration`

| Type | Responsibility / contract |
|---|---|
| [TransactionInfrastructureConfiguration](../jmouse-transaction/src/main/java/org/jmouse/transaction/configuration/TransactionInfrastructureConfiguration.java) | Operations: `transactionContextHolder`, `synchronizationContextHolder`, `joinTransactionValidator`. |
| [TransactionTemplateConfiguration](../jmouse-transaction/src/main/java/org/jmouse/transaction/configuration/TransactionTemplateConfiguration.java) | Operations: `txCallback`, `txOperations`, `txDefinitionDefaults`. |

#### `org.jmouse.transaction.infrastructure`

| Type | Responsibility / contract |
|---|---|
| [AbstractTransactionCoordinator](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/AbstractTransactionCoordinator.java) | Template-based transaction coordinator. |
| [CompositeTransactionCoordinator](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/CompositeTransactionCoordinator.java) | Implements/extends `TransactionManager`. Operations: `begin`, `commit`, `rollback`. |
| [JoinAttributePolicy](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/JoinAttributePolicy.java) | Enum values: `IGNORE`, `WARNING`, `FAIL`. |
| [JoinTransactionValidator](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/JoinTransactionValidator.java) | Operations: `validate`. |
| [MutableTransactionContext](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/MutableTransactionContext.java) | Implements/extends `TransactionContext`. Operations: `getDefinition`, `getStatus`, `getSession`, `getSavepoint`, `isRollbackOnly`, `setRollbackOnly`, `setSavepoint`, `clearSavepoint`, `getEffectiveTimeoutSeconds`, `setEffectiveTimeoutSeconds`, `getAttributes`, `setAttributes`. |
| [NoopTransactionStatus](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/NoopTransactionStatus.java) | Implements/extends `TransactionStatus`. Operations: `isNew`, `isRollbackOnly`, `markRollbackOnly`, `isCompleted`, `markCompleted`. |
| [SuspendedResources](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/SuspendedResources.java) | Value record: `( TransactionContext transactionContext, SynchronizationContext synchronizationContext, Map , Object> resourceSnapshot )`. |
| [TransactionAttributes](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/TransactionAttributes.java) | Value record: `(TransactionIsolation isolation, boolean readOnly, long deadlineNanos)`. |
| [TransactionContext](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/TransactionContext.java) | Holds transactional state for the current execution scope. |
| [TransactionContextHolder](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/TransactionContextHolder.java) | Strategy for binding transaction context to an execution scope. |
| [TransactionParticipation](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/TransactionParticipation.java) | Describes transaction participation semantics. |
| [TransactionParticipationContext](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/TransactionParticipationContext.java) | Result of propagation resolution. |
| [TransactionSessionFactory](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/TransactionSessionFactory.java) | Factory responsible for creating transactional sessions. |
| [TransactionStatusSupport](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/TransactionStatusSupport.java) | Implements/extends `TransactionStatus`. Operations: `isNew`, `isRollbackOnly`, `markRollbackOnly`, `isCompleted`, `markCompleted`, `hasSuspended`, `getSuspended`, `setSuspended`. |

#### `org.jmouse.transaction.infrastructure.support`

| Type | Responsibility / contract |
|---|---|
| [TransactionContextAccessSupport](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/support/TransactionContextAccessSupport.java) | Static access point for the active TransactionContextHolder. |

#### `org.jmouse.transaction.infrastructure.thread`

| Type | Responsibility / contract |
|---|---|
| [ThreadBoundTransactionContextHolder](../jmouse-transaction/src/main/java/org/jmouse/transaction/infrastructure/thread/ThreadBoundTransactionContextHolder.java) | Thread-bound implementation of TransactionContextHolder. |

#### `org.jmouse.transaction.synchronization`

| Type | Responsibility / contract |
|---|---|
| [DefaultSynchronizationContext](../jmouse-transaction/src/main/java/org/jmouse/transaction/synchronization/DefaultSynchronizationContext.java) | Implements/extends `SynchronizationContext`. Operations: `getRegistry`. |
| [DefaultSynchronizationRegistry](../jmouse-transaction/src/main/java/org/jmouse/transaction/synchronization/DefaultSynchronizationRegistry.java) | Implements/extends `SynchronizationRegistry`. Operations: `register`, `getSynchronizations`, `clear`. |
| [SynchronizationAwareTransactionContext](../jmouse-transaction/src/main/java/org/jmouse/transaction/synchronization/SynchronizationAwareTransactionContext.java) | Empty synchronization-aware context placeholder; currently declares no state or synchronization behavior. |
| [SynchronizationContext](../jmouse-transaction/src/main/java/org/jmouse/transaction/synchronization/SynchronizationContext.java) | Holds synchronizations bound to a transaction scope. |
| [SynchronizationContextHolder](../jmouse-transaction/src/main/java/org/jmouse/transaction/synchronization/SynchronizationContextHolder.java) | Strategy for binding synchronization context. |
| [SynchronizationRegistry](../jmouse-transaction/src/main/java/org/jmouse/transaction/synchronization/SynchronizationRegistry.java) | Registry for transaction synchronizations. |
| [SynchronizationSupport](../jmouse-transaction/src/main/java/org/jmouse/transaction/synchronization/SynchronizationSupport.java) | Coordinator base helper invoking registered before-begin, before/after-commit, after-rollback and completion callbacks when a synchronization context exists. |
| [ThreadBoundSynchronizationContextHolder](../jmouse-transaction/src/main/java/org/jmouse/transaction/synchronization/ThreadBoundSynchronizationContextHolder.java) | Thread-bound implementation of SynchronizationContextHolder. |
| [TransactionSynchronization](../jmouse-transaction/src/main/java/org/jmouse/transaction/synchronization/TransactionSynchronization.java) | Callback interface for transaction lifecycle events. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
