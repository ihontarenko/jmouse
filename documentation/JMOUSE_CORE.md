# jmouse-core

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-core`. Build/dependency declaration: [pom.xml](../jmouse-core/pom.xml).

## What it is

The shared mechanisms underneath the jMouse modules: structured object access, reflection metadata, conversion, binding, resources, environment properties, interception, events and execution context. It provides small contracts and reusable implementations rather than an application bootstrap.

Use the owning package as the boundary. Object traversal belongs to access, type coercion to convert, object construction from external values to binding, and interception to proxy. The beans and context modules build container lifecycle on these mechanisms; they do not belong inside core.

## The map

| Type | Responsibility |
| --- | --- |
| [ObjectAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/ObjectAccessor.java) | Uniform property/index access to a wrapped value; callers do not need a separate branch for every supported shape. |
| [ObjectAccessorWrapper](../jmouse-core/src/main/java/org/jmouse/core/access/ObjectAccessorWrapper.java) | Chooses an accessor provider for scalars, arrays, collections, maps, beans, records and resolvers. |
| [PropertyPath](../jmouse-core/src/main/java/org/jmouse/core/access/PropertyPath.java) | Represents structured paths used while navigating object properties. |
| [ObjectDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/ObjectDescriptor.java) | Shared structured-object metadata contract used by bean, map and record descriptors. |
| [StandardConversion](../jmouse-core/src/main/java/org/jmouse/core/convert/StandardConversion.java) | Registers type-pair and named converters; searches inherited candidates and multi-step conversion paths. |
| [PredefinedConversion](../jmouse-core/src/main/java/org/jmouse/core/convert/PredefinedConversion.java) | Conversion service with the standard converter set installed. |
| [DefaultBinderFactory](../jmouse-core/src/main/java/org/jmouse/core/binding/DefaultBinderFactory.java) | Selects the first supporting object binder after ordering the registered binders. |
| [StandardEnvironment](../jmouse-core/src/main/java/org/jmouse/core/environment/StandardEnvironment.java) | Delegates property lookup and manages active profiles. |
| [DefaultProxyFactory](../jmouse-core/src/main/java/org/jmouse/core/proxy/DefaultProxyFactory.java) | Selects a supporting JDK or ByteBuddy proxy engine. |
| [EventManager](../jmouse-core/src/main/java/org/jmouse/core/events/EventManager.java) | Event subscription/publication infrastructure shared by higher-level modules. |
| [ResourceLoader](../jmouse-core/src/main/java/org/jmouse/core/io/ResourceLoader.java) | Resource discovery/loading contract, independent of the bean container. |
| [ExecutionContext](../jmouse-core/src/main/java/org/jmouse/core/context/execution/ExecutionContext.java) | Typed execution state used by infrastructure that needs to carry context. |

## Execution and ownership

1. Choose the service matching the task rather than assembling a global utility object: a conversion service, accessor wrapper, binder factory or proxy factory.
2. Register providers, converters, binders and interceptors during wiring. Provider order and binder support checks determine which implementation is selected.
3. Wrap source values before reading structured properties; keep reflection descriptors reusable rather than repeating bean introspection for each field.
4. Use execution/context holders only inside a boundary that also arranges cleanup; the framework has both current typed-context APIs and older scope APIs.

## Public API and behavior

ObjectAccessorWrapper.defaults() supplies the shared standard wrapper. A separately constructed wrapper accepts an explicit provider list.

StandardConversion supports both type-pair converters and converters addressed by name. A named registration is a separate registry; it does not implicitly become an automatic type-pair conversion.

DefaultBinderFactory is a registry, not a prepopulated binding policy: callers register the ObjectBinder implementations they need.

StandardEnvironment reads active profiles from the profiles property when its local active-profile set is empty. getDefaultProfiles currently returns an empty set.

## Wiring and extension points

- Implement ObjectAccessorProvider for a new object shape; retain the wrapper-aware navigation contract.
- Implement GenericConverter or Converter for coercion, ObjectBinder for construction, and MethodInterceptor for invocation behavior.
- ResourceLoader implementations separate classpath, filesystem, archive and network resolution. Select or compose loaders at the application boundary.

## Pitfalls

- Do not confuse binding with mapper rules: binding and conversion are lower-level facilities; the mapper module owns recursive mapping policy.
- DefaultProxyFactory requires a definition supported by an engine. The thrown error explicitly calls out interfaces for JDK proxies and a non-final class for ByteBuddy.
- Avoid concurrent mutation of configured services unless that implementation explicitly supports it. A concurrent converter map does not make every collaborator in the service immutable.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-core/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Existing executable examples/smokes: [NamedConverterProbe](../jmouse-core/src/main/java/org/jmouse/core/convert/examples/NamedConverterProbe.java), [Smoke1](../jmouse-core/src/main/java/org/jmouse/core/parameters/smoke/Smoke1.java).

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-core/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `net.bytebuddy:byte-buddy` | compile | no |
| `org.objenesis:objenesis` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse`

| Type | Responsibility / contract |
|---|---|
| [JMouseModule](../jmouse-core/src/main/java/org/jmouse/JMouseModule.java) | Represents a module in the JMouse framework. |

#### `org.jmouse.core`

| Type | Responsibility / contract |
|---|---|
| [AbstractStringIdGenerator](../jmouse-core/src/main/java/org/jmouse/core/AbstractStringIdGenerator.java) | 🔤 Abstract base for String-based ID generators. |
| [Adder](../jmouse-core/src/main/java/org/jmouse/core/Adder.java) | Operations: `add`. |
| [AnsiColors](../jmouse-core/src/main/java/org/jmouse/core/AnsiColors.java) | ANSI escape codes for terminal styling 🎨 |
| [AnyComparator](../jmouse-core/src/main/java/org/jmouse/core/AnyComparator.java) | Utility class for comparing arbitrary objects with null safety. |
| [AttributeMapper](../jmouse-core/src/main/java/org/jmouse/core/AttributeMapper.java) | A utility for managing value-based transformations and conditional execution. |
| [Bits](../jmouse-core/src/main/java/org/jmouse/core/Bits.java) | 🔢 Lightweight 64-bit flag set based on a long bitmask. |
| [Bytes](../jmouse-core/src/main/java/org/jmouse/core/Bytes.java) | Represents a size in bytes, with utility methods for parsing, converting, and comparing storage units (B, KB, MB, GB, TB, PB). |
| [CachedSupplier](../jmouse-core/src/main/java/org/jmouse/core/CachedSupplier.java) | A Supplier implementation that caches the result of the first invocation. |
| [CacheKey](../jmouse-core/src/main/java/org/jmouse/core/CacheKey.java) | Composite cache key abstraction. 🗝️ |
| [CharMask](../jmouse-core/src/main/java/org/jmouse/core/CharMask.java) | 🔤 Compact character set built on top of Bits windows (64 chars each). |
| [Charset](../jmouse-core/src/main/java/org/jmouse/core/Charset.java) | Enum containing many commonly used character encodings (charsets), along with additional attributes that may be useful. |
| [Chunker](../jmouse-core/src/main/java/org/jmouse/core/Chunker.java) | Operations: `split`. |
| [Clock](../jmouse-core/src/main/java/org/jmouse/core/Clock.java) | Operations: `nanoTime`. |
| [CollectionFactory](../jmouse-core/src/main/java/org/jmouse/core/CollectionFactory.java) | Factory utilities for creating Collection instances by requested collection type, with sensible defaults for common collection interfaces. |
| [Customizer](../jmouse-core/src/main/java/org/jmouse/core/Customizer.java) | 🪝 Customizer — single-argument action. |
| [CyclicReferenceDetector](../jmouse-core/src/main/java/org/jmouse/core/CyclicReferenceDetector.java) | Interface for detecting cyclic dependencies in structured creation or dependency graphs. |
| [DefaultCyclicReferenceDetector](../jmouse-core/src/main/java/org/jmouse/core/DefaultCyclicReferenceDetector.java) | DirectAccess implementation of the CyclicReferenceDetector interface for detecting cyclic dependencies among beans or objects of type T. It uses a thread-local Visitor to keep track of visited identifiers during dependency resolution, allowing the detection of cycles in the structured graph. |
| [Delegate](../jmouse-core/src/main/java/org/jmouse/core/Delegate.java) | Generic interface for managing a delegate structured. |
| [EmojiFingerprint](../jmouse-core/src/main/java/org/jmouse/core/EmojiFingerprint.java) | 🦊 An identifier hashed to an emoji — a fingerprint a person tells apart at a glance. |
| [Exceptions](../jmouse-core/src/main/java/org/jmouse/core/Exceptions.java) | Utility class for common exception handling and validation checks. |
| [Factory](../jmouse-core/src/main/java/org/jmouse/core/Factory.java) | A functional interface representing a factory for creating instances of type T. |
| [Getter](../jmouse-core/src/main/java/org/jmouse/core/Getter.java) | A functional interface representing a getter method for retrieving a value from an instance. |
| [Hash](../jmouse-core/src/main/java/org/jmouse/core/Hash.java) | 🧪 Hash mixing utilities (32/64-bit). |
| [HashStringGenerator](../jmouse-core/src/main/java/org/jmouse/core/HashStringGenerator.java) | Hash-based String ID generator using MessageDigest. |
| [IdGenerator](../jmouse-core/src/main/java/org/jmouse/core/IdGenerator.java) | 🆔 Functional interface for generating unique or pseudo-unique identifiers. |
| [IgnoreReasonGenerator](../jmouse-core/src/main/java/org/jmouse/core/IgnoreReasonGenerator.java) | Implements/extends `AbstractStringIdGenerator`. Operations: `generate`. |
| [InstanceResolver](../jmouse-core/src/main/java/org/jmouse/core/InstanceResolver.java) | 🧩 Functional contract for resolving instances by their Class type. |
| [InvalidMimeTypeException](../jmouse-core/src/main/java/org/jmouse/core/InvalidMimeTypeException.java) | Implements/extends `IllegalArgumentException`. Operations: `getMimeType`. |
| [LogBox](../jmouse-core/src/main/java/org/jmouse/core/LogBox.java) | Structured diagnostic log renderer (framework-level utility). 🚀 |
| [MapFactory](../jmouse-core/src/main/java/org/jmouse/core/MapFactory.java) | Operations: `createMap`, `createLike`, `createEnumMap`. |
| [Md5HashGenerator](../jmouse-core/src/main/java/org/jmouse/core/Md5HashGenerator.java) | MD5-based String ID/hash generator. |
| [MediaType](../jmouse-core/src/main/java/org/jmouse/core/MediaType.java) | Extension of MimeType that adds support for the HTTP "q" (quality) parameter. |
| [MediaTypeFactory](../jmouse-core/src/main/java/org/jmouse/core/MediaTypeFactory.java) | 🎨 Factory for resolving MediaType instances from file extensions using a mime.types mapping. |
| [MediaTypeHelper](../jmouse-core/src/main/java/org/jmouse/core/MediaTypeHelper.java) | 🎨 Helper utilities for working with MediaTypes and multipart boundaries. |
| [MemoizedFunction](../jmouse-core/src/main/java/org/jmouse/core/MemoizedFunction.java) | Memoizing Function with thread-safe caching. 🧩 |
| [MethodAccessorFactory](../jmouse-core/src/main/java/org/jmouse/core/MethodAccessorFactory.java) | Turns a Method into a Getter or a Setter that calls it directly. ⚡ |
| [MethodParameter](../jmouse-core/src/main/java/org/jmouse/core/MethodParameter.java) | 🧩 Represents a method or constructor parameter with associated metadata. |
| [MimeParser](../jmouse-core/src/main/java/org/jmouse/core/MimeParser.java) | Utility class for parsing MIME type strings into MimeType instances. |
| [MimeType](../jmouse-core/src/main/java/org/jmouse/core/MimeType.java) | Represents a MIME media type, consisting of a primary type, subtype, and optional parameters. |
| [MultipartBoundaryGenerator](../jmouse-core/src/main/java/org/jmouse/core/MultipartBoundaryGenerator.java) | 🧩 Generator for multipart/form-data boundaries. |
| [NullSafeSupplier](../jmouse-core/src/main/java/org/jmouse/core/NullSafeSupplier.java) | A Supplier wrapper that ensures a null-safe value retrieval. |
| [Ordered](../jmouse-core/src/main/java/org/jmouse/core/Ordered.java) | A functional interface representing an entity with an order or priority. Implementations can specify their relative order by overriding the #order() method. |
| [ParameterizedSupplier](../jmouse-core/src/main/java/org/jmouse/core/ParameterizedSupplier.java) | Supplier that accepts a single argument. 🧩 |
| [PlaceholderReplacer](../jmouse-core/src/main/java/org/jmouse/core/PlaceholderReplacer.java) | Defines a contract for replacing placeholders in strings. |
| [PlaceholderResolver](../jmouse-core/src/main/java/org/jmouse/core/PlaceholderResolver.java) | Resolves placeholders within strings. |
| [PrefixedIdGenerator](../jmouse-core/src/main/java/org/jmouse/core/PrefixedIdGenerator.java) | Implements/extends `AbstractStringIdGenerator`. Operations: `defaultGenerator`, `prefixedGenerator`, `generate`. |
| [Priority](../jmouse-core/src/main/java/org/jmouse/core/Priority.java) | Indicates a priority value for beans or structured-producing methods. |
| [QuadFunction](../jmouse-core/src/main/java/org/jmouse/core/QuadFunction.java) | Operations: `apply`, `return`. |
| [RandomStringGenerator](../jmouse-core/src/main/java/org/jmouse/core/RandomStringGenerator.java) | 🎲 Abstract base class for generating random alphanumeric strings. |
| [RegexPlaceholderReplacer](../jmouse-core/src/main/java/org/jmouse/core/RegexPlaceholderReplacer.java) | Implements/extends `PlaceholderReplacer`. Operations: `replace`, `suffix`, `prefix`. |
| [Safe](../jmouse-core/src/main/java/org/jmouse/core/Safe.java) | Null-safe helper operations for common framework scenarios. 🛡️ |
| [SafeFunction](../jmouse-core/src/main/java/org/jmouse/core/SafeFunction.java) | Operations: `apply`. |
| [SafeHashStringGenerator](../jmouse-core/src/main/java/org/jmouse/core/SafeHashStringGenerator.java) | Thread-safe hash-based String ID generator using a ThreadLocal MessageDigest. |
| [SecureRandomStringGenerator](../jmouse-core/src/main/java/org/jmouse/core/SecureRandomStringGenerator.java) | 🔐 Secure random string generator. |
| [Setter](../jmouse-core/src/main/java/org/jmouse/core/Setter.java) | A functional interface representing a setter method for assigning a value to an instance. |
| [Sha256HashGenerator](../jmouse-core/src/main/java/org/jmouse/core/Sha256HashGenerator.java) | SHA-256-based String ID/hash generator. |
| [SimpleRandomStringGenerator](../jmouse-core/src/main/java/org/jmouse/core/SimpleRandomStringGenerator.java) | 🎲 Simple (non-secure) random string generator. |
| [SingletonFactories](../jmouse-core/src/main/java/org/jmouse/core/SingletonFactories.java) | Factory utilities for singleton and memoized functions. 🧩 |
| [SingletonFactory](../jmouse-core/src/main/java/org/jmouse/core/SingletonFactory.java) | Lazily creates and caches a single instance using an initialization argument. 🧩 |
| [SingletonSupplier](../jmouse-core/src/main/java/org/jmouse/core/SingletonSupplier.java) | A Supplier implementation that ensures a value is initialized only once (lazy singleton). |
| [Sorter](../jmouse-core/src/main/java/org/jmouse/core/Sorter.java) | A utility class for sorting objects according to a priority scheme. The priority may be obtained from an annotation (i.e., @Priority) on a class or from the Ordered interface if the structured implements it. If no priority information is available, objects are sorted to the end (i.e., Integer.MAX_VALUE), and ties are further resolved by comparing hash codes. |
| [StandardPlaceholderReplacer](../jmouse-core/src/main/java/org/jmouse/core/StandardPlaceholderReplacer.java) | A standard implementation of PlaceholderReplacer that replaces placeholders in a given string using a specified prefix, suffix, and separator. |
| [Streamable](../jmouse-core/src/main/java/org/jmouse/core/Streamable.java) | 🌀 Combines Iterable and Stream. |
| [StreamHelper](../jmouse-core/src/main/java/org/jmouse/core/StreamHelper.java) | 💾 Stream utility helper. |
| [URIType](../jmouse-core/src/main/java/org/jmouse/core/URIType.java) | 🧭 RFC 3986 component-aware character policy + encoder. |
| [UUIDGenerator](../jmouse-core/src/main/java/org/jmouse/core/UUIDGenerator.java) | Implements/extends `AbstractStringIdGenerator`. Operations: `generate`. |
| [Values](../jmouse-core/src/main/java/org/jmouse/core/Values.java) | Implements/extends `Streamable`. Operations: `get`, `size`, `iterator`, `toString`, `Basic`. |
| [Verify](../jmouse-core/src/main/java/org/jmouse/core/Verify.java) | Centralized contract enforcement utility. |
| [Visitor](../jmouse-core/src/main/java/org/jmouse/core/Visitor.java) | A generic Visitor interface that defines operations for handling elements of type T. This interface can be implemented by classes that need to traverse or process elements, deciding how to handle familiar or unfamiliar elements, perform actions, and forget elements. |
| [WrappedException](../jmouse-core/src/main/java/org/jmouse/core/WrappedException.java) | Implements/extends `RuntimeException`. |

