# jmouse-mapper

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-mapper`. Build/dependency declaration: [pom.xml](../jmouse-mapper/pom.xml).

## What it is

A recursive object mapping engine with pluggable strategies, explicit per-type rules, conversion and policy. It maps supported beans, records, maps, arrays and collections while keeping traversal state and plugin callbacks inside a mapping invocation.

Mapping rules describe source/target intent; strategies implement target shapes; core accessors and conversion supply mechanics. Textual .jmm rules are compiled by mapper-el into the same TypeMappingRule model, so there is one mapping engine.

## The map

| Type | Responsibility |
| --- | --- |
| [Mapper](../jmouse-mapper/src/main/java/org/jmouse/mapper/Mapper.java) | Public mapping contract using a target type/value description. |
| [Mappers](../jmouse-mapper/src/main/java/org/jmouse/mapper/Mappers.java) | Factory entry points for the standard mapper wiring. |
| [MapperBuilder](../jmouse-mapper/src/main/java/org/jmouse/mapper/MapperBuilder.java) | Explicitly wires strategy registry, accessors, conversion, policy, configuration and rules. |
| [ObjectMapper](../jmouse-mapper/src/main/java/org/jmouse/mapper/ObjectMapper.java) | Runs a MappingInvocation, resolves a MappingStrategy and applies error recovery. |
| [MappingContext](../jmouse-mapper/src/main/java/org/jmouse/mapper/MappingContext.java) | Shared mapper collaborators and the mapper constructed from them. |
| [MappingInvocation](../jmouse-mapper/src/main/java/org/jmouse/mapper/MappingInvocation.java) | Per-call scope and plugin/error lifecycle. |
| [MappingConfig](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/MappingConfig.java) | Factories, depth/size limits, object-to-map behavior, errors and plugins. |
| [MappingPolicy](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/MappingPolicy.java) | Behavioral choices for nulls, references, collection mapping and unassigned properties. |
| [TypeMappingRegistry](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/TypeMappingRegistry.java) | Resolves explicit TypeMappingRule definitions by source/target type. |
| [TypeMappingBuilder](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/TypeMappingBuilder.java) | Java DSL for per-type mapping rules and property mappings. |
| [MappingStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/MappingStrategy.java) | Shape-specific execution seam; implemented by bean, record, collection, array, map and scalar strategies. |
| [MappingPlugin](../jmouse-mapper/src/main/java/org/jmouse/mapper/plugin/MappingPlugin.java) | Observes or adjusts mapping through the plugin bus. |

## Execution and ownership

1. Assemble the mapper once with Mappers or MapperBuilder, including all rules and conversion registrations.
2. Pass a source object and target TypedValue to Mapper. ObjectMapper immediately returns null for a null source.
3. A MappingInvocation owns the source-root scope; the registry selects the appropriate mapping strategy.
4. Strategies recursively map properties/elements through shared services. Invocation recovery reports mapping failures to plugins and follows ErrorsPolicy.

## Public API and behavior

MapperBuilder.rules adds to the builder rule set; mappingRegistry replaces the rule registry outright.

MappingConfig exposes maxDepth and maxCollectionSize as guards against runaway traversal or materialization.

TypeMappingRuleSource supports Java, annotation and external-language producers without requiring strategies to know how rules were authored.

TypeMapper is the explicit typed implementation seam when a pair needs custom code instead of reflective property mapping.

## Wiring and extension points

- Register MappingStrategyContributor implementations for a new target shape.
- Supply collection/map factories and MappingPolicy deliberately when concrete destination types or reference behavior matter.
- Add MappingPlugin implementations for cross-cutting observations; use error policy separately from the plugin notification mechanism.

## Pitfalls

- Do not create a mapper per property or per item; wiring and metadata/rule lookup are intended to be reused.
- A swallowed MappingException is still governed by configured recovery policy. A null output is not proof that mapping was successful.
- Choose collection/depth limits for external data rather than relying on accidentally finite input.
- Property rules are addressed by target properties; schema validation for authored .jmm belongs to its eager reader.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-mapper/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Existing executable examples/smokes: [FixProbe](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/FixProbe.java), [GuardProbe](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/GuardProbe.java), [MappingBenchmark](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/MappingBenchmark.java), [Smoke3](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/Smoke3.java), [SmokeDeepMapping](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/SmokeDeepMapping.java).

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-mapper/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.mapper`

