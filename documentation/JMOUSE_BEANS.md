# jmouse-beans

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-beans`. Build/dependency declaration: [pom.xml](../jmouse-beans/pom.xml).

## What it is

The jMouse dependency injection container: bean definitions, scanning, dependency resolution, scopes, instantiation and initialization. It can be used directly without the application-level property/resource conventions added by jmouse-context.

Bean definitions describe how objects are created; containers hold scoped instances; resolution chooses candidates; post-processors initialize/adapt created objects. Preserve these boundaries when adding a new creation form or injection rule.

## The map

| Type | Responsibility |
| --- | --- |
| [BeanContext](../jmouse-beans/src/main/java/org/jmouse/beans/BeanContext.java) | Public container interface for registration, lookup, hierarchy and lifecycle collaborators. |
| [DefaultBeanContext](../jmouse-beans/src/main/java/org/jmouse/beans/DefaultBeanContext.java) | Coordinates definitions, scoped instances, cycle detection, initializers and post-processing. |
| [DefaultBeanFactory](../jmouse-beans/src/main/java/org/jmouse/beans/DefaultBeanFactory.java) | Delegates construction to the first applicable instantiation strategy. |
| [BeanDefinition](../jmouse-beans/src/main/java/org/jmouse/beans/definition/BeanDefinition.java) | Metadata for bean type, name, scope and creation form. |
| [SimpleBeanDefinitionFactory](../jmouse-beans/src/main/java/org/jmouse/beans/definition/SimpleBeanDefinitionFactory.java) | Creates definitions through registered definition-creation strategies. |
| [SimpleDependencyResolver](../jmouse-beans/src/main/java/org/jmouse/beans/instantiation/SimpleDependencyResolver.java) | Builds dependency values using the bean-resolution strategy infrastructure. |
| [BeanResolutionRequest](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/BeanResolutionRequest.java) | Carries the requested dependency type and associated resolution metadata. |
| [BeanPostProcessor](../jmouse-beans/src/main/java/org/jmouse/beans/processor/BeanPostProcessor.java) | Lifecycle seam for adapting and initializing newly created beans. |
| [BeanContextInitializer](../jmouse-beans/src/main/java/org/jmouse/beans/BeanContextInitializer.java) | Ordered context-wide setup/scanning seam. |
| [SingletonBeanContainer](../jmouse-beans/src/main/java/org/jmouse/beans/SingletonBeanContainer.java) | Scope container reusing one instance per bean name. |
| [PrototypeBeanContainer](../jmouse-beans/src/main/java/org/jmouse/beans/PrototypeBeanContainer.java) | Scope container creating an instance when requested. |
| [BeanScanner](../jmouse-beans/src/main/java/org/jmouse/beans/BeanScanner.java) | Discovers and contributes bean definitions. |

## Execution and ownership

1. Construct DefaultBeanContext with base classes and optionally a parent context.
2. Configure initializers, definitions, scanners, scope containers and post-processors before refresh.
3. refresh orders initializers and runs each initializer not already recorded as initialized.
4. A bean lookup resolves a definition and scope, creates the bean when necessary, performs initialization/post-processing and stores it according to its scope.

## Public API and behavior

getBean has type, name and combined overloads. Missing beans and ambiguous type lookup are separate errors.

registerBean supports already constructed objects or object factories; registerDefinition contributes a creation recipe.

initializeBean performs initialization for a created instance, including annotated initialization methods.

cleanup currently clears initializer tracking. It does not clear bean instances or destroy arbitrary user resources.

## Wiring and extension points

- Add BeanDefinitionCreationStrategy plus BeanInstantiationStrategy for another kind of creation recipe.
- Use BeanResolver strategies for optional, provider, collection, map, qualified and primary candidate handling.
- Register a BeanContainer for a custom Scope; unsupported scopes fail instead of silently behaving as singletons.
- Use BeanPostProcessor for injection/adaptation; context initializers are for container setup.

## Pitfalls

- A BeanDefinition is not the scoped bean instance. Replacing a definition and replacing an existing scoped instance are different operations.
- Type lookup can be ambiguous; define names/qualifiers/primary intent rather than relying on scanning order.
- Context hierarchy affects lookup visibility. Check the configured lookup strategy when a parent bean shadows or contributes candidates.
- Do not assume refresh recreates everything: initialized initializers are tracked, and repeat initialization is explicitly guarded.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-beans/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-beans/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `jakarta.inject:jakarta.inject-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.beans`

| Type | Responsibility / contract |
|---|---|
| [BeanAmbiguousException](../jmouse-beans/src/main/java/org/jmouse/beans/BeanAmbiguousException.java) | Implements/extends `RuntimeException`. |
| [BeanContainer](../jmouse-beans/src/main/java/org/jmouse/beans/BeanContainer.java) | Represents a container for beans. Provides methods to retrieve, register, and manage beans within the container. |
| [BeanContainerRegistry](../jmouse-beans/src/main/java/org/jmouse/beans/BeanContainerRegistry.java) | Interface for managing the registry of BeanContainers across different Scopes. |
| [BeanContext](../jmouse-beans/src/main/java/org/jmouse/beans/BeanContext.java) | 🎯 Represents the central parser and registry for bean lifecycle in the application context. |
| [BeanContextAware](../jmouse-beans/src/main/java/org/jmouse/beans/BeanContextAware.java) | Interface for components that are aware of their BeanContext. Provides methods to set and retrieve the BeanContext. |
| [BeanContextException](../jmouse-beans/src/main/java/org/jmouse/beans/BeanContextException.java) | An exception indicating errors or abnormal conditions encountered within a bean context. |
| [BeanContextInitializer](../jmouse-beans/src/main/java/org/jmouse/beans/BeanContextInitializer.java) | Interface for initializing a BeanContext. Implementations of this interface are responsible for configuring and preparing the BeanContext before it is used. |
| [BeanDefinitionContainer](../jmouse-beans/src/main/java/org/jmouse/beans/BeanDefinitionContainer.java) | An interface for managing BeanDefinition objects within a container. |
| [BeanDefinitionContainerRegistry](../jmouse-beans/src/main/java/org/jmouse/beans/BeanDefinitionContainerRegistry.java) | Interface for managing access to a BeanDefinitionContainer. |
| [BeanDefinitionMatchers](../jmouse-beans/src/main/java/org/jmouse/beans/BeanDefinitionMatchers.java) | Utility class providing predefined Matchers for filtering BeanDefinitions. |
| [BeanDefinitionScopeResolver](../jmouse-beans/src/main/java/org/jmouse/beans/BeanDefinitionScopeResolver.java) | Resolves the Scope of a bean based on its BeanDefinition. |
| [BeanFactory](../jmouse-beans/src/main/java/org/jmouse/beans/BeanFactory.java) | Interface representing a factory for creating bean instances. |
| [BeanInitializationException](../jmouse-beans/src/main/java/org/jmouse/beans/BeanInitializationException.java) | Implements/extends `RuntimeException`. |
| [BeanInitializer](../jmouse-beans/src/main/java/org/jmouse/beans/BeanInitializer.java) | Interface for initializing beans during their lifecycle. |
| [BeanInstantiationException](../jmouse-beans/src/main/java/org/jmouse/beans/BeanInstantiationException.java) | Implements/extends `RuntimeException`. |
| [BeanInstantiationType](../jmouse-beans/src/main/java/org/jmouse/beans/BeanInstantiationType.java) | Enumeration representing the types of bean instantiation strategies. Defines how a bean instance can be created in the container. |
| [BeanLifecycleException](../jmouse-beans/src/main/java/org/jmouse/beans/BeanLifecycleException.java) | Implements/extends `RuntimeException`. |
| [BeanLookupStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/BeanLookupStrategy.java) | Enum values: `DELEGATE_TO_PARENT`, `INHERIT_DEFINITION`. |
| [BeanNameException](../jmouse-beans/src/main/java/org/jmouse/beans/BeanNameException.java) | Implements/extends `RuntimeException`. |
| [BeanNameKeeper](../jmouse-beans/src/main/java/org/jmouse/beans/BeanNameKeeper.java) | Operations: `setBeanName`. |
| [BeanNotFoundException](../jmouse-beans/src/main/java/org/jmouse/beans/BeanNotFoundException.java) | An exception indicating error that bean can be found in any contained or bean is unknown at all |
| [BeanProvider](../jmouse-beans/src/main/java/org/jmouse/beans/BeanProvider.java) | Operations: `getBean`. |
| [Beans](../jmouse-beans/src/main/java/org/jmouse/beans/Beans.java) | Operations: `getBeans`, `get`. |
| [BeanScanAnnotatedContextInitializer](../jmouse-beans/src/main/java/org/jmouse/beans/BeanScanAnnotatedContextInitializer.java) | 🔁 Initializes the context with additional scan targets from BeanScan. |
| [BeanScanner](../jmouse-beans/src/main/java/org/jmouse/beans/BeanScanner.java) | 🔍 Contract for scanning classes and extracting annotated elements. |
| [BeanScope](../jmouse-beans/src/main/java/org/jmouse/beans/BeanScope.java) | Enum representing the lifecycle of a bean in the context. Defines various scopes for managing bean instances. |
| [BeansScannerBeanContextInitializer](../jmouse-beans/src/main/java/org/jmouse/beans/BeansScannerBeanContextInitializer.java) | 🧪 Initializes bean context by scanning common bean annotations. |
| [BeanWarmer](../jmouse-beans/src/main/java/org/jmouse/beans/BeanWarmer.java) | Operations: `warmup`. |
| [ConcurrentHashMapBeanContainerRegistry](../jmouse-beans/src/main/java/org/jmouse/beans/ConcurrentHashMapBeanContainerRegistry.java) | A simple implementation of the BeanContainerRegistry interface. |
| [DefaultBeanContext](../jmouse-beans/src/main/java/org/jmouse/beans/DefaultBeanContext.java) | DirectAccess implementation of the BeanContext interface. |
| [DefaultBeanDefinitionContainer](../jmouse-beans/src/main/java/org/jmouse/beans/DefaultBeanDefinitionContainer.java) | A simple implementation of the BeanDefinitionContainer interface. |
| [DefaultBeanFactory](../jmouse-beans/src/main/java/org/jmouse/beans/DefaultBeanFactory.java) | The default implementation of BeanFactory and BeanInstantiationFactory. Provides logic for instantiating beans using registered BeanInstantiationStrategy instances. |
| [DelegateBeanContainer](../jmouse-beans/src/main/java/org/jmouse/beans/DelegateBeanContainer.java) | A bean container implementation that delegates all operations to another BeanContainer. |
| [DelegateBeanContainerRegistry](../jmouse-beans/src/main/java/org/jmouse/beans/DelegateBeanContainerRegistry.java) | A BeanContainerRegistry implementation that delegates all operations to another registry. |
| [EagerBean](../jmouse-beans/src/main/java/org/jmouse/beans/EagerBean.java) | Marker interface for eager beans. ⚡ |
| [EventBridgeContextInitializer](../jmouse-beans/src/main/java/org/jmouse/beans/EventBridgeContextInitializer.java) | 🔌 Bridges EventListener beans into the org.jmouse.core.events.EventManager owned by a BeanContext that supports BeanContextEventSupport. |
| [Globals](../jmouse-beans/src/main/java/org/jmouse/beans/Globals.java) | Operations: `set`, `get`. |
| [GlobalsBeanContainerRegistry](../jmouse-beans/src/main/java/org/jmouse/beans/GlobalsBeanContainerRegistry.java) | A parser implementation of BeanContainerRegistry that dynamically resolves and delegates to scoped BeanContainerRegistry instances. |
| [InitializingBean](../jmouse-beans/src/main/java/org/jmouse/beans/InitializingBean.java) | Callback interface for post-initialization logic. 🔄 |
| [InitializingBeanSupport](../jmouse-beans/src/main/java/org/jmouse/beans/InitializingBeanSupport.java) | 🔧 Extended InitializingBean with typed context support. |
| [InstantiationType](../jmouse-beans/src/main/java/org/jmouse/beans/InstantiationType.java) | Marker for bean instantiation strategy. 🧩 |
| [ObjectFactory](../jmouse-beans/src/main/java/org/jmouse/beans/ObjectFactory.java) | A functional interface representing a factory for creating objects. |
| [ParentLookup](../jmouse-beans/src/main/java/org/jmouse/beans/ParentLookup.java) | Enum values: `ENABLE`, `DISABLE`. |
| [PrototypeBeanContainer](../jmouse-beans/src/main/java/org/jmouse/beans/PrototypeBeanContainer.java) | A dummy container implementation of BeanContainer for prototype-scoped beans. |
| [ProxyFactoryContextInitializer](../jmouse-beans/src/main/java/org/jmouse/beans/ProxyFactoryContextInitializer.java) | 🏗️ BeanContextInitializer that sets up the proxy infrastructure. |
| [ScannerBeanContextInitializer](../jmouse-beans/src/main/java/org/jmouse/beans/ScannerBeanContextInitializer.java) | This class is responsible for initializing a BeanContext by scanning specified base classes for annotated elements using registered BeanScanners. |
| [Scope](../jmouse-beans/src/main/java/org/jmouse/beans/Scope.java) | Interface representing a generic scope for managing bean lifecycles in the container. |
| [ScopedBeanContainer](../jmouse-beans/src/main/java/org/jmouse/beans/ScopedBeanContainer.java) | A composite interface that combines the functionalities of BeanContainer and BeanContainerRegistry. |
| [ScopedHashMapBeanContainer](../jmouse-beans/src/main/java/org/jmouse/beans/ScopedHashMapBeanContainer.java) | A default implementation of the ScopedBeanContainer that integrates with ScopeResolver. |
| [ScopeResolver](../jmouse-beans/src/main/java/org/jmouse/beans/ScopeResolver.java) | Functional interface for resolving a Scope based on a given bean name. |
| [SingletonBeanContainer](../jmouse-beans/src/main/java/org/jmouse/beans/SingletonBeanContainer.java) | Implements/extends `BeanContainer`. Operations: `getBean`, `registerBean`, `containsBean`, `toString`. |
| [UnsupportedScopeException](../jmouse-beans/src/main/java/org/jmouse/beans/UnsupportedScopeException.java) | Exception thrown when an unsupported Scope is encountered in a bean context or container. |
| [WarmupEagerBeansContextInitializer](../jmouse-beans/src/main/java/org/jmouse/beans/WarmupEagerBeansContextInitializer.java) | Initializes eager beans during context startup. 🔥 |

#### `org.jmouse.beans.annotation`

| Type | Responsibility / contract |
|---|---|
| [AggregatedBeans](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/AggregatedBeans.java) | 🔗 Injects all beans of the target type. |
| [Bean](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/Bean.java) | Annotation to mark a class or method as a bean valueProvider for dependency injection. |
| [BeanConstructor](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/BeanConstructor.java) | 🏗️ Marks the constructor to use for bean instantiation. |
| [BeanFactories](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/BeanFactories.java) | 🏭 Marks a class that provides one or more bean factories. |
| [BeanImport](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/BeanImport.java) | 📦 Imports other bean classes into the current context. |
| [BeanInitializer](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/BeanInitializer.java) | Indicates a method that should be called after a bean is constructed, typically for performing custom initialization logic. |
| [BeanName](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/BeanName.java) | 🏷️ Assigns a custom name to a bean. |
| [BeanScan](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/BeanScan.java) | 🔍 Triggers additional bean scanning on specified classes/packages. |
| [Dependency](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/Dependency.java) | Indicates that a field or method is a dependency that should be injected by the container. |
| [Eager](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/Eager.java) | Marks a bean for eager initialization. ⚡ |
| [Ignore](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/Ignore.java) | 🚫 Excludes a class or method from bean registration. |
| [Lazy](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/Lazy.java) | Annotation contract; members and defaults are defined in this source. |
| [PrimaryBean](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/PrimaryBean.java) | ⭐ Marks a bean as primary when multiple candidates are present. |
| [ProxiedBean](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/ProxiedBean.java) | 🪞 Marks a bean (class or factory method) as proxied. |
| [Qualifier](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/Qualifier.java) | 🏷️ Qualifies injection by name when multiple candidates exist. |
| [SkipBean](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/SkipBean.java) | Annotation contract; members and defaults are defined in this source. |
| [SuppressException](../jmouse-beans/src/main/java/org/jmouse/beans/annotation/SuppressException.java) | 🚫 Suppress specified exceptions on the annotated class. |

#### `org.jmouse.beans.conditions`

| Type | Responsibility / contract |
|---|---|
| [AbstractBeanRegistrationCondition](../jmouse-beans/src/main/java/org/jmouse/beans/conditions/AbstractBeanRegistrationCondition.java) | 🧩 Base class for bean registration conditions. |
| [BeanCondition](../jmouse-beans/src/main/java/org/jmouse/beans/conditions/BeanCondition.java) | Register bean only if the given condition(s) match. |
| [BeanRegistrationCondition](../jmouse-beans/src/main/java/org/jmouse/beans/conditions/BeanRegistrationCondition.java) | Base contract for evaluating conditions before bean registration. |
| [ConditionalMetadata](../jmouse-beans/src/main/java/org/jmouse/beans/conditions/ConditionalMetadata.java) | 📌 Metadata describing conditional context for a bean. |
| [ConditionEvaluator](../jmouse-beans/src/main/java/org/jmouse/beans/conditions/ConditionEvaluator.java) | ✅ Evaluates whether a BeanDefinition satisfies all registered BeanConditions. |
| [SimpleConditionalMetadata](../jmouse-beans/src/main/java/org/jmouse/beans/conditions/SimpleConditionalMetadata.java) | 📦 Simple conditional metadata for a single merged annotation. |

#### `org.jmouse.beans.container`

| Type | Responsibility / contract |
|---|---|
| [ThreadLocalBeanContainer](../jmouse-beans/src/main/java/org/jmouse/beans/container/ThreadLocalBeanContainer.java) | A BeanContainer implementation that manages beans in a thread-local scope. |
| [ThreadLocalScope](../jmouse-beans/src/main/java/org/jmouse/beans/container/ThreadLocalScope.java) | Enum representing a thread-local Scope. |

#### `org.jmouse.beans.definition`

| Type | Responsibility / contract |
|---|---|
| [AbstractBeanDefinition](../jmouse-beans/src/main/java/org/jmouse/beans/definition/AbstractBeanDefinition.java) | An abstract base class for BeanDefinition implementations. Manages common bean descriptor such as dependencies, lifecycle, annotations, and more. |
| [AggregatedBeansDependency](../jmouse-beans/src/main/java/org/jmouse/beans/definition/AggregatedBeansDependency.java) | Value record: `( InferredType javaType, String name, AnnotatedElement dependant) implements BeanDependency`. |
| [BeanDefinition](../jmouse-beans/src/main/java/org/jmouse/beans/definition/BeanDefinition.java) | Describes a bean's descriptor and lifecycle within the container, including its name, class, creation strategy, dependencies, and annotations. |
| [BeanDefinitionException](../jmouse-beans/src/main/java/org/jmouse/beans/definition/BeanDefinitionException.java) | Exception thrown to indicate an error related to a BeanDefinition. |
| [BeanDefinitionFactory](../jmouse-beans/src/main/java/org/jmouse/beans/definition/BeanDefinitionFactory.java) | Factory interface for creating BeanDefinition instances. This interface defines the contract for managing the creation of bean definitions and supports a pluggable strategy-based approach for custom definition creation logic. |
| [BeanDependency](../jmouse-beans/src/main/java/org/jmouse/beans/definition/BeanDependency.java) | Represents a dependency required by a bean. |
| [ConstructorBeanDefinition](../jmouse-beans/src/main/java/org/jmouse/beans/definition/ConstructorBeanDefinition.java) | 🧱 Bean definition based on constructor instantiation. |
| [DuplicateBeanDefinitionException](../jmouse-beans/src/main/java/org/jmouse/beans/definition/DuplicateBeanDefinitionException.java) | Implements/extends `RuntimeException`. |
| [MethodBeanDefinition](../jmouse-beans/src/main/java/org/jmouse/beans/definition/MethodBeanDefinition.java) | A BeanDefinition implementation for beans created using a factory method. Stores descriptor about the factory method and its associated bean. |
| [ObjectFactoryBeanDefinition](../jmouse-beans/src/main/java/org/jmouse/beans/definition/ObjectFactoryBeanDefinition.java) | A BeanDefinition implementation for beans created using an ObjectFactory. Stores the factory and descriptor about the bean. |
| [SimpleBeanDefinitionFactory](../jmouse-beans/src/main/java/org/jmouse/beans/definition/SimpleBeanDefinitionFactory.java) | A simple implementation of the BeanDefinitionFactory that uses a list of strategies to create bean definitions. |
| [SimpleBeanDependency](../jmouse-beans/src/main/java/org/jmouse/beans/definition/SimpleBeanDependency.java) | A simple implementation of the BeanDependency interface. Represents a dependency required by a bean, defined by its type and optional name. |

#### `org.jmouse.beans.definition.strategy`

| Type | Responsibility / contract |
|---|---|
| [AbstractBeanDefinitionCreationStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/definition/strategy/AbstractBeanDefinitionCreationStrategy.java) | An abstract base class for creating BeanDefinition objects from annotated elements. |
| [BeanDefinitionCreationStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/definition/strategy/BeanDefinitionCreationStrategy.java) | Strategy interface for creating BeanDefinition objects from various elements. |
| [ConstructorBeanDefinitionCreationStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/definition/strategy/ConstructorBeanDefinitionCreationStrategy.java) | A strategy for creating ConstructorBeanDefinition instances from classes. |
| [MethodBeanDefinitionCreationStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/definition/strategy/MethodBeanDefinitionCreationStrategy.java) | A strategy for creating MethodBeanDefinition instances from Method elements. |
| [ObjectFactoryBeanDefinitionCreationStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/definition/strategy/ObjectFactoryBeanDefinitionCreationStrategy.java) | A strategy for creating BeanDefinition instances from ObjectFactory objects. |

#### `org.jmouse.beans.events`

| Type | Responsibility / contract |
|---|---|
| [BeanContextEvent](../jmouse-beans/src/main/java/org/jmouse/beans/events/BeanContextEvent.java) | Implements/extends `AbstractEvent`. |
| [BeanContextEventCategory](../jmouse-beans/src/main/java/org/jmouse/beans/events/BeanContextEventCategory.java) | Enum values: `LIFECYCLE`, `LOOKUP`, `CREATION`, `INITIALIZATION`, `FAILURE`. |
| [BeanContextEventPayload](../jmouse-beans/src/main/java/org/jmouse/beans/events/BeanContextEventPayload.java) | 🔔 Marker interface for all BeanContext event payloads. |
| [BeanContextEventSupport](../jmouse-beans/src/main/java/org/jmouse/beans/events/BeanContextEventSupport.java) | Convenience hooks API for subscribing to BeanEventName lifecycle events. 🧩 |
| [BeanEventCategory](../jmouse-beans/src/main/java/org/jmouse/beans/events/BeanEventCategory.java) | Bean module event categories. |
| [BeanEventDeduplicateKeyStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/events/BeanEventDeduplicateKeyStrategy.java) | Dedup key strategy for BeanContext events. |
| [BeanEventName](../jmouse-beans/src/main/java/org/jmouse/beans/events/BeanEventName.java) | Defines all lifecycle, resolution, and failure events emitted by a BeanContext. |
| [EventSeverity](../jmouse-beans/src/main/java/org/jmouse/beans/events/EventSeverity.java) | Enum values: `INFO`, `WARNING`, `ERROR`. |

#### `org.jmouse.beans.instantiation`

| Type | Responsibility / contract |
|---|---|
| [AbstractBeanInstantiationStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/instantiation/AbstractBeanInstantiationStrategy.java) | An abstract base class for BeanInstantiationStrategy implementations. Provides a utility method for resolving dependencies from a BeanContext. |
| [BeanInstantiationFactory](../jmouse-beans/src/main/java/org/jmouse/beans/instantiation/BeanInstantiationFactory.java) | A factory interface for creating bean instances using various BeanInstantiationStrategy implementations. |
| [BeanInstantiationStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/instantiation/BeanInstantiationStrategy.java) | Interface representing a strategy for instantiating beans in a container. Provides methods to create a bean instance and determine if the strategy supports a given BeanDefinition. |
| [ConstructorBeanInstantiationStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/instantiation/ConstructorBeanInstantiationStrategy.java) | A strategy for instantiating beans using constructors. |
| [DependencyResolver](../jmouse-beans/src/main/java/org/jmouse/beans/instantiation/DependencyResolver.java) | Strategy interface for resolving a BeanDependency into an actual bean instance within a given BeanContext. |
| [MethodBeanInstantiationStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/instantiation/MethodBeanInstantiationStrategy.java) | A strategy for instantiating beans using factory methods. |
| [ObjectFactoryBeanInstantiationStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/instantiation/ObjectFactoryBeanInstantiationStrategy.java) | A BeanInstantiationStrategy implementation for beans defined by an ObjectFactory. This strategy creates bean instances using the provided ObjectFactory. |
| [SimpleDependencyResolver](../jmouse-beans/src/main/java/org/jmouse/beans/instantiation/SimpleDependencyResolver.java) | Default DependencyResolver based on BeanResolutionStrategy. 🧩 |

#### `org.jmouse.beans.naming`

| Type | Responsibility / contract |
|---|---|
| [AnnotationBeanNameStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/naming/AnnotationBeanNameStrategy.java) | A strategy for resolving bean names based on annotations present on a Class or Method. This strategy prioritizes annotation values in the following order: |
| [BeanNameResolver](../jmouse-beans/src/main/java/org/jmouse/beans/naming/BeanNameResolver.java) | Interface for resolving bean names using a set of BeanNameStrategy instances. |
| [BeanNameStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/naming/BeanNameStrategy.java) | Strategy interface for resolving bean names based on annotated elements. |
| [ClassNameStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/naming/ClassNameStrategy.java) | Implements/extends `BeanNameStrategy`. Operations: `supports`, `resolve`. |
| [DefaultBeanNameResolver](../jmouse-beans/src/main/java/org/jmouse/beans/naming/DefaultBeanNameResolver.java) | DirectAccess implementation of BeanNameResolver. |

#### `org.jmouse.beans.processor`

| Type | Responsibility / contract |
|---|---|
| [BeanContextAwareBeanPostProcessor](../jmouse-beans/src/main/java/org/jmouse/beans/processor/BeanContextAwareBeanPostProcessor.java) | A BeanPostProcessor implementation that injects the BeanContext into beans implementing BeanContextAware. |
| [BeanNameKeeperBeanPostProcessor](../jmouse-beans/src/main/java/org/jmouse/beans/processor/BeanNameKeeperBeanPostProcessor.java) | BeanPostProcessor that populates beans implementing BeanNameKeeper with their configured bean name from the BeanDefinition. |
| [BeanPostProcessor](../jmouse-beans/src/main/java/org/jmouse/beans/processor/BeanPostProcessor.java) | Interface for performing custom operations on beans during their initialization lifecycle. |
| [BeanPostProcessorAware](../jmouse-beans/src/main/java/org/jmouse/beans/processor/BeanPostProcessorAware.java) | Interface for classes that are aware of and can manage BeanPostProcessor instances. |
| [BeanProcessorException](../jmouse-beans/src/main/java/org/jmouse/beans/processor/BeanProcessorException.java) | Exception class for handling errors related to BeanPostProcessor. |
| [InjectDependencyBeanPostProcessor](../jmouse-beans/src/main/java/org/jmouse/beans/processor/InjectDependencyBeanPostProcessor.java) | Injects field dependencies annotated with Dependency or Inject. 💉 |
| [ProxyBeanPostProcessor](../jmouse-beans/src/main/java/org/jmouse/beans/processor/ProxyBeanPostProcessor.java) | A BeanPostProcessor implementation that handles proxy-related logic for beans. |

#### `org.jmouse.beans.proxy`

| Type | Responsibility / contract |
|---|---|
| [GlobalLoggingMethodInterceptor](../jmouse-beans/src/main/java/org/jmouse/beans/proxy/GlobalLoggingMethodInterceptor.java) | A parser logging interceptor that logs method calls before and after their invocation, as well as during the invocation process. This interceptor is annotated with Intercept @ProxyMethodInterceptor to indicate that it can be automatically discovered and applied by a compatible proxy factory. |

#### `org.jmouse.beans.resolve`

| Type | Responsibility / contract |
|---|---|
| [BeanCandidate](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/BeanCandidate.java) | Resolved bean candidate with metadata. 🧩 |
| [BeanCandidateProvider](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/BeanCandidateProvider.java) | Provider of bean candidates for resolution. 📚 |
| [BeanContextCandidateProvider](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/BeanContextCandidateProvider.java) | BeanCandidateProvider backed by BeanContext. 🏗️ |
| [BeanResolutionRequest](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/BeanResolutionRequest.java) | Describes a bean resolution request. 🧩 |
| [BeanResolutionStrategies](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/BeanResolutionStrategies.java) | Factory methods for common bean resolution strategies. 🏭 |
| [BeanResolutionStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/BeanResolutionStrategy.java) | Strategy for resolving values from org.jmouse.beans.BeanContext. 🧠 |
| [BeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/BeanResolver.java) | A single bean resolution step in a resolution chain. 🔗 |
| [CompositeBeanResolutionStrategy](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/CompositeBeanResolutionStrategy.java) | Composite BeanResolutionStrategy that delegates resolution to ordered resolvers. 🧩 |
| [NullableSupport](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/NullableSupport.java) | Utilities for detecting nullable annotations. 🌫️ |

#### `org.jmouse.beans.resolve.resolver`

| Type | Responsibility / contract |
|---|---|
| [ArrayBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/resolver/ArrayBeanResolver.java) | Resolves array-based dependencies. 📦 |
| [BeansBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/resolver/BeansBeanResolver.java) | AbstractBeanResolver implementation that resolves dependencies of type Beans. 📦 |
| [CollectionBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/resolver/CollectionBeanResolver.java) | AbstractBeanResolver implementation that resolves collection-based dependencies. 📚 |
| [JakartaNamedBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/resolver/JakartaNamedBeanResolver.java) | AnnotatedBeanResolver implementation that resolves dependencies annotated with jakarta.inject.Named. 🏷️ |
| [MapBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/resolver/MapBeanResolver.java) | AbstractBeanResolver implementation that resolves map-based dependencies. 🗺️ |
| [OptionalBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/resolver/OptionalBeanResolver.java) | Resolves Optional dependencies. 🎯 |
| [PrimaryBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/resolver/PrimaryBeanResolver.java) | Resolves a single primary bean for a given type. ⭐ |
| [ProviderBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/resolver/ProviderBeanResolver.java) | Resolves lazy provider-style dependencies. 🏭 |
| [QualifierBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/resolver/QualifierBeanResolver.java) | Resolves dependencies annotated with Qualifier. 🏷️ |
| [TypeBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/resolver/TypeBeanResolver.java) | Resolves a bean by requested type. 🧩 |

#### `org.jmouse.beans.resolve.support`

| Type | Responsibility / contract |
|---|---|
| [AbstractBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/support/AbstractBeanResolver.java) | Base implementation of BeanResolver. 🧩 |
| [AbstractDelegatingBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/support/AbstractDelegatingBeanResolver.java) | Base resolver that delegates nested resolution to a BeanResolutionStrategy. 🔗 |
| [AnnotatedBeanResolver](../jmouse-beans/src/main/java/org/jmouse/beans/resolve/support/AnnotatedBeanResolver.java) | Base resolver for annotation-driven bean resolution. 🏷️ |

#### `org.jmouse.beans.scanner`

| Type | Responsibility / contract |
|---|---|
| [BeanAnnotatedClassesBeanScanner](../jmouse-beans/src/main/java/org/jmouse/beans/scanner/BeanAnnotatedClassesBeanScanner.java) | A scanner that identifies classes or elements annotated with Bean. |
| [BeanFactoriesAnnotatedClassBeanScanner](../jmouse-beans/src/main/java/org/jmouse/beans/scanner/BeanFactoriesAnnotatedClassBeanScanner.java) | A BeanScanner implementation that scans for methods annotated with Bean in classes annotated with BeanFactories. |
| [BeanImportAnnotatedClassBeanScanner](../jmouse-beans/src/main/java/org/jmouse/beans/scanner/BeanImportAnnotatedClassBeanScanner.java) | 📦 BeanScanner that processes BeanImport-annotated classes and recursively scans imported configurations. |

#### `org.jmouse.beans.support`

| Type | Responsibility / contract |
|---|---|
| [EnvironmentValue](../jmouse-beans/src/main/java/org/jmouse/beans/support/EnvironmentValue.java) | Annotation contract; members and defaults are defined in this source. |

### Packaged resources

See [packaged resources](../jmouse-beans/src/main/resources/) for configuration/templates owned by this module.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