#### `org.jmouse.core.access`

| Type | Responsibility / contract |
|---|---|
| [AbstractAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/AbstractAccessor.java) | An abstract base implementation of ObjectAccessor. |
| [AbstractAccessorWrapper](../jmouse-core/src/main/java/org/jmouse/core/access/AbstractAccessorWrapper.java) | A flexible factory for creating ObjectAccessor instances. |
| [AbstractBeanAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/AbstractBeanAccessor.java) | A ObjectAccessor implementation for accessing properties of a structured instance. |
| [AccessorWrapper](../jmouse-core/src/main/java/org/jmouse/core/access/AccessorWrapper.java) | A factory interface for creating ObjectAccessor instances. |
| [AttributeResolver](../jmouse-core/src/main/java/org/jmouse/core/access/AttributeResolver.java) | Resolves custom or virtual attributes on objects during expression evaluation. |
| [Bean](../jmouse-core/src/main/java/org/jmouse/core/access/Bean.java) | Represents a generic structured model that encapsulates metadata and properties of a Java type. |
| [BeanPropertyNotFound](../jmouse-core/src/main/java/org/jmouse/core/access/BeanPropertyNotFound.java) | Implements/extends `RuntimeException`. |
| [DefaultPropertyValueResolver](../jmouse-core/src/main/java/org/jmouse/core/access/DefaultPropertyValueResolver.java) | A default implementation of PropertyValueResolver that resolves property values using an underlying ObjectAccessor and, optionally, a VirtualPropertyResolver for virtual (computed) properties. |
| [DirectPropertyAccess](../jmouse-core/src/main/java/org/jmouse/core/access/DirectPropertyAccess.java) | Direct implementation of a PropertyAccessor. |
| [FastValueNavigator](../jmouse-core/src/main/java/org/jmouse/core/access/FastValueNavigator.java) | Optimized ValueNavigator implementation. ⚡ |
| [JavaBean](../jmouse-core/src/main/java/org/jmouse/core/access/JavaBean.java) | Represents a JavaBean-based model that encapsulates metadata and properties of a standard Java object. |
| [LazyPropertyAccess](../jmouse-core/src/main/java/org/jmouse/core/access/LazyPropertyAccess.java) | Lazy implementation of a PropertyAccessor. |
| [ObjectAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/ObjectAccessor.java) | Represents a data source abstraction that provides methods for accessing and transforming data. |
| [ObjectAccessorProvider](../jmouse-core/src/main/java/org/jmouse/core/access/ObjectAccessorProvider.java) | Defines a valueProvider for creating ObjectAccessor instances. |
| [ObjectAccessorWrapper](../jmouse-core/src/main/java/org/jmouse/core/access/ObjectAccessorWrapper.java) | A standard wrapper for ObjectAccessor that aggregates multiple ObjectAccessorProvider implementations. This class extends AbstractAccessorWrapper and provides a pre‐configured list of providers that support various data source types including scalar values, collections, maps, property value resolvers, Java Beans, records, and property resolvers. The default constructor initializes the wrapper with the following providers: |
| [PropertyAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/PropertyAccessor.java) | Defines an interface for accessing and modifying an object's property. |
| [PropertyPath](../jmouse-core/src/main/java/org/jmouse/core/access/PropertyPath.java) | Represents a structured path name that can be used to navigate and manipulate hierarchical properties. This class provides lexer, merging, and transformation capabilities for structured property paths. |
| [PropertyValueResolver](../jmouse-core/src/main/java/org/jmouse/core/access/PropertyValueResolver.java) | A high‑level abstraction for resolving property values from an object. |
| [TypedValue](../jmouse-core/src/main/java/org/jmouse/core/access/TypedValue.java) | Represents a bindable value with an associated InferredType. |
| [UnsupportedOperationException](../jmouse-core/src/main/java/org/jmouse/core/access/UnsupportedOperationException.java) | Implements/extends `RuntimeException`. |
| [ValueNavigator](../jmouse-core/src/main/java/org/jmouse/core/access/ValueNavigator.java) | Strategy interface for resolving values from an ObjectAccessor using textual or structured property paths. 🧭 |
| [ValueObject](../jmouse-core/src/main/java/org/jmouse/core/access/ValueObject.java) | Represents a wrapper for Java records, allowing for structured value retrieval and instantiation. |
| [VirtualProperty](../jmouse-core/src/main/java/org/jmouse/core/access/VirtualProperty.java) | Represents a virtual property that can be dynamically computed or derived from an object of type T. |
| [VirtualPropertyResolver](../jmouse-core/src/main/java/org/jmouse/core/access/VirtualPropertyResolver.java) | Defines an interface for resolving virtual properties for a given object instance. |

#### `org.jmouse.core.access.accessor`

| Type | Responsibility / contract |
|---|---|
| [ArrayAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/ArrayAccessor.java) | Implements/extends `AbstractAccessor`. Operations: `get`, `set`. |
| [ArrayAccessorProvider](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/ArrayAccessorProvider.java) | Implements/extends `ObjectAccessorProvider`. Operations: `supports`, `create`. |
| [CollectionAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/CollectionAccessor.java) | A ObjectAccessor implementation that provides indexed access to collections and maps. |
| [CollectionAccessorProvider](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/CollectionAccessorProvider.java) | Implements/extends `ObjectAccessorProvider`. Operations: `supports`, `create`. |
| [DummyObjectAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/DummyObjectAccessor.java) | Implements/extends `AbstractAccessor`. Operations: `get`, `set`. |
| [JavaBeanAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/JavaBeanAccessor.java) | A ObjectAccessor implementation for accessing properties of a structured instance. |
| [JavaBeanAccessorProvider](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/JavaBeanAccessorProvider.java) | An ObjectAccessorProvider implementation that supports Java Bean objects. |
| [MapAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/MapAccessor.java) | A ObjectAccessor implementation that provides indexed access to collections and maps. |
| [MapAccessorProvider](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/MapAccessorProvider.java) | An ObjectAccessorProvider implementation that supports Map-based data sources. |
| [NullObjectAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/NullObjectAccessor.java) | Implements/extends `AbstractAccessor`. Operations: `get`, `set`, `unwrap`, `asObject`, `getDataType`. |
| [PropertyResolverAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/PropertyResolverAccessor.java) | A ObjectAccessor implementation that wraps a PropertyResolver, converting its properties into a hierarchical structure. |
| [PropertyResolverAccessorProvider](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/PropertyResolverAccessorProvider.java) | Implements/extends `ObjectAccessorProvider`. Operations: `supports`, `create`. |
| [PropertyValueResolverAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/PropertyValueResolverAccessor.java) | An ObjectAccessor implementation that wraps a PropertyValueResolver. |
| [PropertyValueResolverAccessorProvider](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/PropertyValueResolverAccessorProvider.java) | An ObjectAccessorProvider implementation for sources that implement PropertyValueResolver. This valueProvider checks if the given source is a PropertyValueResolver and creates an appropriate ObjectAccessor to wrap it. |
| [RecordAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/RecordAccessor.java) | An implementation of an ObjectAccessor specifically for value objects. |
| [RecordAccessorProvider](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/RecordAccessorProvider.java) | Implements/extends `ObjectAccessorProvider`. Operations: `supports`, `create`. |
| [ScalarValueAccessor](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/ScalarValueAccessor.java) | Implements/extends `AbstractAccessor`. Operations: `get`, `set`. |
| [ScalarValueAccessorProvider](../jmouse-core/src/main/java/org/jmouse/core/access/accessor/ScalarValueAccessorProvider.java) | Implements/extends `ObjectAccessorProvider`. Operations: `supports`, `create`. |

#### `org.jmouse.core.access.data`

| Type | Responsibility / contract |
|---|---|
| [AbstractDataProvider](../jmouse-core/src/main/java/org/jmouse/core/access/data/AbstractDataProvider.java) | Base DataProvider implementation backed by a map. 🧩 |
| [DataProvider](../jmouse-core/src/main/java/org/jmouse/core/access/data/DataProvider.java) | Generic provider for key–value data access. 📦 |
| [EnumDataProvider](../jmouse-core/src/main/java/org/jmouse/core/access/data/EnumDataProvider.java) | DataProvider backed by enum constants. 🧩 |
| [MapDataProvider](../jmouse-core/src/main/java/org/jmouse/core/access/data/MapDataProvider.java) | DataProvider implementation backed by a Map. 🗂️ |

#### `org.jmouse.core.access.descriptor`