| Type | Responsibility / contract |
|---|---|
| [Mapper](../jmouse-mapper/src/main/java/org/jmouse/mapper/Mapper.java) | Core mapping API for converting objects between types. 🧩 |
| [MapperBuilder](../jmouse-mapper/src/main/java/org/jmouse/mapper/MapperBuilder.java) | Fluent builder for constructing a Mapper with explicit wiring. 🧱 |
| [MapperConversion](../jmouse-mapper/src/main/java/org/jmouse/mapper/MapperConversion.java) | Implements/extends `PredefinedConversion`. |
| [MapperProvider](../jmouse-mapper/src/main/java/org/jmouse/mapper/MapperProvider.java) | Operations: `get`. |
| [Mappers](../jmouse-mapper/src/main/java/org/jmouse/mapper/Mappers.java) | Entry point for obtaining a Mapper. 🧰 |
| [MappingContext](../jmouse-mapper/src/main/java/org/jmouse/mapper/MappingContext.java) | Value record: `( MapperProvider mapperProvider, StrategyRegistry strategyRegistry, AccessorWrapper wrapper, Conversion conversion, TypeMappingRegistry mappingRegistry, MappingPolicy policy, MappingConfig config, MappingScope scope )`. |
| [MappingDestination](../jmouse-mapper/src/main/java/org/jmouse/mapper/MappingDestination.java) | The slot a mapped value is on its way into. 📍 |
| [MappingInvocation](../jmouse-mapper/src/main/java/org/jmouse/mapper/MappingInvocation.java) | Orchestrates a single mapping invocation and its plugin lifecycle. 🧭 |
| [MappingScope](../jmouse-mapper/src/main/java/org/jmouse/mapper/MappingScope.java) | Mapping scope holder that tracks the root source object and the current property path. 🧭 |
| [ObjectMapper](../jmouse-mapper/src/main/java/org/jmouse/mapper/ObjectMapper.java) | Default Mapper implementation that executes mapping plans using a MappingContext. 🧠 |

#### `org.jmouse.mapper.binding`

| Type | Responsibility / contract |
|---|---|
| [ComputeFunction](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/ComputeFunction.java) | Operations: `compute`. |
| [MappingAssertion](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/MappingAssertion.java) | A condition that stops a mapping, and what to say when it holds. 🚫 |
| [MappingAssertions](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/MappingAssertions.java) | Running a set of MappingAssertions at one point of a mapping. 🛑 |
| [PropertyMapping](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/PropertyMapping.java) | Declarative mapping rule for a single target property. 🧩 |
| [PropertyMappingBuilder](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/PropertyMappingBuilder.java) | DSL builder for configuring a PropertyMapping for a single target property. 🧩 |
| [PropertyMappingEvaluation](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/PropertyMappingEvaluation.java) | Runtime evaluator for PropertyMapping definitions. ⚙️ |
| [PropertyMappings](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/PropertyMappings.java) | The explicit PropertyMappings that apply to one (sourceType -> targetType) pair. 🗂️ |
| [PropertyMappingVisitor](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/PropertyMappingVisitor.java) | Visitor for evaluating or transforming PropertyMapping definitions. 🧠 |
| [TypeMappingBuilder](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/TypeMappingBuilder.java) | Fluent builder for defining a TypeMappingRule between S -> T. 🧱 |
| [TypeMappingRegistry](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/TypeMappingRegistry.java) | Registry of TypeMappingRuleSource mapping rule sources. 🧩 |
| [TypeMappingRule](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/TypeMappingRule.java) | Immutable mapping rule for a specific (sourceType -> targetType) pair. 🧩 |
| [TypeMappingRuleSource](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/TypeMappingRuleSource.java) | Strategy interface for resolving TypeMappingRule mapping rules. 🧭 |
| [ValueProvider](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/ValueProvider.java) | Operations: `provide`. |
| [ValueTransformer](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/ValueTransformer.java) | Operations: `transform`. |

