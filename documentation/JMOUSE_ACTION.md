# jmouse-action

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-action`. Build/dependency declaration: [pom.xml](../jmouse-action/pom.xml).

## What it is

A named action registry and execution adapter. An action can be a typed handler or an annotated Java method; descriptors normalize those forms so the executor can resolve a name and execute it with a request/context.

The library owns registration, adaptation and invocation. Product-specific commands and their consequences belong to action handlers. The EL and mapper adapters translate action definitions into this shared invocation model.

## The map

| Type | Responsibility |
| --- | --- |
| [ActionDefinition](../jmouse-action/src/main/java/org/jmouse/action/ActionDefinition.java) | Declarative name/argument description for an action invocation. |
| [ActionRequest](../jmouse-action/src/main/java/org/jmouse/action/ActionRequest.java) | Runtime action request used by the executor and handler adapters. |
| [ActionDescriptor](../jmouse-action/src/main/java/org/jmouse/action/ActionDescriptor.java) | Registered action identity and handler metadata. |
| [ActionRegistry](../jmouse-action/src/main/java/org/jmouse/action/ActionRegistry.java) | Read-side lookup contract for named descriptors. |
| [SimpleActionRegistry](../jmouse-action/src/main/java/org/jmouse/action/SimpleActionRegistry.java) | Concurrent in-memory descriptor registry with duplicate and missing-name errors. |
| [ActionExecutor](../jmouse-action/src/main/java/org/jmouse/action/ActionExecutor.java) | Execution contract; nested Default resolves descriptors and calls their handler. |
| [ActionHandler](../jmouse-action/src/main/java/org/jmouse/action/ActionHandler.java) | Normalized executable action contract. |
| [TypedAction](../jmouse-action/src/main/java/org/jmouse/action/TypedAction.java) | Typed action contract for a result-bearing action. |
| [MethodActionHandlerAdapter](../jmouse-action/src/main/java/org/jmouse/action/MethodActionHandlerAdapter.java) | Adapts a reflected method into the handler interface. |
| [ActionAnnotationProcessor](../jmouse-action/src/main/java/org/jmouse/action/support/ActionAnnotationProcessor.java) | Discovers annotated action methods and registers adapted descriptors. |
| [ActionExpressionAdapter](../jmouse-action/src/main/java/org/jmouse/action/adapter/el/ActionExpressionAdapter.java) | Connects expression-language action nodes to action execution. |
| [ActionDefinitionMapper](../jmouse-action/src/main/java/org/jmouse/action/adapter/mapper/ActionDefinitionMapper.java) | Mapper adapter for materializing action definitions. |

## Execution and ownership

1. Create a configurable registry and register ActionDescriptor instances directly or through annotation processing.
2. Construct ActionExecutor.Default with the registry; keep the registry and executor reusable.
3. Supply an ActionRequest or an ActionDefinition together with its Context.
4. The executor resolves the registered handler and invokes it. Existing ActionException failures propagate unchanged; other Exceptions are wrapped in ActionExecutionException.

## Public API and behavior

SimpleActionRegistry.register refuses an already registered name rather than replacing its handler.

getDescriptor throws ActionNotFoundException when the requested name is absent; contains supplies a read-side existence check.

ActionExecutionContext extends the core variables context for action-specific values.

ActionRequestMethodArgumentResolver and MappedActionArgumentResolver connect invocation arguments to the method-invocation infrastructure.

## Wiring and extension points

- Implement ActionHandler or TypedAction for custom actions.
- Use @Action and ActionAnnotationProcessor when a bean method is the natural action boundary.
- Configure the EL action extension only when action definitions are actually authored as expressions.

## Pitfalls

- Treat the registered name as an API: a duplicate registration is an error and a rename invalidates existing definitions.
- Registration does not itself execute an action; handlers run only through an execution request.
- Keep request/context state local to an invocation; a concurrent registry does not make the supplied context thread-safe.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-action/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Existing executable examples/smokes: [SmokeA](../jmouse-action/src/main/java/org/jmouse/action/smoke/SmokeA.java).

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-action/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.jmouse:jmouse-mapper` | compile | no |
| `org.jmouse:jmouse-el` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.action`

| Type | Responsibility / contract |
|---|---|
| [ActionDefinition](../jmouse-action/src/main/java/org/jmouse/action/ActionDefinition.java) | Declarative action description. 🧩 |
| [ActionDescriptor](../jmouse-action/src/main/java/org/jmouse/action/ActionDescriptor.java) | Describes a registered action. 🧩 |
| [ActionException](../jmouse-action/src/main/java/org/jmouse/action/ActionException.java) | Base action exception. ⚠️ |
| [ActionExecutionContext](../jmouse-action/src/main/java/org/jmouse/action/ActionExecutionContext.java) | Implements/extends `AbstractVariablesContext`. |
| [ActionExecutionException](../jmouse-action/src/main/java/org/jmouse/action/ActionExecutionException.java) | Thrown when action execution fails. 💥 |
| [ActionExecutor](../jmouse-action/src/main/java/org/jmouse/action/ActionExecutor.java) | Executes actions. ▶️ |
| [ActionExecutorSingleton](../jmouse-action/src/main/java/org/jmouse/action/ActionExecutorSingleton.java) | Operations: `getActionExecutor`, `getActionExpressionAdapter`, `getMethodInvoker`. |
| [ActionHandler](../jmouse-action/src/main/java/org/jmouse/action/ActionHandler.java) | Handles a single action request. 🎯 |
| [ActionInvocationRequest](../jmouse-action/src/main/java/org/jmouse/action/ActionInvocationRequest.java) | InvocationRequest specialized for action execution. ⚙️ |
| [ActionNotFoundException](../jmouse-action/src/main/java/org/jmouse/action/ActionNotFoundException.java) | Thrown when action handler cannot be resolved. 🔎 |
| [ActionRegistrationException](../jmouse-action/src/main/java/org/jmouse/action/ActionRegistrationException.java) | Thrown when action registration fails. 🚫 |
| [ActionRegistry](../jmouse-action/src/main/java/org/jmouse/action/ActionRegistry.java) | Registry for action handlers. 🗂️ |
| [ActionRequest](../jmouse-action/src/main/java/org/jmouse/action/ActionRequest.java) | Runtime action request. ⚙️ |
| [ActionRequestMethodArgumentResolver](../jmouse-action/src/main/java/org/jmouse/action/ActionRequestMethodArgumentResolver.java) | MethodArgumentResolver for ActionRequest parameters. 📩 |
| [ConfigurableActionRegistry](../jmouse-action/src/main/java/org/jmouse/action/ConfigurableActionRegistry.java) | Configurable wrapper for ActionRegistry. ⚙️ |
| [MappedActionArgumentResolver](../jmouse-action/src/main/java/org/jmouse/action/MappedActionArgumentResolver.java) | MethodArgumentResolver that maps action definition data to method argument objects. 🔄 |
| [MethodActionHandlerAdapter](../jmouse-action/src/main/java/org/jmouse/action/MethodActionHandlerAdapter.java) | ActionHandler adapter for method-backed actions. 🎯 |
| [SimpleActionRegistry](../jmouse-action/src/main/java/org/jmouse/action/SimpleActionRegistry.java) | Default in-memory implementation of ActionRegistry. 🧱 |
| [TypedAction](../jmouse-action/src/main/java/org/jmouse/action/TypedAction.java) | Typed action contract. 🎯 |
| [TypedActionHandlerAdapter](../jmouse-action/src/main/java/org/jmouse/action/TypedActionHandlerAdapter.java) | ActionHandler implementation that bridges declarative ActionDefinition and concrete TypedAction classes. |

#### `org.jmouse.action.adapter.el`

| Type | Responsibility / contract |
|---|---|
| [ActionDefinitionNode](../jmouse-action/src/main/java/org/jmouse/action/adapter/el/ActionDefinitionNode.java) | AST node representing an action definition expression. ⚙️ |
| [ActionDefinitionParser](../jmouse-action/src/main/java/org/jmouse/action/adapter/el/ActionDefinitionParser.java) | Parses action definitions like @[namespace:name]{...}. ⚙️ |
| [ActionExpressionAdapter](../jmouse-action/src/main/java/org/jmouse/action/adapter/el/ActionExpressionAdapter.java) | Adapter that executes actions defined as EL expressions. ⚙️ |
| [ActionExpressionConfigurer](../jmouse-action/src/main/java/org/jmouse/action/adapter/el/ActionExpressionConfigurer.java) | Registers EL extensions required for action expressions. 🧩 |

#### `org.jmouse.action.adapter.mapper`

| Type | Responsibility / contract |
|---|---|
| [ActionDefinitionMapper](../jmouse-action/src/main/java/org/jmouse/action/adapter/mapper/ActionDefinitionMapper.java) | Maps ActionDefinition argument maps into typed action objects. ⚙️ |

#### `org.jmouse.action.annotation`

| Type | Responsibility / contract |
|---|---|
| [Action](../jmouse-action/src/main/java/org/jmouse/action/annotation/Action.java) | Declares a method as an action handler. 🏷️ |

#### `org.jmouse.action.support`

| Type | Responsibility / contract |
|---|---|
| [ActionAnnotationProcessor](../jmouse-action/src/main/java/org/jmouse/action/support/ActionAnnotationProcessor.java) | org.jmouse.core.annotation.AnnotationProcessor that registers Action-annotated methods in the ActionRegistry. 🚀 |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [SmokeA.java](../jmouse-action/src/main/java/org/jmouse/action/smoke/SmokeA.java).
- [SourceTarget.java](../jmouse-action/src/main/java/org/jmouse/action/smoke/SourceTarget.java).