| Type | Responsibility / contract |
|---|---|
| [AbstractDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/AbstractDescriptor.java) | Implements/extends `Introspector > implements Descriptor`. Operations: `getName`, `unwrap`. |
| [AbstractIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/AbstractIntrospector.java) | Provides an abstract base implementation for introspects, encapsulating common functionality such as naming and descriptor caching mechanisms. |
| [AnnotatedElementDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/AnnotatedElementDescriptor.java) | Represents a descriptor for annotated Java elements, such as classes, methods, fields, or constructors. |
| [AnnotatedElementIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/AnnotatedElementIntrospector.java) | Implements/extends `AnnotatedElementIntrospector , E extends AnnotatedElement, D extends AnnotatedElementDescriptor > extends AbstractIntrospector`. Operations: `annotation`, `annotations`. |
| [AnnotationDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/AnnotationDescriptor.java) | 📝 Descriptor wrapping an Annotation instance with introspection support. |
| [AnnotationIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/AnnotationIntrospector.java) | Implements/extends `AbstractIntrospector`. Operations: `name`, `introspect`, `annotationType`, `attributes`, `toDescriptor`, `getContainerFor`. |
| [ClassTypeDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/ClassTypeDescriptor.java) | Implements/extends `AnnotatedElementDescriptor , ClassTypeData, ClassTypeIntrospector> implements TypeClassifier`. Operations: `getJavaType`, `getClassType`, `getConstructors`, `getConstructor`, `getMethods`, `getMethod`, `hasMethod`, `getFields`, `getField`, `hasField`, `toIntrospector`, `toString`. |
| [ClassTypeIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/ClassTypeIntrospector.java) | Introspects Java class types, including constructors, fields, and methods, collecting detailed metadata through reflection. |
| [ConstructorDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/ConstructorDescriptor.java) | Implements/extends `ExecutableDescriptor , ConstructorData, ConstructorIntrospector>`. Operations: `toIntrospector`. |
| [ConstructorIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/ConstructorIntrospector.java) | Implements/extends `ExecutableIntrospector , ConstructorDescriptor>`. Operations: `introspect`, `toDescriptor`, `getContainerFor`. |
| [Describer](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/Describer.java) | Operations: `forParameter`, `forMethod`, `forClass`, `className`, `forJavaBean`, `forValueObject`, `forObjectDescriptor`. |
| [Descriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/Descriptor.java) | Implements/extends `Introspector >`. Operations: `getName`, `unwrap`, `toIntrospector`. |
| [DescriptorProperty](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/DescriptorProperty.java) | Annotation contract; members and defaults are defined in this source. |
| [ExecutableDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/ExecutableDescriptor.java) | Represents a descriptor for an executable element, such as a method or constructor. This class provides metadata descriptor capabilities for executable elements, including parameter descriptors and exception types. |
| [ExecutableIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/ExecutableIntrospector.java) | Provides an abstraction for introspecting Executable elements, including methods and constructors. This class is responsible for analyzing metadata such as annotations, parameters, and method names. |
| [FieldDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/FieldDescriptor.java) | Implements/extends `AnnotatedElementDescriptor`. Operations: `toIntrospector`. |
| [FieldIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/FieldIntrospector.java) | Performs descriptor of Java class fields, capturing metadata such as name, annotations, and type information. |
| [Introspector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/Introspector.java) | Defines a contract for introspecting data containers and generating metadata descriptors. |
| [MethodDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/MethodDescriptor.java) | 🔎 Descriptor for Method metadata. |
| [MethodIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/MethodIntrospector.java) | Implements/extends `ExecutableIntrospector`. Operations: `returnType`, `introspect`, `toDescriptor`, `getContainerFor`. |
| [ParameterDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/ParameterDescriptor.java) | Implements/extends `AnnotatedElementDescriptor`. Operations: `getType`, `getExecutable`, `toIntrospector`, `toString`. |
| [ParameterIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/ParameterIntrospector.java) | Implements/extends `AnnotatedElementIntrospector`. Operations: `name`, `type`, `executable`, `introspect`, `toDescriptor`, `getContainerFor`. |
| [PreferredPropertyName](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/PreferredPropertyName.java) | Annotation contract; members and defaults are defined in this source. |

#### `org.jmouse.core.access.descriptor.internal`

| Type | Responsibility / contract |
|---|---|
| [AbstractDataContainer](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/internal/AbstractDataContainer.java) | A base implementation of DataContainer that provides common functionality for managing a data target and its associated name. |
| [AnnotatedElementData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/internal/AnnotatedElementData.java) | Implements/extends `AbstractDataContainer`. Operations: `getAnnotations`, `addAnnotation`, `removeAnnotation`. |
| [AnnotationData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/internal/AnnotationData.java) | Implements/extends `AbstractDataContainer`. Operations: `getAnnotationType`, `setAnnotationType`, `getAttributes`, `getAttribute`, `setAttributes`, `addAttribute`. |
| [ClassTypeData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/internal/ClassTypeData.java) | Implements/extends `AnnotatedElementData >`. Operations: `getType`, `setType`, `getConstructors`, `getFields`, `getMethods`, `addConstructor`, `addField`, `addMethod`, `clearConstructors`, `clearFields`, `clearMethods`. |
| [ConstructorData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/internal/ConstructorData.java) | Implements/extends `ExecutableData >`. |
| [DataContainer](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/internal/DataContainer.java) | Operations: `getName`, `setName`, `getTarget`, `setTarget`. |
| [ExecutableData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/internal/ExecutableData.java) | Implements/extends `AnnotatedElementData`. Operations: `getParameters`, `getParameter`, `addParameter`, `addExceptionType`, `getExceptionTypes`. |
| [FieldData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/internal/FieldData.java) | Implements/extends `AnnotatedElementData`. Operations: `getType`, `setType`. |
| [MethodData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/internal/MethodData.java) | Holds introspected metadata about a specific Java Method. |
| [ParameterData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/internal/ParameterData.java) | Container for metadata related to a method or constructor parameter. Extends AnnotatedElementData to store annotations and type information. |

#### `org.jmouse.core.access.descriptor.structured`

| Type | Responsibility / contract |
|---|---|
| [DescriptorResolver](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/DescriptorResolver.java) | Where a type's description is looked up, and where it is built exactly once. 🗃️ |
| [ObjectData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/ObjectData.java) | Implements/extends `AbstractDataContainer implements ObjectDescriptor`. Operations: `getType`, `setType`, `getProperties`. |
| [ObjectDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/ObjectDescriptor.java) | Represents a descriptor for an object that provides structured metadata about its properties. |
| [ObjectDescriptorType](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/ObjectDescriptorType.java) | Enum values: `JB`, `VO`, `MAP`. |
| [ObjectIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/ObjectIntrospector.java) | Operations: `getPreferredPropertyName`. |
| [PropertyData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/PropertyData.java) | Implements/extends `AbstractDataContainer implements PropertyDescriptor`. Operations: `getGetter`, `setGetter`, `getSetter`, `setSetter`, `getGetterMethod`, `setGetterMethod`, `getSetterMethod`, `setSetterMethod`, `getType`, `setType`, `getOwner`, `setOwner`. |
| [PropertyDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/PropertyDescriptor.java) | Represents a property descriptor that provides metadata and access mechanisms for a property within an ObjectDescriptor. |

#### `org.jmouse.core.access.descriptor.structured.bean`

| Type | Responsibility / contract |
|---|---|
| [JavaBeanDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/bean/JavaBeanDescriptor.java) | Represents a descriptor for a JavaBean, providing metadata about its structure and properties. |
| [JavaBeanIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/bean/JavaBeanIntrospector.java) | An introspector for Java Beans, allowing reflection-based analysis of their structure. |
| [JavaBeanPropertyDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/bean/JavaBeanPropertyDescriptor.java) | Represents a descriptor for a JavaBean property, providing metadata about its getter, setter, type, and owner object. |
| [JavaBeanPropertyIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/bean/JavaBeanPropertyIntrospector.java) | Introspector for JavaBean properties, allowing analysis and metadata extraction. |

#### `org.jmouse.core.access.descriptor.structured.map`

| Type | Responsibility / contract |
|---|---|
| [MapDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/map/MapDescriptor.java) | A descriptor for a Map structure that provides metadata about its key-value pairs. |
| [MapIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/map/MapIntrospector.java) | An introspector for analyzing Map structures. |
| [MapPropertyData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/map/MapPropertyData.java) | Represents structured property data for a Map entry. |
| [MapPropertyDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/map/MapPropertyDescriptor.java) | Represents a descriptor for a map property, encapsulating metadata about the key and value types, accessors, and ownership. |
| [MapPropertyIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/map/MapPropertyIntrospector.java) | Introspector for analyzing and describing properties of a Map entry. |

#### `org.jmouse.core.access.descriptor.structured.record`

| Type | Responsibility / contract |
|---|---|
| [ValueObjectData](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/record/ValueObjectData.java) | Implements/extends `ObjectData`. Operations: `getConstructor`, `setConstructor`, `addComponent`, `getComponents`. |
| [ValueObjectDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/record/ValueObjectDescriptor.java) | Implements/extends `AbstractDescriptor , ValueObjectIntrospector > implements TypeClassifier, ObjectDescriptor`. Operations: `toIntrospector`, `getType`, `setType`, `getProperties`, `getClassType`, `getComponents`, `toString`. |
| [ValueObjectIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/record/ValueObjectIntrospector.java) | Implements/extends `AbstractIntrospector , ValueObjectIntrospector , T, ValueObjectDescriptor >`. Operations: `type`, `constructor`, `properties`, `property`, `component`, `name`, `introspect`, `toDescriptor`, `getContainerFor`. |
| [ValueObjectPropertyDescriptor](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/record/ValueObjectPropertyDescriptor.java) | Implements/extends `AbstractDescriptor , ValueObjectPropertyIntrospector > implements PropertyDescriptor`. Operations: `toIntrospector`, `getType`, `setType`, `getOwner`, `getGetter`, `getGetterMethod`, `setGetter`, `getSetter`, `setSetter`, `isWritable`, `toString`. |
| [ValueObjectPropertyIntrospector](../jmouse-core/src/main/java/org/jmouse/core/access/descriptor/structured/record/ValueObjectPropertyIntrospector.java) | Implements/extends `AbstractIntrospector , ValueObjectPropertyIntrospector , T, ValueObjectPropertyDescriptor >`. Operations: `name`, `type`, `owner`, `getter`, `getterMethod`, `introspect`, `toDescriptor`, `getContainerFor`. |

#### `org.jmouse.core.annotation`

| Type | Responsibility / contract |
|---|---|
| [AnnotationBootstrapper](../jmouse-core/src/main/java/org/jmouse/core/annotation/AnnotationBootstrapper.java) | Bootstraps annotation processors against scan roots. 🚀 |
| [AnnotationCandidate](../jmouse-core/src/main/java/org/jmouse/core/annotation/AnnotationCandidate.java) | Describes a discovered annotated element. 🔎 |
| [AnnotationDiscovery](../jmouse-core/src/main/java/org/jmouse/core/annotation/AnnotationDiscovery.java) | Discovers annotation candidates in the classpath. 🔍 |
| [AnnotationElementKind](../jmouse-core/src/main/java/org/jmouse/core/annotation/AnnotationElementKind.java) | Kind of annotated program element. 🧩 |
| [AnnotationProcessingContext](../jmouse-core/src/main/java/org/jmouse/core/annotation/AnnotationProcessingContext.java) | Shared context used during annotation processing. ⚙️ |
| [AnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/annotation/AnnotationProcessor.java) | Strategy for processing discovered annotation candidates. 🎯 |
| [FieldAnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/annotation/FieldAnnotationProcessor.java) | AnnotationProcessor specialization for field-level annotations. 🧷 |
| [MethodAnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/annotation/MethodAnnotationProcessor.java) | AnnotationProcessor specialization for method-level annotations. 🔧 |
| [ParameterAnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/annotation/ParameterAnnotationProcessor.java) | Processor for parameter-level annotations. 🎯 |
| [TypeAnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/annotation/TypeAnnotationProcessor.java) | AnnotationProcessor specialization for type-level annotations. 🏷️ |

#### `org.jmouse.core.annotation.support`

| Type | Responsibility / contract |
|---|---|
| [AbstractAnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/annotation/support/AbstractAnnotationProcessor.java) | Base implementation of AnnotationProcessor. 🧩 |
| [AbstractFieldAnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/annotation/support/AbstractFieldAnnotationProcessor.java) | Base support class for FieldAnnotationProcessor implementations. 🧷 |
| [AbstractMethodAnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/annotation/support/AbstractMethodAnnotationProcessor.java) | Base support class for MethodAnnotationProcessor implementations. 🔧 |
| [AbstractParameterAnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/annotation/support/AbstractParameterAnnotationProcessor.java) | Base implementation for ParameterAnnotationProcessor. 🧩 |
| [AbstractTypeAnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/annotation/support/AbstractTypeAnnotationProcessor.java) | Base support class for TypeAnnotationProcessor implementations. 🏷️ |

#### `org.jmouse.core.binding`

| Type | Responsibility / contract |
|---|---|
| [AbstractBinder](../jmouse-core/src/main/java/org/jmouse/core/binding/AbstractBinder.java) | Base implementation of the ObjectBinder interface. |
| [AbstractCallback](../jmouse-core/src/main/java/org/jmouse/core/binding/AbstractCallback.java) | Abstract base class for binding callbacks. |
| [ArrayBinder](../jmouse-core/src/main/java/org/jmouse/core/binding/ArrayBinder.java) | ArrayBinder is a specific implementation of CollectionBinder that handles binding for array types. This binder supports binding of arrays by first converting them into a list, binding the individual elements, and then converting the list back into an array. |
| [Bind](../jmouse-core/src/main/java/org/jmouse/core/binding/Bind.java) | A utility class for binding data from a ObjectAccessor to Java objects. Provides convenient factory methods and type-safe bindings for common types. |
| [BindCallback](../jmouse-core/src/main/java/org/jmouse/core/binding/BindCallback.java) | A callback interface for handling various stages of the binding process. |
| [BindContext](../jmouse-core/src/main/java/org/jmouse/core/binding/BindContext.java) | Provides the context for data binding operations. This interface encapsulates the binding environment, including data sources, type conversion, and binding depth settings. |
| [BindDefault](../jmouse-core/src/main/java/org/jmouse/core/binding/BindDefault.java) | Annotation contract; members and defaults are defined in this source. |
| [Binder](../jmouse-core/src/main/java/org/jmouse/core/binding/Binder.java) | The Binder class is responsible for managing the binding process, allowing for flexible object binding strategies. It handles recursive reference detection and provides various binding methods for different data types. |
| [BinderConversion](../jmouse-core/src/main/java/org/jmouse/core/binding/BinderConversion.java) | Implements/extends `PredefinedConversion`. |
| [BinderFactory](../jmouse-core/src/main/java/org/jmouse/core/binding/BinderFactory.java) | Factory for managing and providing ObjectBinder instances. This interface allows dynamic registration, retrieval, and removal of binders. |
| [BindException](../jmouse-core/src/main/java/org/jmouse/core/binding/BindException.java) | Exception thrown when a binding operation fails. |
| [BindingStrategy](../jmouse-core/src/main/java/org/jmouse/core/binding/BindingStrategy.java) | Enum values: `SHALLOW`, `DEEP`. |
| [BindName](../jmouse-core/src/main/java/org/jmouse/core/binding/BindName.java) | Annotation contract; members and defaults are defined in this source. |
| [BindRequired](../jmouse-core/src/main/java/org/jmouse/core/binding/BindRequired.java) | Annotation contract; members and defaults are defined in this source. |
| [BindResult](../jmouse-core/src/main/java/org/jmouse/core/binding/BindResult.java) | The BindResult class represents the result of a binding operation. |
| [CollectionBinder](../jmouse-core/src/main/java/org/jmouse/core/binding/CollectionBinder.java) | The CollectionBinder class is an abstract binder for handling collection types. It binds each element of a collection in a recursive manner, ensuring that each element is processed individually based on the provided path. |
| [DefaultBinderFactory](../jmouse-core/src/main/java/org/jmouse/core/binding/DefaultBinderFactory.java) | A default implementation of BinderFactory. |
| [DefaultBindingCallback](../jmouse-core/src/main/java/org/jmouse/core/binding/DefaultBindingCallback.java) | DirectAccess implementation of a binding callback used in property binding operations. |
| [JavaBeanBinder](../jmouse-core/src/main/java/org/jmouse/core/binding/JavaBeanBinder.java) | A binder implementation that supports binding JavaBeans. |
| [ListBinder](../jmouse-core/src/main/java/org/jmouse/core/binding/ListBinder.java) | ListBinder is a specific implementation of CollectionBinder that handles binding for List types. This binder supports binding of List collections, allowing individual elements to be bound to their respective paths and added to the list. |
| [MapBinder](../jmouse-core/src/main/java/org/jmouse/core/binding/MapBinder.java) | A binder for Map types, extending the AbstractBinder class. |
| [ObjectBinder](../jmouse-core/src/main/java/org/jmouse/core/binding/ObjectBinder.java) | Defines the contract for binding objects from a ObjectAccessor. Implementations of this interface are responsible for mapping data from a source to a target bindable type, optionally applying a BindCallback. |
| [ScalarValueBinder](../jmouse-core/src/main/java/org/jmouse/core/binding/ScalarValueBinder.java) | A binder responsible for binding scalar values such as String, Number, Boolean, and Character. |
| [SetBinder](../jmouse-core/src/main/java/org/jmouse/core/binding/SetBinder.java) | SetBinder is a specific implementation of CollectionBinder that handles binding for Set types. This binder supports binding of Set collections by providing a supplier to create a new LinkedHashSet and ensuring that the bindable type is compatible with a Set. |
| [ValueObjectBinder](../jmouse-core/src/main/java/org/jmouse/core/binding/ValueObjectBinder.java) | Binder for Java records, enabling their instantiation and property binding from a data source. |