#### `org.jmouse.mapper.binding.annotation`

| Type | Responsibility / contract |
|---|---|
| [AnnotationRuleSource](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/annotation/AnnotationRuleSource.java) | TypeMappingRuleSource that derives TypeMappingRules from annotations declared on the target type's accessor methods. 🏷️ |
| [MappingConstant](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/annotation/MappingConstant.java) | Annotation contract; members and defaults are defined in this source. |
| [MappingIgnore](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/annotation/MappingIgnore.java) | Annotation contract; members and defaults are defined in this source. |
| [MappingReference](../jmouse-mapper/src/main/java/org/jmouse/mapper/binding/annotation/MappingReference.java) | Annotation contract; members and defaults are defined in this source. |

#### `org.jmouse.mapper.config`

| Type | Responsibility / contract |
|---|---|
| [ArrayMaterializationPolicy](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/ArrayMaterializationPolicy.java) | Controls how arrays are produced from iterable-like sources. |
| [CollectionMappingPolicy](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/CollectionMappingPolicy.java) | Enum values: `REPLACE`, `MERGE_APPEND`. |
| [MapKeyPolicy](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/MapKeyPolicy.java) | Enum values: `STRINGIFY`, `FAIL`, `SKIP`. |
| [MappingConfig](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/MappingConfig.java) | Runtime configuration for the mapping engine. ⚙️ |
| [MappingPolicy](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/MappingPolicy.java) | Operations: `builder`, `defaults`, `nullHandlingPolicy`, `typeMismatchPolicy`, `unassignedTargetPropertyPolicy`, `collectionMappingPolicy`, `referenceMappingPolicy`, `build`. |
| [NullHandlingPolicy](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/NullHandlingPolicy.java) | Enum values: `PROPAGATE`, `SKIP`, `NULL_TO_EMPTY`. |
| [ObjectToMapMode](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/ObjectToMapMode.java) | Enum values: `TREE`, `STRINGIFY`, `FLATTEN`. |
| [ReferenceMappingPolicy](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/ReferenceMappingPolicy.java) | What to do when a source object is reached again while it is still being mapped. 🔄 |
| [TypeMismatchPolicy](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/TypeMismatchPolicy.java) | Enum values: `CONVERT_IF_POSSIBLE`, `FAIL`. |
| [UnassignedTargetPropertyPolicy](../jmouse-mapper/src/main/java/org/jmouse/mapper/config/UnassignedTargetPropertyPolicy.java) | Enum values: `LEAVE_DEFAULT`, `SET_NULL`, `FAIL`. |

#### `org.jmouse.mapper.errors`

| Type | Responsibility / contract |
|---|---|
| [ErrorAction](../jmouse-mapper/src/main/java/org/jmouse/mapper/errors/ErrorAction.java) | Defines how the mapping engine should handle a mapping error. 🎯 |
| [ErrorCodes](../jmouse-mapper/src/main/java/org/jmouse/mapper/errors/ErrorCodes.java) | Stable machine-readable error codes for mapping engine. |
| [ErrorsPolicy](../jmouse-mapper/src/main/java/org/jmouse/mapper/errors/ErrorsPolicy.java) | Error handling policy by error code (exact) and/or prefix. |
| [MappingException](../jmouse-mapper/src/main/java/org/jmouse/mapper/errors/MappingException.java) | A mapping failure, carrying a stable #code(), free-form #meta() and the #path() it happened at. 💥 |

#### `org.jmouse.mapper.plugin`

