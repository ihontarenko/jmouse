# jmouse-context

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-context`. Build/dependency declaration: [pom.xml](../jmouse-context/pom.xml).

## What it is

Application-level services on top of jmouse-beans: environment properties, resource loading, conditional registration, configuration-property objects and feature selection. The module makes the lightweight bean container usable as an application context.

The beans module owns generic construction and lookup. This module adds application conventions and post-processors. It remains separate from Spring Boot adapters and from product startup/transport code.

## The map

| Type | Responsibility |
| --- | --- |
| [ApplicationBeanContext](../jmouse-context/src/main/java/org/jmouse/context/ApplicationBeanContext.java) | BeanContext enriched with environment/property and resource-loader access. |
| [AbstractApplicationBeanContext](../jmouse-context/src/main/java/org/jmouse/context/AbstractApplicationBeanContext.java) | Installs application post-processors and filters definitions through ConditionEvaluator. |
| [ApplicationContextFactory](../jmouse-context/src/main/java/org/jmouse/context/ApplicationContextFactory.java) | Contract for root, child and application-context construction. |
| [AbstractApplicationFactory](../jmouse-context/src/main/java/org/jmouse/context/AbstractApplicationFactory.java) | Shared default environment creation and classpath property loading. |
| [ContextComponentsBeanContextInitializer](../jmouse-context/src/main/java/org/jmouse/context/ContextComponentsBeanContextInitializer.java) | Contributes the core components required by an application context. |
| [BeanProperties](../jmouse-context/src/main/java/org/jmouse/context/BeanProperties.java) | Annotation declaring a property-backed bean. |
| [BeanPropertiesBeanPostProcessor](../jmouse-context/src/main/java/org/jmouse/context/processor/BeanPropertiesBeanPostProcessor.java) | Binds application properties onto eligible beans. |
| [EnvironmentValueBeanPostProcessor](../jmouse-context/src/main/java/org/jmouse/context/processor/EnvironmentValueBeanPostProcessor.java) | Injects values from the configured environment. |
| [ResourceInjectionBeanPostProcessor](../jmouse-context/src/main/java/org/jmouse/context/processor/ResourceInjectionBeanPostProcessor.java) | Injects resources through the context resource loader. |
| [RecordPropertiesInstantiationStrategy](../jmouse-context/src/main/java/org/jmouse/context/instantiation/RecordPropertiesInstantiationStrategy.java) | Creates record-shaped configuration values. |
| [FeatureSelector](../jmouse-context/src/main/java/org/jmouse/context/feature/FeatureSelector.java) | Contract deciding which feature classes to import. |
| [FeatureSelectionImporter](../jmouse-context/src/main/java/org/jmouse/context/FeatureSelectionImporter.java) | Imports selected feature contributions into the context. |

## Execution and ownership

1. Create or subclass the application factory/context appropriate for the application hierarchy.
2. Prepare the Environment and its property sources before property-backed beans are materialized.
3. Register definitions: AbstractApplicationBeanContext evaluates registration conditions and skips definitions that do not match.
4. Refresh the context so required components, scanners and feature imports run, then resolve application beans and resources.

## Public API and behavior

getEnvironment resolves Environment as a bean. Calling it before infrastructure initialization produces a context-specific failure.

PropertyResolver methods on AbstractApplicationBeanContext delegate to the context Environment.

createDefaultEnvironment contributes named system-environment and system-properties sources.

BeanIfExpressionSatisfied, BeanIfNameAbsent, BeanIfTypeAbsent and BeanIfPropertyMatch express different registration conditions.

## Wiring and extension points

- Implement ApplicationConfigurer/ServiceConfigurer for application wiring conventions.
- Implement FeatureSelector and its metadata/binding collaborators when imported components depend on application feature selection.
- Use the existing properties/resource processors instead of duplicating injection logic in every application bean.

## Pitfalls

- Condition evaluation happens at definition registration, so properties and prerequisite definitions must be available at that boundary.
- Environment access before context initialization is not a valid shortcut to bootstrap properties.
- Do not merge application EnvironmentValue with similarly named lower-level annotations without checking the owning package.
- Keep feature declarations distinct from instantiated beans: selecting a feature contributes classes for subsequent container processing.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-context/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-context/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.jmouse:jmouse-beans` | compile | no |
| `org.jmouse:jmouse-el` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.context`

| Type | Responsibility / contract |
|---|---|
| [AbstractApplicationBeanContext](../jmouse-context/src/main/java/org/jmouse/context/AbstractApplicationBeanContext.java) | 🧠 Base context with environment and resource support. |
| [AbstractApplicationFactory](../jmouse-context/src/main/java/org/jmouse/context/AbstractApplicationFactory.java) | Abstract base class for implementing ApplicationFactory. |
| [ApplicationBeanContext](../jmouse-context/src/main/java/org/jmouse/context/ApplicationBeanContext.java) | A specialized BeanContext that combines functionality from BeanFactory, PropertyResolver, and environment/resource management. |
| [ApplicationConfigurer](../jmouse-context/src/main/java/org/jmouse/context/ApplicationConfigurer.java) | A high-level configuration interface for setting up an application context. By extending both ConversionConfigurer, implementations can configure mappings, conversions, and optionally register singletons within a |
| [ApplicationContextBeansScanner](../jmouse-context/src/main/java/org/jmouse/context/ApplicationContextBeansScanner.java) | 🔍 Extends the base BeansScannerBeanContextInitializer to add scanning support for classes annotated with BeanProperties. |
| [ApplicationContextEvent](../jmouse-context/src/main/java/org/jmouse/context/ApplicationContextEvent.java) | An event class for publishing ApplicationBeanContext updates or lifecycle changes. This event carries an ApplicationBeanContext as its payload and provides constants indicating different stages in the application's lifecycle. Typical usage involves creating and firing the event at key application milestones, allowing interested observers to respond accordingly. |
| [ApplicationContextFactory](../jmouse-context/src/main/java/org/jmouse/context/ApplicationContextFactory.java) | 🏭 Factory for creating ApplicationBeanContext instances. |
| [ApplicationFactory](../jmouse-context/src/main/java/org/jmouse/context/ApplicationFactory.java) | A factory interface for creating instances of ApplicationBeanContext. |
| [BeanContextInstantiationType](../jmouse-context/src/main/java/org/jmouse/context/BeanContextInstantiationType.java) | Enum values: `RECORD_BEAN_PROPERTIES`. |
| [BeanIfExpressionSatisfied](../jmouse-context/src/main/java/org/jmouse/context/BeanIfExpressionSatisfied.java) | 🧠 Enables conditional bean registration based on expression evaluation. |
| [BeanIfNameAbsent](../jmouse-context/src/main/java/org/jmouse/context/BeanIfNameAbsent.java) | 📦 Conditional annotation that registers a bean only if a bean with the given name is absent. |
| [BeanIfPropertyMatch](../jmouse-context/src/main/java/org/jmouse/context/BeanIfPropertyMatch.java) | 🧩 Registers a bean if a property exists (optionally with expected value). |
| [BeanIfTypeAbsent](../jmouse-context/src/main/java/org/jmouse/context/BeanIfTypeAbsent.java) | 📦 Conditional annotation that registers a bean only if no bean of the given type exists. |
| [BeanProperties](../jmouse-context/src/main/java/org/jmouse/context/BeanProperties.java) | 🧩 Indicates that a bean should be configured using properties. |
| [BeanPropertiesClassBeanScanner](../jmouse-context/src/main/java/org/jmouse/context/BeanPropertiesClassBeanScanner.java) | 🔍 Scans for classes annotated with BeanProperties. |
| [ComparisonOperator](../jmouse-context/src/main/java/org/jmouse/context/ComparisonOperator.java) | 🔀 Comparison logic for property matching. |
| [ContextComponentsBeanContextInitializer](../jmouse-context/src/main/java/org/jmouse/context/ContextComponentsBeanContextInitializer.java) | Implements/extends `BeanContextInitializer`. Operations: `initialize`. |
| [EnvironmentValue](../jmouse-context/src/main/java/org/jmouse/context/EnvironmentValue.java) | Annotation contract; members and defaults are defined in this source. |
| [FeatureSelectionImporter](../jmouse-context/src/main/java/org/jmouse/context/FeatureSelectionImporter.java) | Imports feature classes into a BeanContext by scanning them and registering discovered bean definitions. 🧩 |
| [FeatureSelectorBeanContextInitializer](../jmouse-context/src/main/java/org/jmouse/context/FeatureSelectorBeanContextInitializer.java) | 🧩 BeanContextInitializer that drives feature selection and import process. |
| [FrameworkFactories](../jmouse-context/src/main/java/org/jmouse/context/FrameworkFactories.java) | 🧩 Maps interfaces to factory implementations via `.properties` files. |
| [Resource](../jmouse-context/src/main/java/org/jmouse/context/Resource.java) | Annotation contract; members and defaults are defined in this source. |
| [ServiceConfigurer](../jmouse-context/src/main/java/org/jmouse/context/ServiceConfigurer.java) | A high-level configuration interface for setting up an application services. |

#### `org.jmouse.context.conversion`

| Type | Responsibility / contract |
|---|---|
| [ContextConversion](../jmouse-context/src/main/java/org/jmouse/context/conversion/ContextConversion.java) | Implements/extends `PredefinedConversion`. |

#### `org.jmouse.context.definition`

| Type | Responsibility / contract |
|---|---|
| [RecordPropertiesDefinition](../jmouse-context/src/main/java/org/jmouse/context/definition/RecordPropertiesDefinition.java) | Bean definition for record-based @BeanProperties. 📦 |
| [RecordPropertiesDefinitionCreationStrategy](../jmouse-context/src/main/java/org/jmouse/context/definition/RecordPropertiesDefinitionCreationStrategy.java) | Creates RecordPropertiesDefinition for record classes annotated with BeanProperties. 🧩 |

#### `org.jmouse.context.feature`

| Type | Responsibility / contract |
|---|---|
| [AnnotationBasedFeatureMetadataResolver](../jmouse-context/src/main/java/org/jmouse/context/feature/AnnotationBasedFeatureMetadataResolver.java) | 🔎 Annotation-based implementation of FeatureMetadataResolver. |
| [AnnotationFeatureSelectionResolver](../jmouse-context/src/main/java/org/jmouse/context/feature/AnnotationFeatureSelectionResolver.java) | 🔎 FeatureSelectionResolver that derives feature classes from annotations. |
| [FeatureImport](../jmouse-context/src/main/java/org/jmouse/context/feature/FeatureImport.java) | Annotation contract; members and defaults are defined in this source. |
| [FeatureMetadata](../jmouse-context/src/main/java/org/jmouse/context/feature/FeatureMetadata.java) | Operations: `imports`, `selectors`, `empty`. |
| [FeatureMetadataResolver](../jmouse-context/src/main/java/org/jmouse/context/feature/FeatureMetadataResolver.java) | Operations: `resolve`. |
| [FeatureSelectionResolver](../jmouse-context/src/main/java/org/jmouse/context/feature/FeatureSelectionResolver.java) | 🧩 Strategy interface for resolving feature classes from a given context. |
| [FeatureSelector](../jmouse-context/src/main/java/org/jmouse/context/feature/FeatureSelector.java) | Operations: `select`. |
| [FeatureSelectorBinding](../jmouse-context/src/main/java/org/jmouse/context/feature/FeatureSelectorBinding.java) | Annotation contract; members and defaults are defined in this source. |
| [FeatureSelectorContext](../jmouse-context/src/main/java/org/jmouse/context/feature/FeatureSelectorContext.java) | Operations: `beanContext`, `sourceClasses`. |
| [FeatureSelectorFactory](../jmouse-context/src/main/java/org/jmouse/context/feature/FeatureSelectorFactory.java) | 🏭 Factory for creating FeatureSelector instances. |
| [ReflectionFeatureSelectorFactory](../jmouse-context/src/main/java/org/jmouse/context/feature/ReflectionFeatureSelectorFactory.java) | 🏭 Reflection-based implementation of FeatureSelectorFactory. |

#### `org.jmouse.context.feature.scanner`

| Type | Responsibility / contract |
|---|---|
| [ExplicitFeatureClassBeanScanner](../jmouse-context/src/main/java/org/jmouse/context/feature/scanner/ExplicitFeatureClassBeanScanner.java) | 📦 BeanScanner that processes explicitly provided feature classes. |

#### `org.jmouse.context.instantiation`

| Type | Responsibility / contract |
|---|---|
| [RecordPropertiesInstantiationStrategy](../jmouse-context/src/main/java/org/jmouse/context/instantiation/RecordPropertiesInstantiationStrategy.java) | Instantiation strategy for record beans backed by BeanProperties. 🧩 |

#### `org.jmouse.context.processor`

| Type | Responsibility / contract |
|---|---|
| [BeanPropertiesBeanPostProcessor](../jmouse-context/src/main/java/org/jmouse/context/processor/BeanPropertiesBeanPostProcessor.java) | BeanPostProcessor that binds external configuration properties into beans annotated with BeanProperties before they are initialized. |
| [EnvironmentValueBeanPostProcessor](../jmouse-context/src/main/java/org/jmouse/context/processor/EnvironmentValueBeanPostProcessor.java) | 🌱 Injects environment properties into bean fields marked with EnvironmentValue. |
| [ResourceInjectionBeanPostProcessor](../jmouse-context/src/main/java/org/jmouse/context/processor/ResourceInjectionBeanPostProcessor.java) | Implements/extends `BeanPostProcessor`. Operations: `postProcessAfterInitialize`. |

### Packaged resources

See [packaged resources](../jmouse-context/src/main/resources/) for configuration/templates owned by this module.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