#### `org.jmouse.core.cache`

| Type | Responsibility / contract |
|---|---|
| [BasicCache](../jmouse-core/src/main/java/org/jmouse/core/cache/BasicCache.java) | 🧰 Minimal cache API: get/put/remove/size/clear. |
| [CountMinSketch](../jmouse-core/src/main/java/org/jmouse/core/cache/CountMinSketch.java) | 📊 Count–Min Sketch (CMS) with a power-of-two width and a practical working depth of 4. |
| [CountMinSketchFrequencyEstimator](../jmouse-core/src/main/java/org/jmouse/core/cache/CountMinSketchFrequencyEstimator.java) | 🎛️ Adapter for CountMinSketch implementing FrequencyEstimator. |
| [Doorkeeper](../jmouse-core/src/main/java/org/jmouse/core/cache/Doorkeeper.java) | 🚪 Doorkeeper : a tiny Bloom-like front filter for TinyLFU. |
| [DoorkeeperSeenFilter](../jmouse-core/src/main/java/org/jmouse/core/cache/DoorkeeperSeenFilter.java) | 🧩 Adapter turning Doorkeeper into a SeenFilter. |
| [FrequencyEstimator](../jmouse-core/src/main/java/org/jmouse/core/cache/FrequencyEstimator.java) | 📊 Abstraction over a frequency sketch (e.g., Count–Min Sketch). |
| [KeyWrapper](../jmouse-core/src/main/java/org/jmouse/core/cache/KeyWrapper.java) | 🧱 Optional value wrapper to centralize hashing/equality semantics for keys. |
| [LRUCache](../jmouse-core/src/main/java/org/jmouse/core/cache/LRUCache.java) | ♻️ LRUCache — a thread-safe, bounded-size cache with Least Recently Used eviction policy. |
| [MaintainedCache](../jmouse-core/src/main/java/org/jmouse/core/cache/MaintainedCache.java) | 🧽 Cache that benefits from periodic maintenance (e.g., CMS decay, doorkeeper reset). |
| [SeenFilter](../jmouse-core/src/main/java/org/jmouse/core/cache/SeenFilter.java) | 🚪 "Seen-before" filter that suppresses one-hit noise. |
| [SegmentLRUCache](../jmouse-core/src/main/java/org/jmouse/core/cache/SegmentLRUCache.java) | 🔀 Segmented LRU (SLRU) cache with two segments: |
| [TinyLFU](../jmouse-core/src/main/java/org/jmouse/core/cache/TinyLFU.java) | 🧠 TinyLFU admission policy composed from a SeenFilter and a FrequencyEstimator. |
| [TinyLFUSLRUCache](../jmouse-core/src/main/java/org/jmouse/core/cache/TinyLFUSLRUCache.java) | 🔀 TinyLFU-over-SLRU cache. |
| [TTLCache](../jmouse-core/src/main/java/org/jmouse/core/cache/TTLCache.java) | 🕒 TTLCache — a thread-safe cache with per-entry time-to-live (TTL) expiration. |

#### `org.jmouse.core.chain`

| Type | Responsibility / contract |
|---|---|
| [Chain](../jmouse-core/src/main/java/org/jmouse/core/chain/Chain.java) | ⛓️ Chain-of-responsibility abstraction with explicit Outcome control. |
| [Chains](../jmouse-core/src/main/java/org/jmouse/core/chain/Chains.java) | 🧩 Utility helpers for working with Chain. |
| [Link](../jmouse-core/src/main/java/org/jmouse/core/chain/Link.java) | 🔗 Single element in a Chain (Chain of Responsibility). |
| [Links](../jmouse-core/src/main/java/org/jmouse/core/chain/Links.java) | 🔗 Factory methods for common Link implementations. |
| [Outcome](../jmouse-core/src/main/java/org/jmouse/core/chain/Outcome.java) | 🎯 Result of a chain step: either continue evaluation or stop with a final value. |

#### `org.jmouse.core.context`

| Type | Responsibility / contract |
|---|---|
| [ArgumentsContext](../jmouse-core/src/main/java/org/jmouse/core/context/ArgumentsContext.java) | 🎯 Arguments-oriented context view. |
| [AttributesContext](../jmouse-core/src/main/java/org/jmouse/core/context/AttributesContext.java) | 🧩 Attributes-oriented context view. |
| [ContextKey](../jmouse-core/src/main/java/org/jmouse/core/context/ContextKey.java) | 🗝️ Strongly-typed key for ExecutionContext. |
| [ContextScope](../jmouse-core/src/main/java/org/jmouse/core/context/ContextScope.java) | Thread-local scope holder for contextual values. 🧵 |
| [KeyValueContext](../jmouse-core/src/main/java/org/jmouse/core/context/KeyValueContext.java) | Read-only key-value context contract. Keys are typically ContextKey or Class, but any object may be used. |
| [ProcessingControl](../jmouse-core/src/main/java/org/jmouse/core/context/ProcessingControl.java) | 🛑 Processing control flag. |
| [VariablesContext](../jmouse-core/src/main/java/org/jmouse/core/context/VariablesContext.java) | 🧮 Variables-oriented context view. |

#### `org.jmouse.core.context.beans`

| Type | Responsibility / contract |
|---|---|
| [BeanLookup](../jmouse-core/src/main/java/org/jmouse/core/context/beans/BeanLookup.java) | 🫘 Bean lookup contract. |
| [BeanLookupContext](../jmouse-core/src/main/java/org/jmouse/core/context/beans/BeanLookupContext.java) | 🫘 Bean lookup contract for contexts. |
| [MissingBeanLookupException](../jmouse-core/src/main/java/org/jmouse/core/context/beans/MissingBeanLookupException.java) | Implements/extends `IllegalStateException`. |

#### `org.jmouse.core.context.errors`

| Type | Responsibility / contract |
|---|---|
| [ContextValueTypeMismatchException](../jmouse-core/src/main/java/org/jmouse/core/context/errors/ContextValueTypeMismatchException.java) | Implements/extends `RuntimeException`. |
| [MissingContextValueException](../jmouse-core/src/main/java/org/jmouse/core/context/errors/MissingContextValueException.java) | Implements/extends `RuntimeException`. |

#### `org.jmouse.core.context.execution`

| Type | Responsibility / contract |
|---|---|
| [DefaultExecutionContext](../jmouse-core/src/main/java/org/jmouse/core/context/execution/DefaultExecutionContext.java) | 🧱 Default immutable implementation of ExecutionContext. |
| [Execution](../jmouse-core/src/main/java/org/jmouse/core/context/execution/Execution.java) | 🎯 Convenience utility for executing code within a temporary execution context. |
| [ExecutionContext](../jmouse-core/src/main/java/org/jmouse/core/context/execution/ExecutionContext.java) | 🧩 Immutable execution context abstraction. |
| [ExecutionContextHolder](../jmouse-core/src/main/java/org/jmouse/core/context/execution/ExecutionContextHolder.java) | 🧭 Thread-bound holder for the current ExecutionContext. |
| [ExecutionSnapshot](../jmouse-core/src/main/java/org/jmouse/core/context/execution/ExecutionSnapshot.java) | 📦 Immutable snapshot of an ExecutionContext. |

#### `org.jmouse.core.context.immutable`

| Type | Responsibility / contract |
|---|---|
| [ImmutableSnapshotContext](../jmouse-core/src/main/java/org/jmouse/core/context/immutable/ImmutableSnapshotContext.java) | 🧊 Immutable snapshot of a key-value context. |

#### `org.jmouse.core.context.keys`

| Type | Responsibility / contract |
|---|---|
| [ContextKeyRegistry](../jmouse-core/src/main/java/org/jmouse/core/context/keys/ContextKeyRegistry.java) | Operations: `findKeyForValue`, `findKeyForUserClass`, `UserClassOnlyContextKeyRegistry`, `IdentityValueOnlyContextKeyRegistry`, `CompositeContextKeyRegistry`. |
| [ContextKeyResolver](../jmouse-core/src/main/java/org/jmouse/core/context/keys/ContextKeyResolver.java) | Context key resolution strategy. |
| [ContextKeyResolvers](../jmouse-core/src/main/java/org/jmouse/core/context/keys/ContextKeyResolvers.java) | 🧰 Context key resolution helpers. |
| [MutableContextKeyRegistry](../jmouse-core/src/main/java/org/jmouse/core/context/keys/MutableContextKeyRegistry.java) | Implements/extends `ContextKeyRegistry`. Operations: `registerForUserClass`, `registerForValue`. |
| [RegistryAwareContextKeyResolver](../jmouse-core/src/main/java/org/jmouse/core/context/keys/RegistryAwareContextKeyResolver.java) | Implements/extends `ContextKeyResolver`. Operations: `resolveKeyFor`. |
| [UserClassContextKeyResolver](../jmouse-core/src/main/java/org/jmouse/core/context/keys/UserClassContextKeyResolver.java) | User-class based context key resolver. |

#### `org.jmouse.core.context.mutable`

| Type | Responsibility / contract |
|---|---|
| [MapBackedMutableContext](../jmouse-core/src/main/java/org/jmouse/core/context/mutable/MapBackedMutableContext.java) | 🗂 Map-backed mutable key-value context. |
| [MutableArgumentsContext](../jmouse-core/src/main/java/org/jmouse/core/context/mutable/MutableArgumentsContext.java) | 🛠 Mutable arguments context. |
| [MutableAttributesContext](../jmouse-core/src/main/java/org/jmouse/core/context/mutable/MutableAttributesContext.java) | 🛠 Mutable attributes context. |
| [MutableKeyValueContext](../jmouse-core/src/main/java/org/jmouse/core/context/mutable/MutableKeyValueContext.java) | Implements/extends `KeyValueContext`. Operations: `setValue`, `removeValue`, `clear`, `IllegalArgumentException`. |
| [MutableProcessingControl](../jmouse-core/src/main/java/org/jmouse/core/context/mutable/MutableProcessingControl.java) | 🛠 Mutable processing control. |
| [MutableVariablesContext](../jmouse-core/src/main/java/org/jmouse/core/context/mutable/MutableVariablesContext.java) | 🛠 Mutable variables context. |
| [StandardMutableContextContainer](../jmouse-core/src/main/java/org/jmouse/core/context/mutable/StandardMutableContextContainer.java) | Typical mutable container that can be used in handlers/pipelines. |

#### `org.jmouse.core.context.result`

| Type | Responsibility / contract |
|---|---|
| [ErrorDetails](../jmouse-core/src/main/java/org/jmouse/core/context/result/ErrorDetails.java) | ❌ Error descriptor. |
| [MapBackedMutableResultContext](../jmouse-core/src/main/java/org/jmouse/core/context/result/MapBackedMutableResultContext.java) | 🗂 Map-backed mutable result context. |
| [MutableResultContext](../jmouse-core/src/main/java/org/jmouse/core/context/result/MutableResultContext.java) | 🛠 Mutable execution result context. |
| [ResultContext](../jmouse-core/src/main/java/org/jmouse/core/context/result/ResultContext.java) | 🎯 Execution result context. |

#### `org.jmouse.core.convert`