| Type | Responsibility / contract |
|---|---|
| [DebugInformationPlugin](../jmouse-mapper/src/main/java/org/jmouse/mapper/plugin/DebugInformationPlugin.java) | Implements/extends `MappingPlugin`. Operations: `onStart`, `onValue`, `onFinish`, `onError`. |
| [MappingCall](../jmouse-mapper/src/main/java/org/jmouse/mapper/plugin/MappingCall.java) | Value record: `(Object source, Class sourceType, InferredType targetType, MappingContext context)`. |
| [MappingFailure](../jmouse-mapper/src/main/java/org/jmouse/mapper/plugin/MappingFailure.java) | Value record: `(MappingException error, PropertyPath path, MappingContext context)`. |
| [MappingPlugin](../jmouse-mapper/src/main/java/org/jmouse/mapper/plugin/MappingPlugin.java) | Extension point for observing and transforming mapping execution. 🧩 |
| [MappingResult](../jmouse-mapper/src/main/java/org/jmouse/mapper/plugin/MappingResult.java) | Value record: `(Object source, Object target, InferredType targetType, MappingContext context)`. |
| [MappingValue](../jmouse-mapper/src/main/java/org/jmouse/mapper/plugin/MappingValue.java) | Value record: `( Object rootSource, Object current, InferredType targetType, PropertyPath path, // user.name / details[0] MappingContext context, MappingDestination destination )`. |
| [PluginBus](../jmouse-mapper/src/main/java/org/jmouse/mapper/plugin/PluginBus.java) | A lightweight dispatcher that propagates mapping lifecycle events to a list of MappingPlugins. 🚌 |
| [TrimStringsPlugin](../jmouse-mapper/src/main/java/org/jmouse/mapper/plugin/TrimStringsPlugin.java) | MappingPlugin that trims mapped String values. |

#### `org.jmouse.mapper.strategy`

| Type | Responsibility / contract |
|---|---|
| [MappingStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/MappingStrategy.java) | Compiled mapping unit responsible for producing a target value from a source object. 🧠 |
| [MappingStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/MappingStrategyContributor.java) | Factory/selector for building MappingStrategy instances. 🧩 |
| [MappingStrategyRegistry](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/MappingStrategyRegistry.java) | Default StrategyRegistry implementation that resolves and caches MappingStrategys. 🧠 |
| [StrategyRegistry](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/StrategyRegistry.java) | Strategy interface for resolving a MappingStrategy for a mapping request. 🧠 |
| [TargetShape](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/TargetShape.java) | What a target type is , answered once instead of per value. 🧬 |

#### `org.jmouse.mapper.strategy.array`

| Type | Responsibility / contract |
|---|---|
| [ArrayStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/array/ArrayStrategy.java) | Implements/extends `AbstractIterableStrategy`. |
| [ArrayStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/array/ArrayStrategyContributor.java) | Implements/extends `MappingStrategyContributor`. Operations: `supports`, `build`. |

#### `org.jmouse.mapper.strategy.bean`

| Type | Responsibility / contract |
|---|---|
| [BeanMappingPlan](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/bean/BeanMappingPlan.java) | Everything about one (sourceType -> targetType) pair that never changes between objects. 📋 |
| [JavaBeanStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/bean/JavaBeanStrategy.java) | Object mapping strategy for mutable JavaBeans (property-based mapping). 🫘 |
| [JavaBeanStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/bean/JavaBeanStrategyContributor.java) | Implements/extends `MappingStrategyContributor`. Operations: `supports`, `build`. |

#### `org.jmouse.mapper.strategy.collection`

| Type | Responsibility / contract |
|---|---|
| [ListCollectionStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/collection/ListCollectionStrategy.java) | Implements/extends `AbstractCollectionStrategy`. |
| [ListStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/collection/ListStrategyContributor.java) | Implements/extends `MappingStrategyContributor`. Operations: `supports`, `build`. |
| [SetCollectionStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/collection/SetCollectionStrategy.java) | Implements/extends `AbstractCollectionStrategy`. |
| [SetStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/collection/SetStrategyContributor.java) | Implements/extends `MappingStrategyContributor`. Operations: `supports`, `build`. |

#### `org.jmouse.mapper.strategy.converted`

| Type | Responsibility / contract |
|---|---|
| [ConvertedPairStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/converted/ConvertedPairStrategy.java) | A pair converted whole — one expression whose result is the target. 🎁 |
| [ConvertedPairStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/converted/ConvertedPairStrategyContributor.java) | Contributes ConvertedPairStrategy for a pair some rule converts whole. 🎁 |