| Type | Responsibility / contract |
|---|---|
| [ClassPair](../jmouse-core/src/main/java/org/jmouse/core/convert/ClassPair.java) | A simple utility class that holds a pair of Class references, typically used to represent some kind of mapping or conversion from classA to classB. The ClassPair is parameterized to ensure type safety in scenarios like converters or adapters. |
| [Conversion](../jmouse-core/src/main/java/org/jmouse/core/convert/Conversion.java) | An extension of the ConverterFactory interface that provides convenient conversion methods in addition to managing a registry of converters. It allows both registration of converters and direct execution of conversions, making it a one-stop interface for all conversion-related operations. |
| [ConversionConfigurer](../jmouse-core/src/main/java/org/jmouse/core/convert/ConversionConfigurer.java) | A simple hook for configuring or extending a Conversion instance with custom logic or additional converters. Implementations can override #configureConversion(Conversion) to register new type converters, set default options, or otherwise influence the conversion process. |
| [ConversionException](../jmouse-core/src/main/java/org/jmouse/core/convert/ConversionException.java) | Implements/extends `RuntimeException`. |
| [Converter](../jmouse-core/src/main/java/org/jmouse/core/convert/Converter.java) | A functional interface representing a simple conversion operation from a source type S to a target type T. This interface is commonly used in contexts where objects of one type need to be transformed into another type, such as data mapping or serialization. |
| [ConverterFactory](../jmouse-core/src/main/java/org/jmouse/core/convert/ConverterFactory.java) | A factory interface for registering and retrieving Converter or GenericConverter instances. Implementations of this interface can maintain a registry of source-target type pairs, providing flexible and extensible type conversion capabilities. |
| [ConverterName](../jmouse-core/src/main/java/org/jmouse/core/convert/ConverterName.java) | A name a converter may be reached by, when the type pair is not enough to say which one is meant. |
| [ConverterNotFound](../jmouse-core/src/main/java/org/jmouse/core/convert/ConverterNotFound.java) | An exception that is thrown when no suitable converter is found for a given pair of source and target types. This indicates that the conversion process cannot proceed because a corresponding GenericConverter or Converter has not been registered for the required type pair. |
| [GenericConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/GenericConverter.java) | A generic interface for converting objects of type S into objects of type T. Implementations of this interface typically define a set of supported source-target type pairs and provide logic for performing the actual conversion. |
| [PredefinedConversion](../jmouse-core/src/main/java/org/jmouse/core/convert/PredefinedConversion.java) | Implements/extends `StandardConversion`. Operations: `convert`. |
| [StandardConversion](../jmouse-core/src/main/java/org/jmouse/core/convert/StandardConversion.java) | The default implementation of the Conversion interface. It manages a registry of GenericConverter instances mapped to specific ClassPairs, allowing for dynamic registration and retrieval of converters. This implementation uses a concurrent hash map to safely handle converters in multi-threaded environments. |
| [TypeNormalizer](../jmouse-core/src/main/java/org/jmouse/core/convert/TypeNormalizer.java) | A functional interface for normalizing types to a standardized or expected form. The #normalize(Class) method takes an input Class and returns a potentially modified or generalized version of that class. |

#### `org.jmouse.core.convert.converter`