#### `org.jmouse.mapper.strategy.direct`

| Type | Responsibility / contract |
|---|---|
| [TypeMapperStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/direct/TypeMapperStrategy.java) | MappingStrategy that delegates mapping to a registered TypeMapper. 🎯 |
| [TypeMapperStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/direct/TypeMapperStrategyContributor.java) | MappingStrategyContributor that resolves direct manual mappers by exact type pair. 🧭 |

#### `org.jmouse.mapper.strategy.map`

| Type | Responsibility / contract |
|---|---|
| [MapToMapStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/map/MapToMapStrategy.java) | Mapping strategy for Map -> Map transformations. 🗺️ |
| [MapToMapStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/map/MapToMapStrategyContributor.java) | Implements/extends `MappingStrategyContributor`. Operations: `supports`, `build`. |
| [ObjectToMapStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/map/ObjectToMapStrategy.java) | Mapping plan that converts an arbitrary object into a "tree" Map structure. 🗺️ |
| [ObjectToMapStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/map/ObjectToMapStrategyContributor.java) | MappingStrategyContributor that builds an ObjectToMapStrategy for mapping arbitrary objects into Map targets. 🗺️ |

#### `org.jmouse.mapper.strategy.record`

| Type | Responsibility / contract |
|---|---|
| [RecordStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/record/RecordStrategy.java) | Object mapping strategy for Java record targets. 🧾 |
| [RecordStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/record/RecordStrategyContributor.java) | Implements/extends `MappingStrategyContributor`. Operations: `supports`, `build`. |

#### `org.jmouse.mapper.strategy.scalar`

| Type | Responsibility / contract |
|---|---|
| [ScalarStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/scalar/ScalarStrategy.java) | Mapping strategy for scalar-like targets (primitive wrappers, strings, enums, and plain classes). 🧱 |
| [ScalarStrategyContributor](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/scalar/ScalarStrategyContributor.java) | Implements/extends `MappingStrategyContributor`. Operations: `supports`, `build`. |

#### `org.jmouse.mapper.strategy.support`

| Type | Responsibility / contract |
|---|---|
| [AbstractCollectionStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/support/AbstractCollectionStrategy.java) | Base strategy for mapping iterable sources into Collection targets. 🧺 |
| [AbstractIterableStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/support/AbstractIterableStrategy.java) | Base strategy for mapping iterable-like sources (arrays, collections, iterables) into iterable targets. 🔁 |
| [AbstractMapStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/support/AbstractMapStrategy.java) | Base strategy support for Map-target mappings. 🗺️ |
| [AbstractObjectStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/support/AbstractObjectStrategy.java) | Base strategy for source-to-object mapping scenarios. 🧩 |
| [AbstractStrategy](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/support/AbstractStrategy.java) | Base implementation for MappingStrategy that provides common utilities for mapping plan execution. 🧠 |
| [IterableSource](../jmouse-mapper/src/main/java/org/jmouse/mapper/strategy/support/IterableSource.java) | Uniform abstraction over iterable-like sources used by collection/array mapping strategies. 🔁 |

#### `org.jmouse.mapper.typed`

| Type | Responsibility / contract |
|---|---|
| [AbstractTypeMapper](../jmouse-mapper/src/main/java/org/jmouse/mapper/typed/AbstractTypeMapper.java) | Base class for TypeMapper with fixed source/target types. 🧱 |
| [TypeMapper](../jmouse-mapper/src/main/java/org/jmouse/mapper/typed/TypeMapper.java) | Direct typed mapper for an exact source/target pair. 🧩 |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [FixProbe.java](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/FixProbe.java).
- [GuardProbe.java](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/GuardProbe.java).
- [MappingBenchmark.java](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/MappingBenchmark.java).
- [MappingProfile.java](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/MappingProfile.java).
- [Smoke3.java](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/Smoke3.java).
- [SmokeDeepMapping.java](../jmouse-mapper/src/main/java/org/jmouse/mapper/examples/SmokeDeepMapping.java).