| Type | Responsibility / contract |
|---|---|
| [BooleanToStringConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/BooleanToStringConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [BytesToStringConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/BytesToStringConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [CharacterToNumberConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/CharacterToNumberConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [CIDRToStringConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/CIDRToStringConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [CollectionConverters](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/CollectionConverters.java) | Operations: `getConverters`. |
| [CountryToStringConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/CountryToStringConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [DateAndTimeConverters](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/DateAndTimeConverters.java) | Operations: `getConverters`. |
| [JavaNetConverters](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/JavaNetConverters.java) | Operations: `getConverters`. |
| [MediaTypeToStringConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/MediaTypeToStringConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [NumberToCharacterConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/NumberToCharacterConverter.java) | A GenericConverter implementation that converts various numeric types (Number) into their corresponding Character representation. |
| [NumberToNumberConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/NumberToNumberConverter.java) | A GenericConverter implementation that handles conversions between various Number types, including both primitive and wrapper types. This converter facilitates transforming a given Number into another specific Number subtype by using predefined conversion functions. |
| [NumberToStringConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/NumberToStringConverter.java) | A GenericConverter implementation that converts various numeric types (Number) into their corresponding String representation. This converter is especially useful for formatting numeric values into human-readable text, logging, or serialization. |
| [ObjectArraysConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/ObjectArraysConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [PatternToStringConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/PatternToStringConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [StringToBooleanConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/StringToBooleanConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [StringToBytesConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/StringToBytesConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [StringToCIDRConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/StringToCIDRConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [StringToCountryConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/StringToCountryConverter.java) | 🌍 Converter from String to Country. |
| [StringToMediaTypeConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/StringToMediaTypeConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [StringToNumberConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/StringToNumberConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [StringToPatternConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/StringToPatternConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |

#### `org.jmouse.core.convert.converter.enums`

| Type | Responsibility / contract |
|---|---|
| [EnumToScalarConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/enums/EnumToScalarConverter.java) | A converter that maps Integer values to corresponding Enum constants. |
| [IntegerToEnumConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/enums/IntegerToEnumConverter.java) | A converter that maps Integer values to corresponding Enum constants. |
| [StringToEnumConverter](../jmouse-core/src/main/java/org/jmouse/core/convert/converter/enums/StringToEnumConverter.java) | A GenericConverter implementation that converts a String value into a corresponding Enum constant by matching names (case-insensitive). If no matching constant is found, this converter returns null. |

#### `org.jmouse.core.environment`

| Type | Responsibility / contract |
|---|---|
| [AbstractPropertyResolver](../jmouse-core/src/main/java/org/jmouse/core/environment/AbstractPropertyResolver.java) | Abstract base class for PropertyResolver implementations. |
| [AbstractPropertySource](../jmouse-core/src/main/java/org/jmouse/core/environment/AbstractPropertySource.java) | Abstract base class for PropertySource implementations. |
| [ClasspathPropertySource](../jmouse-core/src/main/java/org/jmouse/core/environment/ClasspathPropertySource.java) | A ResourcePropertySource that loads properties from a classpath resource. |
| [Environment](../jmouse-core/src/main/java/org/jmouse/core/environment/Environment.java) | Central interface for accessing properties and resolving placeholders. |
| [EnvironmentConfigurer](../jmouse-core/src/main/java/org/jmouse/core/environment/EnvironmentConfigurer.java) | Environment customization hook; its default configureEnvironment implementation performs no changes. |
| [JavaPropertiesPropertySource](../jmouse-core/src/main/java/org/jmouse/core/environment/JavaPropertiesPropertySource.java) | A PropertySource implementation that uses Properties as the underlying property source. |
| [MapPropertySource](../jmouse-core/src/main/java/org/jmouse/core/environment/MapPropertySource.java) | A PropertySource implementation that uses a Map as the underlying property source. |
| [PropertyNotFoundException](../jmouse-core/src/main/java/org/jmouse/core/environment/PropertyNotFoundException.java) | Implements/extends `RuntimeException`. |
| [PropertyResolver](../jmouse-core/src/main/java/org/jmouse/core/environment/PropertyResolver.java) | Operations: `getRawProperty`, `getProperty`, `PropertyNotFoundException`, `getPropertySource`, `addPropertySource`, `hasPropertySource`, `removePropertySource`, `getPropertySources`. |
| [PropertySource](../jmouse-core/src/main/java/org/jmouse/core/environment/PropertySource.java) | 📦 Property source abstraction. Used to resolve config values (e.g. from file, env, etc). |
| [PropertyValueConversion](../jmouse-core/src/main/java/org/jmouse/core/environment/PropertyValueConversion.java) | Provides a set of standard converters for property value transformation. |
| [ResourcePropertySource](../jmouse-core/src/main/java/org/jmouse/core/environment/ResourcePropertySource.java) | A JavaPropertiesPropertySource that loads properties from a Resource. |
| [StandardEnvironment](../jmouse-core/src/main/java/org/jmouse/core/environment/StandardEnvironment.java) | A standard implementation of Environment that provides access to property resolution and profile management. |
| [StandardPropertyResolver](../jmouse-core/src/main/java/org/jmouse/core/environment/StandardPropertyResolver.java) | A standard implementation of PropertyResolver that supports property retrieval and conversion. |
| [SystemEnvironmentPropertySource](../jmouse-core/src/main/java/org/jmouse/core/environment/SystemEnvironmentPropertySource.java) | A PropertySource implementation that uses system environment variables as the underlying property source. |
| [SystemPropertiesPropertySource](../jmouse-core/src/main/java/org/jmouse/core/environment/SystemPropertiesPropertySource.java) | A PropertySource implementation that uses system properties as the underlying property source. |
| [UnresolvedPropertyPlaceholderException](../jmouse-core/src/main/java/org/jmouse/core/environment/UnresolvedPropertyPlaceholderException.java) | Implements/extends `RuntimeException`. |

#### `org.jmouse.core.environment.converter`

| Type | Responsibility / contract |
|---|---|
| [HierarchicalMapConverter](../jmouse-core/src/main/java/org/jmouse/core/environment/converter/HierarchicalMapConverter.java) | A converter that transforms a flat key-value Map into a hierarchical structure. It parses dotted and indexed keys (e.g., "server.hosts[0].name") into nested maps and lists. |

#### `org.jmouse.core.events`

| Type | Responsibility / contract |
|---|---|
| [AbstractEvent](../jmouse-core/src/main/java/org/jmouse/core/events/AbstractEvent.java) | An abstract base implementation of the Event interface, providing common functionality for name, payload, and optional caller. Subclasses can extend this class to define more specialized event behaviors or additional fields and methods. |
| [AbstractEventListener](../jmouse-core/src/main/java/org/jmouse/core/events/AbstractEventListener.java) | Implements/extends `EventListener`. Operations: `name`, `supportsPayloadType`. |
| [AbstractTraceableEvent](../jmouse-core/src/main/java/org/jmouse/core/events/AbstractTraceableEvent.java) | Base implementation for traceable events. |
| [AnyEvent](../jmouse-core/src/main/java/org/jmouse/core/events/AnyEvent.java) | Value record: `(EventCategory category) implements EventName`. |
| [DeduplicatingPublishPolicy](../jmouse-core/src/main/java/org/jmouse/core/events/DeduplicatingPublishPolicy.java) | Event publish policy that delegates to another policy and additionally deduplicates events using a trace-local "seen keys" storage. |
| [Event](../jmouse-core/src/main/java/org/jmouse/core/events/Event.java) | Represents an event in a publish-subscribe (observer) pattern. Each event has: |
| [EventCategory](../jmouse-core/src/main/java/org/jmouse/core/events/EventCategory.java) | Represents a logical category of events. |
| [EventDispatchErrorHandler](../jmouse-core/src/main/java/org/jmouse/core/events/EventDispatchErrorHandler.java) | Handles errors thrown by listeners during event dispatch. |
| [EventListener](../jmouse-core/src/main/java/org/jmouse/core/events/EventListener.java) | Listener for events published by EventManager. |
| [EventManager](../jmouse-core/src/main/java/org/jmouse/core/events/EventManager.java) | Thread-safe event bus that supports per-event and global subscriptions. |
| [EventManagerException](../jmouse-core/src/main/java/org/jmouse/core/events/EventManagerException.java) | Implements/extends `RuntimeException`. |
| [EventManagerFactory](../jmouse-core/src/main/java/org/jmouse/core/events/EventManagerFactory.java) | Factory class for creating and configuring EventManager instances. |
| [EventManagerSingleton](../jmouse-core/src/main/java/org/jmouse/core/events/EventManagerSingleton.java) | Lazy singleton access point for EventManager. 🔔 |
| [EventName](../jmouse-core/src/main/java/org/jmouse/core/events/EventName.java) | Represents a stable event identifier. |
| [EventOperations](../jmouse-core/src/main/java/org/jmouse/core/events/EventOperations.java) | Operations: `within`. |
| [EventPublishPolicy](../jmouse-core/src/main/java/org/jmouse/core/events/EventPublishPolicy.java) | 📨 Strategy interface that decides whether an event should be published. |
| [ListenerTypeAnnotationProcessor](../jmouse-core/src/main/java/org/jmouse/core/events/ListenerTypeAnnotationProcessor.java) | Processes Listener annotations on types. 🎧 |
| [ObserverException](../jmouse-core/src/main/java/org/jmouse/core/events/ObserverException.java) | Implements/extends `RuntimeException`. |
| [SimpleEventName](../jmouse-core/src/main/java/org/jmouse/core/events/SimpleEventName.java) | Simple EventName implementation backed by strings. |
| [Subscription](../jmouse-core/src/main/java/org/jmouse/core/events/Subscription.java) | A handle representing an active subscription. |
| [TraceableEvent](../jmouse-core/src/main/java/org/jmouse/core/events/TraceableEvent.java) | Marker interface for events enriched with TraceContext. |
| [TracedEvent](../jmouse-core/src/main/java/org/jmouse/core/events/TracedEvent.java) | Implements/extends `TraceableEvent`. Operations: `name`, `payload`, `payloadType`, `caller`, `trace`, `toString`. |

#### `org.jmouse.core.events.annotation`

| Type | Responsibility / contract |
|---|---|
| [Listener](../jmouse-core/src/main/java/org/jmouse/core/events/annotation/Listener.java) | 🎧 Declares a class as an event listener. |

#### `org.jmouse.core.graph`

| Type | Responsibility / contract |
|---|---|
| [AbstractGraph](../jmouse-core/src/main/java/org/jmouse/core/graph/AbstractGraph.java) | 🧩 Base class for Graph implementations backed by a concurrent adjacency map. |
| [BFSPathFinder](../jmouse-core/src/main/java/org/jmouse/core/graph/BFSPathFinder.java) | An implementation of the PathFinder interface using Breadth-First Search (BFS). This class attempts to find the shortest path between two nodes in a graph. |
| [DirectedMapGraph](../jmouse-core/src/main/java/org/jmouse/core/graph/DirectedMapGraph.java) | ➡️ Directed graph backed by AbstractGraph's concurrent adjacency map. |
| [Edge](../jmouse-core/src/main/java/org/jmouse/core/graph/Edge.java) | Represents an undirected edge connecting two nodes of type T in a graph. This class encapsulates a connection between #nodeA and #nodeB. |
| [Graph](../jmouse-core/src/main/java/org/jmouse/core/graph/Graph.java) | A generic interface representing a directed graph structure where nodes of type T can be connected by edges. This interface provides basic operations to add nodes and edges, retrieve neighbors of a node, and check for the existence of a node in the graph. |
| [PathFinder](../jmouse-core/src/main/java/org/jmouse/core/graph/PathFinder.java) | An interface defining the contract for finding paths within a graph. Implementations of this interface should provide algorithms to determine a path from a start node to an end node within a given graph. |
| [UndirectedMapGraph](../jmouse-core/src/main/java/org/jmouse/core/graph/UndirectedMapGraph.java) | 🔁 Undirected graph backed by AbstractGraph's concurrent adjacency map. |

#### `org.jmouse.core.i18n`

| Type | Responsibility / contract |
|---|---|
| [AbstractMessageSource](../jmouse-core/src/main/java/org/jmouse/core/i18n/AbstractMessageSource.java) | An abstract implementation of the MessageSource interface. |
| [AbstractMessageSourceBundle](../jmouse-core/src/main/java/org/jmouse/core/i18n/AbstractMessageSourceBundle.java) | An abstract implementation of MessageSourceBundle. |
| [CharsetResourceBundleControl](../jmouse-core/src/main/java/org/jmouse/core/i18n/CharsetResourceBundleControl.java) | Implements/extends `ResourceBundle.Control`. Operations: `newBundle`. |
| [JdkMessageBundle](../jmouse-core/src/main/java/org/jmouse/core/i18n/JdkMessageBundle.java) | A MessageBundle implementation based on the JDK's ResourceBundle. |
| [LocaleResolver](../jmouse-core/src/main/java/org/jmouse/core/i18n/LocaleResolver.java) | A functional interface for resolving the current Locale. |
| [LocalizableMessage](../jmouse-core/src/main/java/org/jmouse/core/i18n/LocalizableMessage.java) | Represents a localizable message that can be resolved using a message source. |
| [MessageBundle](../jmouse-core/src/main/java/org/jmouse/core/i18n/MessageBundle.java) | Represents a localized message bundle containing key-value pairs for messages. |
| [MessageBundleLoader](../jmouse-core/src/main/java/org/jmouse/core/i18n/MessageBundleLoader.java) | Functional interface for loading localized message bundles. |
| [MessageSource](../jmouse-core/src/main/java/org/jmouse/core/i18n/MessageSource.java) | Defines a contract for retrieving localized messages. |
| [MessageSourceBundle](../jmouse-core/src/main/java/org/jmouse/core/i18n/MessageSourceBundle.java) | Represents a source for loading localized message bundles. |
| [MessageSourceTypeAware](../jmouse-core/src/main/java/org/jmouse/core/i18n/MessageSourceTypeAware.java) | An extension of MessageSource that provides message retrieval based on the type of an instance or class. |
| [StandardMessageSourceBundle](../jmouse-core/src/main/java/org/jmouse/core/i18n/StandardMessageSourceBundle.java) | A standard implementation of a resource bundle loader. |

#### `org.jmouse.core.invoke`

| Type | Responsibility / contract |
|---|---|
| [ArrayArgumentsMethodArgumentResolver](../jmouse-core/src/main/java/org/jmouse/core/invoke/ArrayArgumentsMethodArgumentResolver.java) | MethodArgumentResolver that resolves arguments from a predefined array/list. 📦 |
| [ContextMethodArgumentResolver](../jmouse-core/src/main/java/org/jmouse/core/invoke/ContextMethodArgumentResolver.java) | MethodArgumentResolver for Context parameters. 📦 |
| [InvocableMethod](../jmouse-core/src/main/java/org/jmouse/core/invoke/InvocableMethod.java) | Bound method ready for invocation. 🎯 |
| [InvocationMethodContext](../jmouse-core/src/main/java/org/jmouse/core/invoke/InvocationMethodContext.java) | Implements/extends `AbstractVariablesContext`. Operations: `createDefault`. |
| [InvocationRequest](../jmouse-core/src/main/java/org/jmouse/core/invoke/InvocationRequest.java) | Describes a runtime request for method invocation. ⚙️ |
| [InvocationRequestMethodArgumentResolver](../jmouse-core/src/main/java/org/jmouse/core/invoke/InvocationRequestMethodArgumentResolver.java) | MethodArgumentResolver for InvocationRequest parameters. 📨 |
| [MethodArgumentResolver](../jmouse-core/src/main/java/org/jmouse/core/invoke/MethodArgumentResolver.java) | Strategy for resolving a single method argument. 🧩 |
| [MethodArgumentResolverComposite](../jmouse-core/src/main/java/org/jmouse/core/invoke/MethodArgumentResolverComposite.java) | Composite MethodArgumentResolver. 🔗 |
| [MethodInvoker](../jmouse-core/src/main/java/org/jmouse/core/invoke/MethodInvoker.java) | Strategy for invoking bound methods. ▶️ |

#### `org.jmouse.core.io`

| Type | Responsibility / contract |
|---|---|
| [AbstractResource](../jmouse-core/src/main/java/org/jmouse/core/io/AbstractResource.java) | An abstract base class for Resource implementations. |
| [AbstractResourceLoader](../jmouse-core/src/main/java/org/jmouse/core/io/AbstractResourceLoader.java) | Implements/extends `ResourceLoader`. Operations: `getClassLoader`, `ensureSupportedProtocol`. |
| [ByteArrayResource](../jmouse-core/src/main/java/org/jmouse/core/io/ByteArrayResource.java) | 📦 Resource backed by a byte array. |
| [ClasspathResourceLoader](../jmouse-core/src/main/java/org/jmouse/core/io/ClasspathResourceLoader.java) | A ResourceLoader implementation for loading resources from the classpath. |
| [CompositeResourceLoader](../jmouse-core/src/main/java/org/jmouse/core/io/CompositeResourceLoader.java) | A composite ResourceLoader that aggregates multiple loaders and delegates resource loading based on the protocol. |
| [FileSystemResource](../jmouse-core/src/main/java/org/jmouse/core/io/FileSystemResource.java) | A resource implementation for accessing and manipulating files in the file system. |
| [FileSystemResourceLoader](../jmouse-core/src/main/java/org/jmouse/core/io/FileSystemResourceLoader.java) | A ResourceLoader implementation for loading resources from the file system. |
| [InputStreamResource](../jmouse-core/src/main/java/org/jmouse/core/io/InputStreamResource.java) | 📦 Resource backed by a single-use InputStream. |
| [JarResourceException](../jmouse-core/src/main/java/org/jmouse/core/io/JarResourceException.java) | Implements/extends `ResourceException`. |
| [JarURLResource](../jmouse-core/src/main/java/org/jmouse/core/io/JarURLResource.java) | Implements/extends `URLResource`. Operations: `getResourceName`, `getLastModified`. |
| [JarURLResourceLoader](../jmouse-core/src/main/java/org/jmouse/core/io/JarURLResourceLoader.java) | A ResourceLoader implementation for loading resources from JAR files. |
| [JrtResourceLoader](../jmouse-core/src/main/java/org/jmouse/core/io/JrtResourceLoader.java) | A resource scanner for locating and loading resources from the JRT (Java Runtime) file system. |
| [JrtURLResource](../jmouse-core/src/main/java/org/jmouse/core/io/JrtURLResource.java) | A specialized URLResource implementation for resources located in the Java Runtime (JRT) module system. |
| [NetworkURLResourceLoader](../jmouse-core/src/main/java/org/jmouse/core/io/NetworkURLResourceLoader.java) | A ResourceLoader implementation for loading resources from network locations. |
| [PatternMatcherResourceLoader](../jmouse-core/src/main/java/org/jmouse/core/io/PatternMatcherResourceLoader.java) | Interface for resource loaders that support pattern matching. |
| [ReadableResource](../jmouse-core/src/main/java/org/jmouse/core/io/ReadableResource.java) | Interface for resources that can be read. |
| [Resource](../jmouse-core/src/main/java/org/jmouse/core/io/Resource.java) | 📦 Represents a generic resource (file, classpath entry, JAR entry, etc.). |
| [ResourceException](../jmouse-core/src/main/java/org/jmouse/core/io/ResourceException.java) | Implements/extends `RuntimeException`. |
| [ResourceLoader](../jmouse-core/src/main/java/org/jmouse/core/io/ResourceLoader.java) | Interface for loading resources from various locations and protocols. |
| [ResourceLoaderException](../jmouse-core/src/main/java/org/jmouse/core/io/ResourceLoaderException.java) | Implements/extends `RuntimeException`. |
| [ResourceLoaderRegistry](../jmouse-core/src/main/java/org/jmouse/core/io/ResourceLoaderRegistry.java) | Interface for managing and accessing multiple ResourceLoader instances. Provides methods for registering, retrieving, and removing loaders. |
| [ResourceSegment](../jmouse-core/src/main/java/org/jmouse/core/io/ResourceSegment.java) | 📦 Represents a segment (subsection) of a Resource. |
| [URLResource](../jmouse-core/src/main/java/org/jmouse/core/io/URLResource.java) | A Resource implementation that represents a resource loaded from a URL. |
| [WritableResource](../jmouse-core/src/main/java/org/jmouse/core/io/WritableResource.java) | Interface for resources that support writing operations. |

#### `org.jmouse.core.io.reader`

| Type | Responsibility / contract |
|---|---|
| [PropertiesResourceReader](../jmouse-core/src/main/java/org/jmouse/core/io/reader/PropertiesResourceReader.java) | Implements/extends `PropertyResourceReader`. Operations: `supports`, `read`. |
| [PropertyResourceReader](../jmouse-core/src/main/java/org/jmouse/core/io/reader/PropertyResourceReader.java) | 🧩 Converts a Resource into a flat key-value map. |

#### `org.jmouse.core.management`

| Type | Responsibility / contract |
|---|---|
| [ManagementEndpoints](../jmouse-core/src/main/java/org/jmouse/core/management/ManagementEndpoints.java) | 🛣️ Where a library's own management routes answer, and how a product moves them. |

#### `org.jmouse.core.matcher`

| Type | Responsibility / contract |
|---|---|
| [CollectionMatchers](../jmouse-core/src/main/java/org/jmouse/core/matcher/CollectionMatchers.java) | Operations: `contains`, `isEmpty`, `size`, `anyOf`, `allOf`, `matches`. |
| [Match](../jmouse-core/src/main/java/org/jmouse/core/matcher/Match.java) | 🎯 Represents a generic outcome of a matching step capable of carrying multiple typed results — called facets . |
| [Matcher](../jmouse-core/src/main/java/org/jmouse/core/matcher/Matcher.java) | A generic interface for matching items of type T against certain conditions. This interface supports chaining of matchers using logical operators such as AND, OR, XOR, and NOT. |
| [MatchOp](../jmouse-core/src/main/java/org/jmouse/core/matcher/MatchOp.java) | 🎯 Functional interface representing a single match operation. |
| [PathMatchers](../jmouse-core/src/main/java/org/jmouse/core/matcher/PathMatchers.java) | A utility class that provides matchers for working with Path objects. These matchers can check various conditions like file existence, directory checks, file permissions, file size, and more. |
| [TextMatchers](../jmouse-core/src/main/java/org/jmouse/core/matcher/TextMatchers.java) | A utility class that provides matchers for strings. These matchers can check if a string contains a substring, equals another string, starts with a prefix, or ends with a suffix. |

#### `org.jmouse.core.matcher.ant`

| Type | Responsibility / contract |
|---|---|
| [AntMatcher](../jmouse-core/src/main/java/org/jmouse/core/matcher/ant/AntMatcher.java) | Implements/extends `Matcher`. Operations: `matches`, `of`, `toString`, `equals`, `hashCode`. |

#### `org.jmouse.core.net`

| Type | Responsibility / contract |
|---|---|
| [CIDR](../jmouse-core/src/main/java/org/jmouse/core/net/CIDR.java) | Immutable representation of a CIDR block for IPv4/IPv6. |

#### `org.jmouse.core.parameters`

| Type | Responsibility / contract |
|---|---|
| [RequestParametersArrayNode](../jmouse-core/src/main/java/org/jmouse/core/parameters/RequestParametersArrayNode.java) | Value record: `(List items) implements RequestParametersNode`. |
| [RequestParametersJavaStructureConverter](../jmouse-core/src/main/java/org/jmouse/core/parameters/RequestParametersJavaStructureConverter.java) | Operations: `toJavaObject`, `toMap`. |
| [RequestParametersJavaStructureOptions](../jmouse-core/src/main/java/org/jmouse/core/parameters/RequestParametersJavaStructureOptions.java) | Value record: `( boolean unwrapSingleScalarValue, boolean unwrapSingleElementArray, boolean dropNullArrayItems )`. |
| [RequestParametersNode](../jmouse-core/src/main/java/org/jmouse/core/parameters/RequestParametersNode.java) | Operations: `nodeType`. |
| [RequestParametersObjectNode](../jmouse-core/src/main/java/org/jmouse/core/parameters/RequestParametersObjectNode.java) | Value record: `(Map fields) implements RequestParametersNode`. |
| [RequestParametersPathParser](../jmouse-core/src/main/java/org/jmouse/core/parameters/RequestParametersPathParser.java) | Operations: `parse`. |
| [RequestParametersPathToken](../jmouse-core/src/main/java/org/jmouse/core/parameters/RequestParametersPathToken.java) | Operations: `instance`. |
| [RequestParametersScalarNode](../jmouse-core/src/main/java/org/jmouse/core/parameters/RequestParametersScalarNode.java) | Value record: `(List values) implements RequestParametersNode`. |
| [RequestParametersTree](../jmouse-core/src/main/java/org/jmouse/core/parameters/RequestParametersTree.java) | Value record: `(RequestParametersObjectNode root)`. |
| [RequestParametersTreeParser](../jmouse-core/src/main/java/org/jmouse/core/parameters/RequestParametersTreeParser.java) | Operations: `parse`. |

#### `org.jmouse.core.parameters.support`

| Type | Responsibility / contract |
|---|---|
| [RequestParametersTreeFlattener](../jmouse-core/src/main/java/org/jmouse/core/parameters/support/RequestParametersTreeFlattener.java) | Operations: `flatten`. |
| [RequestParametersTreePrinter](../jmouse-core/src/main/java/org/jmouse/core/parameters/support/RequestParametersTreePrinter.java) | Operations: `toPrettyString`. |

#### `org.jmouse.core.proxy`

| Type | Responsibility / contract |
|---|---|
| [AbstractProxyFactory](../jmouse-core/src/main/java/org/jmouse/core/proxy/AbstractProxyFactory.java) | 🏗️ Base implementation of ProxyFactory. |
| [Bubble](../jmouse-core/src/main/java/org/jmouse/core/proxy/Bubble.java) | Implements/extends `RuntimeException`. Operations: `getCause`. |
| [ByteBuddyProxyEngine](../jmouse-core/src/main/java/org/jmouse/core/proxy/ByteBuddyProxyEngine.java) | Implements/extends `ProxyEngine`. Operations: `supports`, `name`, `createProxy`, `intercept`. |
| [DefaultProxyFactory](../jmouse-core/src/main/java/org/jmouse/core/proxy/DefaultProxyFactory.java) | 🏭 Default implementation of ProxyFactory. |
| [InstanceProvider](../jmouse-core/src/main/java/org/jmouse/core/proxy/InstanceProvider.java) | 🎭 Abstraction for providing target instances. |
| [Intercept](../jmouse-core/src/main/java/org/jmouse/core/proxy/Intercept.java) | 🪝 Marks a class as a MethodInterceptor applicable to specific targets. |
| [InterceptableProxy](../jmouse-core/src/main/java/org/jmouse/core/proxy/InterceptableProxy.java) | 🎯 Marker interface for ByteBuddy-generated proxies that can dispatch directly through the interceptor chain. |
| [InterceptionPolicy](../jmouse-core/src/main/java/org/jmouse/core/proxy/InterceptionPolicy.java) | 🎛️ Defines which Methods should be intercepted by a proxy. |
| [InterceptorLink](../jmouse-core/src/main/java/org/jmouse/core/proxy/InterceptorLink.java) | Implements/extends `Link`. Operations: `handle`, `getProxy`, `getTarget`, `getMethod`, `getArguments`, `setArgumentsUnsafe`, `getOrdinal`, `getReturnValue`, `setReturnValue`, `toString`, `proceed`. |
| [InterceptorMatcher](../jmouse-core/src/main/java/org/jmouse/core/proxy/InterceptorMatcher.java) | 🎯 Specialization of Matcher for class-level interception rules. |
| [InterceptorRegistrar](../jmouse-core/src/main/java/org/jmouse/core/proxy/InterceptorRegistrar.java) | 📦 Registrar that scans the classpath for MethodInterceptors annotated with Intercept and registers them into an InterceptorRegistry. |
| [InterceptorRegistry](../jmouse-core/src/main/java/org/jmouse/core/proxy/InterceptorRegistry.java) | 📚 Registry of MethodInterceptors with optional ordering and matching rules. |
| [InvocationContext](../jmouse-core/src/main/java/org/jmouse/core/proxy/InvocationContext.java) | 📦 Immutable call-scoped context shared across all interceptors in a pipeline. |
| [InvocationDispatcher](../jmouse-core/src/main/java/org/jmouse/core/proxy/InvocationDispatcher.java) | 🛣️ Abstraction for dispatching method calls from a proxy. |
| [InvocationPipeline](../jmouse-core/src/main/java/org/jmouse/core/proxy/InvocationPipeline.java) | 🧵 Builds an interceptor-based invocation pipeline. |
| [JdkInvocationHandler](../jmouse-core/src/main/java/org/jmouse/core/proxy/JdkInvocationHandler.java) | 📦 JDK dynamic-proxy InvocationHandler implementation. |
| [JdkProxyEngine](../jmouse-core/src/main/java/org/jmouse/core/proxy/JdkProxyEngine.java) | Implements/extends `ProxyEngine`. Operations: `supports`, `name`, `createProxy`. |
| [MethodInterceptor](../jmouse-core/src/main/java/org/jmouse/core/proxy/MethodInterceptor.java) | An interface defining a method interceptor, which allows code to be executed before and after a method invocation. Implementations typically handle concerns such as logging, security checks, or performance measurement, and then delegate the actual method call to #invoke(MethodInvocation). |
| [MethodInvocation](../jmouse-core/src/main/java/org/jmouse/core/proxy/MethodInvocation.java) | Represents a method invocation on a proxy instance, allowing for method interception and custom behavior. |
| [MethodInvocationDecorator](../jmouse-core/src/main/java/org/jmouse/core/proxy/MethodInvocationDecorator.java) | Implements/extends `MethodInvocation`. Operations: `getTypedArgument`, `getMethodName`, `getMethodClass`, `getMethodClassName`, `getThisClass`, `getArguments`, `setArgumentsUnsafe`, `getMethod`, `getTarget`, `getOrdinal`, `getProxy`, `proceed`. |
| [Mixins](../jmouse-core/src/main/java/org/jmouse/core/proxy/Mixins.java) | 🧩 Container for mixins — bindings of interface → implementation. |
| [PrototypeProvider](../jmouse-core/src/main/java/org/jmouse/core/proxy/PrototypeProvider.java) | Implements/extends `InstanceProvider`. Operations: `get`, `toString`. |
| [ProxyDefinition](../jmouse-core/src/main/java/org/jmouse/core/proxy/ProxyDefinition.java) | 📐 Immutable description of a proxy. |
| [ProxyDispatcher](../jmouse-core/src/main/java/org/jmouse/core/proxy/ProxyDispatcher.java) | 🎭 General-purpose proxy dispatcher. |
| [ProxyEngine](../jmouse-core/src/main/java/org/jmouse/core/proxy/ProxyEngine.java) | ⚙️ Strategy interface for building proxies. |
| [ProxyFactory](../jmouse-core/src/main/java/org/jmouse/core/proxy/ProxyFactory.java) | 🏭 Factory interface for creating proxies and their ProxyDefinition. |
| [ProxyHelper](../jmouse-core/src/main/java/org/jmouse/core/proxy/ProxyHelper.java) | 🛠️ Utility methods for working with proxies. |
| [ProxyIntrospection](../jmouse-core/src/main/java/org/jmouse/core/proxy/ProxyIntrospection.java) | 🔍 Introspection contract for proxies. |
| [ProxyInvocationException](../jmouse-core/src/main/java/org/jmouse/core/proxy/ProxyInvocationException.java) | Implements/extends `RuntimeException`. |
| [SingletonProvider](../jmouse-core/src/main/java/org/jmouse/core/proxy/SingletonProvider.java) | Implements/extends `InstanceProvider`. Operations: `get`, `toString`. |
| [ThreadLocalProvider](../jmouse-core/src/main/java/org/jmouse/core/proxy/ThreadLocalProvider.java) | Implements/extends `InstanceProvider`. Operations: `get`, `remove`, `toString`. |
| [UnsupportedProxyException](../jmouse-core/src/main/java/org/jmouse/core/proxy/UnsupportedProxyException.java) | Implements/extends `RuntimeException`. |

#### `org.jmouse.core.proxy.interceptor`

| Type | Responsibility / contract |
|---|---|
| [CacheInterceptor](../jmouse-core/src/main/java/org/jmouse/core/proxy/interceptor/CacheInterceptor.java) | 🗄️ Interceptor that caches results of method calls using a BasicCache. |
| [RetryInterceptor](../jmouse-core/src/main/java/org/jmouse/core/proxy/interceptor/RetryInterceptor.java) | 🔁 Interceptor that retries failed method invocations according to a configurable Backoff strategy and RetryPolicy. |

#### `org.jmouse.core.reflection`

| Type | Responsibility / contract |
|---|---|
| [AbstractFilter](../jmouse-core/src/main/java/org/jmouse/core/reflection/AbstractFilter.java) | An abstract base class for implementing filters that work with reflection members. |
| [ClassFinder](../jmouse-core/src/main/java/org/jmouse/core/reflection/ClassFinder.java) | ClassFinder provides utilities for finding, filtering, and sorting classes. It includes caching mechanisms, a default class scanner, and a variety of comparators to customize the sorting of classes. |
| [ClassMatchers](../jmouse-core/src/main/java/org/jmouse/core/reflection/ClassMatchers.java) | Utility class for creating matchers that work with Class objects. |
| [Classpath](../jmouse-core/src/main/java/org/jmouse/core/reflection/Classpath.java) | Operations: `present`. |
| [ConstructorFilter](../jmouse-core/src/main/java/org/jmouse/core/reflection/ConstructorFilter.java) | Implements/extends `AbstractFilter >`. |
| [ConstructorFinder](../jmouse-core/src/main/java/org/jmouse/core/reflection/ConstructorFinder.java) | A class that finds constructors in a given class. It supports scanning superclasses to retrieve inherited constructors if required. |
| [Executables](../jmouse-core/src/main/java/org/jmouse/core/reflection/Executables.java) | Operations: `updateArgument`, `resolveParameterType`. |
| [FieldFilter](../jmouse-core/src/main/java/org/jmouse/core/reflection/FieldFilter.java) | Implements/extends `AbstractFilter`. |
| [FieldFinder](../jmouse-core/src/main/java/org/jmouse/core/reflection/FieldFinder.java) | A class that finds fields in a given class. It supports scanning superclasses to retrieve inherited fields and filtering fields based on annotations. |
| [FieldMatchers](../jmouse-core/src/main/java/org/jmouse/core/reflection/FieldMatchers.java) | Utility class for creating matchers that work with Field objects. |
| [Filter](../jmouse-core/src/main/java/org/jmouse/core/reflection/Filter.java) | Interface for filtering reflection members in a class. |
| [Infer](../jmouse-core/src/main/java/org/jmouse/core/reflection/Infer.java) | Operations: `rawClassOf`, `typeOf`. |
| [InferredType](../jmouse-core/src/main/java/org/jmouse/core/reflection/InferredType.java) | A utility class for inspecting, resolving, and navigating Java Type objects. |
| [MemberFinder](../jmouse-core/src/main/java/org/jmouse/core/reflection/MemberFinder.java) | An interface for finding members (fields, methods, constructors) of a class that match specific criteria. |
| [MemberMatcher](../jmouse-core/src/main/java/org/jmouse/core/reflection/MemberMatcher.java) | A utility class for creating Matcher instances that operate on Member objects. |
| [MethodFilter](../jmouse-core/src/main/java/org/jmouse/core/reflection/MethodFilter.java) | A filter class to apply various conditions for filtering methods in a class. |
| [MethodFinder](../jmouse-core/src/main/java/org/jmouse/core/reflection/MethodFinder.java) | A class that finds methods in a given class. It supports scanning superclasses and interfaces to retrieve inherited or implemented methods. |
| [MethodMatchers](../jmouse-core/src/main/java/org/jmouse/core/reflection/MethodMatchers.java) | Utility class for creating matchers that work with Method and Constructor objects. |
| [ReflectionException](../jmouse-core/src/main/java/org/jmouse/core/reflection/ReflectionException.java) | Implements/extends `RuntimeException`. |
| [Reflections](../jmouse-core/src/main/java/org/jmouse/core/reflection/Reflections.java) | A utility class providing various reflection-based methods for working with classes, fields, methods, and constructors. |
| [ScannerContext](../jmouse-core/src/main/java/org/jmouse/core/reflection/ScannerContext.java) | A specialized context for managing sourceRoot classes during scanning operations. Extends AbstractContext to provide additional functionality specific to sourceRoot class management. |
| [TypeClassifier](../jmouse-core/src/main/java/org/jmouse/core/reflection/TypeClassifier.java) | Provides a type inspection utility for analyzing and classifying Java classes. |
| [TypeInformation](../jmouse-core/src/main/java/org/jmouse/core/reflection/TypeInformation.java) | Represents type descriptor and provides utility methods for type analysis. |
| [TypeMap](../jmouse-core/src/main/java/org/jmouse/core/reflection/TypeMap.java) | A utility class that provides mappings for Java primitive types, their wrapper types, and default values for primitive types. |
| [TypeMatchers](../jmouse-core/src/main/java/org/jmouse/core/reflection/TypeMatchers.java) | A utility class that provides matchers for comparing types. These matchers allow for checking if two types are similar (i.e., primitive and its wrapper) or if one type is a subtype of another. |
| [TypeReference](../jmouse-core/src/main/java/org/jmouse/core/reflection/TypeReference.java) | A generic type reference that captures and retains generic type information at runtime via the super type's type parameter. This is particularly useful for scenarios where Java's type erasure would otherwise remove generic type details, such as deserialization frameworks or reflection-based utilities. |
| [Unknown](../jmouse-core/src/main/java/org/jmouse/core/reflection/Unknown.java) | Marker type whose TYPE constant represents Unknown.class for reflection/type-resolution code. |

#### `org.jmouse.core.reflection.annotation`

| Type | Responsibility / contract |
|---|---|
| [AnnotationAttributeMapping](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/AnnotationAttributeMapping.java) | 🧩 Describes a mapping of annotation attributes within a merged hierarchy. |
| [AnnotationAttributes](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/AnnotationAttributes.java) | 🧠 Caches and provides access to annotation attribute methods. |
| [AnnotationData](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/AnnotationData.java) | 📎 Represents a single annotation with metadata context. |
| [AnnotationFinder](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/AnnotationFinder.java) | 🔍 Utility for recursive annotation scanning. |
| [AnnotationMapping](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/AnnotationMapping.java) | 🔗 Default implementation of AnnotationAttributeMapping. |
| [AnnotationMatcher](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/AnnotationMatcher.java) | 🎯 Matchers for annotation types. Helps define reusable conditions for filtering annotations. |
| [AnnotationRepository](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/AnnotationRepository.java) | 🧩 Aggregates and flattens annotations from an AnnotatedElement, including meta-annotations. |
| [Annotations](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/Annotations.java) | 🧩 Utilities for annotation lookup and merging. |
| [AnnotationScanner](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/AnnotationScanner.java) | 🔍 Scans annotations recursively, including meta-annotations. |
| [AnnotationSynthesizer](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/AnnotationSynthesizer.java) | 🔧 Synthesizes annotations with mapped attributes from meta-annotations. |
| [CollisionPolicy](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/CollisionPolicy.java) | Enum values: `KEEP_EXISTING`, `OVERWRITE`. |
| [DefaultAnnotationSynthesizer](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/DefaultAnnotationSynthesizer.java) | Implements/extends `AnnotationSynthesizer`. Operations: `synthesize`. |
| [MapTo](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/MapTo.java) | 🔗 Maps an attribute to a target annotation and attribute. |
| [MergedAnnotation](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/MergedAnnotation.java) | 🧬 Represents a merged annotation with its meta-annotations. |
| [MetaKeyQualifier](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/MetaKeyQualifier.java) | Operations: `qualify`. |
| [MetaScope](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/MetaScope.java) | Enum values: `DIRECT_ONLY`, `ALL`. |
| [SynthesizedAnnotationProxy](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/SynthesizedAnnotationProxy.java) | 🌀 Dynamic proxy for synthesized annotations. |
| [SyntheticAnnotation](../jmouse-core/src/main/java/org/jmouse/core/reflection/annotation/SyntheticAnnotation.java) | 🧬 Marker interface for synthetic annotations. |

#### `org.jmouse.core.reflection.scanner`

| Type | Responsibility / contract |
|---|---|
| [ClassScanner](../jmouse-core/src/main/java/org/jmouse/core/reflection/scanner/ClassScanner.java) | Interface for scanning classes based on specific criteria. |
| [DefaultClassScanner](../jmouse-core/src/main/java/org/jmouse/core/reflection/scanner/DefaultClassScanner.java) | DirectAccess implementation of ClassScanner. |
| [Example](../jmouse-core/src/main/java/org/jmouse/core/reflection/scanner/Example.java) | Operations: `main`. |

#### `org.jmouse.core.scope`

| Type | Responsibility / contract |
|---|---|
| [AbstractArgumentsContext](../jmouse-core/src/main/java/org/jmouse/core/scope/AbstractArgumentsContext.java) | Implements/extends `ArgumentsContext`. Operations: `getArgument`, `setArgument`, `hasArgument`, `toString`. |
| [AbstractAttributesContext](../jmouse-core/src/main/java/org/jmouse/core/scope/AbstractAttributesContext.java) | Implements/extends `AttributesContext`. Operations: `requireAttribute`, `getAttribute`, `setAttribute`, `setAttributes`, `hasAttribute`, `toString`. |
| [AbstractContext](../jmouse-core/src/main/java/org/jmouse/core/scope/AbstractContext.java) | An abstract implementation of the Context interface. Provides a base for managing key-value properties and integrating with a BeanLookup. |
| [AbstractResultContext](../jmouse-core/src/main/java/org/jmouse/core/scope/AbstractResultContext.java) | Implements/extends `ResultContext`. Operations: `getReturnValue`, `setReturnValue`, `hasErrors`, `getErrors`, `getError`, `addError`, `cleanup`. |
| [AbstractVariablesContext](../jmouse-core/src/main/java/org/jmouse/core/scope/AbstractVariablesContext.java) | Implements/extends `AbstractContext implements VariablesContext`. Operations: `getVariable`, `setVariable`. |
| [ArgumentNotFoundException](../jmouse-core/src/main/java/org/jmouse/core/scope/ArgumentNotFoundException.java) | Implements/extends `RuntimeException`. |
| [ArgumentsContext](../jmouse-core/src/main/java/org/jmouse/core/scope/ArgumentsContext.java) | Operations: `ArgumentNotFoundException`, `getArgument`, `setArgument`, `hasArgument`. |
| [ArgumentsContextAware](../jmouse-core/src/main/java/org/jmouse/core/scope/ArgumentsContextAware.java) | Operations: `getArgumentsContext`, `setArgumentsContext`. |
| [AttributeContextAware](../jmouse-core/src/main/java/org/jmouse/core/scope/AttributeContextAware.java) | Operations: `setAttributeContext`. |
| [AttributeNotFoundException](../jmouse-core/src/main/java/org/jmouse/core/scope/AttributeNotFoundException.java) | Implements/extends `RuntimeException`. |
| [AttributesContext](../jmouse-core/src/main/java/org/jmouse/core/scope/AttributesContext.java) | Operations: `requireAttribute`, `getAttribute`, `setAttribute`, `setAttributes`, `hasAttribute`. |
| [Context](../jmouse-core/src/main/java/org/jmouse/core/scope/Context.java) | Interface defining a generic context for managing properties and beans. Provides methods to manipulate key-value properties and integrate with BeanLookup. |
| [DefaultContextException](../jmouse-core/src/main/java/org/jmouse/core/scope/DefaultContextException.java) | Implements/extends `RuntimeException`. |
| [ErrorDetails](../jmouse-core/src/main/java/org/jmouse/core/scope/ErrorDetails.java) | Value record: `(String code, String message, Throwable cause)`. |
| [MissingBeanProviderException](../jmouse-core/src/main/java/org/jmouse/core/scope/MissingBeanProviderException.java) | Implements/extends `RuntimeException`. |
| [ResultContext](../jmouse-core/src/main/java/org/jmouse/core/scope/ResultContext.java) | Operations: `getReturnValue`, `setReturnValue`, `hasErrors`, `getErrors`, `getError`, `addError`, `cleanup`. |
| [ResultContextAware](../jmouse-core/src/main/java/org/jmouse/core/scope/ResultContextAware.java) | Operations: `getResultContext`, `setResultContext`. |
| [VariableNotFoundException](../jmouse-core/src/main/java/org/jmouse/core/scope/VariableNotFoundException.java) | Implements/extends `RuntimeException`. |
| [VariablesContext](../jmouse-core/src/main/java/org/jmouse/core/scope/VariablesContext.java) | Operations: `VariableNotFoundException`, `getVariable`, `setVariable`. |

#### `org.jmouse.core.support`

| Type | Responsibility / contract |
|---|---|
| [ArraySupport](../jmouse-core/src/main/java/org/jmouse/core/support/ArraySupport.java) | Operations: `toCollection`, `isArray`, `toList`, `toCodePoints`. |

#### `org.jmouse.core.text`

| Type | Responsibility / contract |
|---|---|
| [CyrillicTransliterator](../jmouse-core/src/main/java/org/jmouse/core/text/CyrillicTransliterator.java) | 🔤 Cyrillic in Latin letters — Ukrainian and Russian, in one table. |
| [ExtendedLatinTransliterator](../jmouse-core/src/main/java/org/jmouse/core/text/ExtendedLatinTransliterator.java) | 🔤 The Latin letters a Unicode decomposition cannot take apart. |
| [Slugifier](../jmouse-core/src/main/java/org/jmouse/core/text/Slugifier.java) | 🏷️ A name a person typed, turned into something that can be an address. |
| [Transliterator](../jmouse-core/src/main/java/org/jmouse/core/text/Transliterator.java) | 🔤 Text of one writing system, rewritten in Latin letters. |

#### `org.jmouse.core.throttle`

| Type | Responsibility / contract |
|---|---|
| [AbstractRateLimitMethodInterceptor](../jmouse-core/src/main/java/org/jmouse/core/throttle/AbstractRateLimitMethodInterceptor.java) | Base interceptor for RateLimit. 🚦 |
| [FixedRateTokenBucket](../jmouse-core/src/main/java/org/jmouse/core/throttle/FixedRateTokenBucket.java) | 🪣 Fixed-Rate Token Bucket rate limiter. |
| [MethodArgumentsHashKeyResolver](../jmouse-core/src/main/java/org/jmouse/core/throttle/MethodArgumentsHashKeyResolver.java) | 🔑 Default RateLimitKeyResolver based on method + arguments hash. |
| [RateLimit](../jmouse-core/src/main/java/org/jmouse/core/throttle/RateLimit.java) | ⏳ Annotation for declaring a rate limit on a method or type. |
| [RateLimited](../jmouse-core/src/main/java/org/jmouse/core/throttle/RateLimited.java) | 🚦 Marker interface for beans/components subject to rate limiting. |
| [RateLimitEnable](../jmouse-core/src/main/java/org/jmouse/core/throttle/RateLimitEnable.java) | Annotation contract; members and defaults are defined in this source. |
| [RateLimiter](../jmouse-core/src/main/java/org/jmouse/core/throttle/RateLimiter.java) | ⏳ RateLimiter — sealed interface for token-bucket–based rate limiting strategies. |
| [RateLimitExceededException](../jmouse-core/src/main/java/org/jmouse/core/throttle/RateLimitExceededException.java) | Implements/extends `RuntimeException`. Operations: `getRateLimitConfiguration`. |
| [RateLimitKeyResolver](../jmouse-core/src/main/java/org/jmouse/core/throttle/RateLimitKeyResolver.java) | Resolves a key used to identify a rate limit bucket. |
| [SmoothTokenBucket](../jmouse-core/src/main/java/org/jmouse/core/throttle/SmoothTokenBucket.java) | 🪙 Smooth Token Bucket rate limiter. |

#### `org.jmouse.core.trace`

| Type | Responsibility / contract |
|---|---|
| [ContextTraceRunner](../jmouse-core/src/main/java/org/jmouse/core/trace/ContextTraceRunner.java) | Implements/extends `EventOperations.TraceScopeRunner`. Operations: `run`. |
| [SpanScopes](../jmouse-core/src/main/java/org/jmouse/core/trace/SpanScopes.java) | Operations: `none`, `rootIfAbsent`, `child`. |
| [Trace](../jmouse-core/src/main/java/org/jmouse/core/trace/Trace.java) | Operations: `current`, `root`, `child`, `touch`. |
| [TraceContext](../jmouse-core/src/main/java/org/jmouse/core/trace/TraceContext.java) | Value record: `( String correlationId, String spanId, String parentSpanId, int depth, Instant timestamp )`. |
| [TraceDeduplicate](../jmouse-core/src/main/java/org/jmouse/core/trace/TraceDeduplicate.java) | Operations: `firstTime`. |
| [TraceKeys](../jmouse-core/src/main/java/org/jmouse/core/trace/TraceKeys.java) | Defines the typed trace context key named trace for storing a TraceContext in execution state. |

#### `org.jmouse.geo`

| Type | Responsibility / contract |
|---|---|
| [Country](../jmouse-core/src/main/java/org/jmouse/geo/Country.java) | 🌍 Country — ISO 3166-1 dataset (без залежності від java.util.Locale). |

#### `org.jmouse.helpers`

| Type | Responsibility / contract |
|---|---|
| [Arrays](../jmouse-core/src/main/java/org/jmouse/helpers/Arrays.java) | Utility class for array operations. |
| [Booleans](../jmouse-core/src/main/java/org/jmouse/helpers/Booleans.java) | Utility class Booleans for performing common boolean comparisons. |
| [Commons](../jmouse-core/src/main/java/org/jmouse/helpers/Commons.java) | Operations: `unwrap`. |
| [DateTimeHelper](../jmouse-core/src/main/java/org/jmouse/helpers/DateTimeHelper.java) | Operations: `parseInstant`, `toNanos`. |
| [Files](../jmouse-core/src/main/java/org/jmouse/helpers/Files.java) | Operations: `packageToPath`, `extractProtocol`, `removeProtocol`, `getPath`, `normalizePath`, `getRelativePath`, `removeExtension`. |
| [Iterables](../jmouse-core/src/main/java/org/jmouse/helpers/Iterables.java) | A utility class that provides helper methods for working with iterables. |
| [Jars](../jmouse-core/src/main/java/org/jmouse/helpers/Jars.java) | Operations: `isJarURL`, `getBasePath`, `getFilePath`. |
| [JavaIO](../jmouse-core/src/main/java/org/jmouse/helpers/JavaIO.java) | Utility class for handling Java IO operations, specifically related to URLs and resources. |
| [Maths](../jmouse-core/src/main/java/org/jmouse/helpers/Maths.java) | Utility class Maths for performing basic mathematical operations. |
| [Numbers](../jmouse-core/src/main/java/org/jmouse/helpers/Numbers.java) | Operations: `toDecimal`. |
| [PathHelper](../jmouse-core/src/main/java/org/jmouse/helpers/PathHelper.java) | Operations: `normalize`. |
| [StringHelper](../jmouse-core/src/main/java/org/jmouse/helpers/StringHelper.java) | Operations: `commaSeparated`, `keyValue`, `tokenize`, `toStringArray`, `digest`, `hex`, `toLength`, `isQuoted`, `ensureQuoted`, `quote`, `unquote`, `toCamel`. |
| [Strings](../jmouse-core/src/main/java/org/jmouse/helpers/Strings.java) | A utility class for working with and manipulating String objects. This class provides common operations such as checking for emptiness, uncapitalizing, converting to underscored format, and padding with characters. |
| [UrlHelper](../jmouse-core/src/main/java/org/jmouse/helpers/UrlHelper.java) | Minimal URI helper with a package-private toURI method delegating to URI.create; currently exposes no public conversion API. |

### Packaged resources

See [packaged resources](../jmouse-core/src/main/resources/) for configuration/templates owned by this module.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [NamedConverterProbe.java](../jmouse-core/src/main/java/org/jmouse/core/convert/examples/NamedConverterProbe.java).
- [Smoke1.java](../jmouse-core/src/main/java/org/jmouse/core/parameters/smoke/Smoke1.java).
