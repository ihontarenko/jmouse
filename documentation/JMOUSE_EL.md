# jmouse-el

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-el`. Build/dependency declaration: [pom.xml](../jmouse-el/pom.xml).

## What it is

jME is the shared lexer, parser, expression tree and evaluation runtime used by jMouse expression and document languages. It also supplies template-related nodes and extension registries. Its role is broader than evaluating an arithmetic string: other modules build distinct languages on its parsing and translation seams.

The base language owns generic expressions, property access, scopes, operators and callable/filter/test extensions. Domain document syntax belongs to its module, such as mapper-el or validation-el. Keep a document parser separate from the expression language that compiles the expressions embedded in that document.

## The map

| Type | Responsibility |
| --- | --- |
| [ExpressionLanguage](../jmouse-el/src/main/java/org/jmouse/el/ExpressionLanguage.java) | Facade for compile, cached expressions, typed evaluation and fresh evaluation contexts. |
| [DefaultEvaluationContext](../jmouse-el/src/main/java/org/jmouse/el/evaluation/DefaultEvaluationContext.java) | Per-evaluation scopes, conversion service, object storage, extensions, bean lookup and property access. |
| [BasicValuesChain](../jmouse-el/src/main/java/org/jmouse/el/evaluation/BasicValuesChain.java) | Stack of scopes; local reads precede outer-scope reads. |
| [DefaultLexer](../jmouse-el/src/main/java/org/jmouse/el/lexer/DefaultLexer.java) | Tokenizes a Source using the configured tokenizer/recognition machinery. |
| [CompositeRecognizer](../jmouse-el/src/main/java/org/jmouse/el/lexer/recognizer/CompositeRecognizer.java) | Maintains recognizer priority; the first recognizer that accepts a token wins. |
| [ParserContext](../jmouse-el/src/main/java/org/jmouse/el/parser/ParserContext.java) | Parser registry and dispatch context for a grammar. |
| [Expression](../jmouse-el/src/main/java/org/jmouse/el/node/Expression.java) | Executable expression-tree contract returned by compilation. |
| [StandardExtensionContainer](../jmouse-el/src/main/java/org/jmouse/el/extension/StandardExtensionContainer.java) | Registry for functions, tests, filters, operators and parsers. |
| [CoreExtension](../jmouse-el/src/main/java/org/jmouse/el/extension/CoreExtension.java) | Installs the standard language vocabulary. |
| [MethodImporter](../jmouse-el/src/main/java/org/jmouse/el/extension/MethodImporter.java) | Exposes selected public Java methods as overload-aware language functions. |
| [SourceReading](../jmouse-el/src/main/java/org/jmouse/el/lexer/support/SourceReading.java) | Reads source positions and literal text from a token cursor. |
| [SourceWriting](../jmouse-el/src/main/java/org/jmouse/el/lexer/support/SourceWriting.java) | Writes literal values consistently with SourceReading. |

## Execution and ownership

1. Construct an ExpressionLanguage with the extensions, lexer, parser context and root parser appropriate to the grammar.
2. Compile stable source once. ExpressionLanguage caches compiled expressions through its configured Cache implementation.
3. Create an EvaluationContext for each independent evaluation, populate scoped values and evaluate the compiled Expression.
4. For a typed result, the facade evaluates and then uses the context Conversion service to obtain the requested Java type.

## Public API and behavior

compile(String) returns Expression; evaluate overloads accept a context, an optional data map and a target Class.

newContext() gives a context sharing the language extension container while keeping evaluation state local.

DefaultEvaluationContext resets its cached value accessor when its scoped chain is replaced.

Function, Filter, Test and Operator are different contracts. A test answers a predicate; a filter transforms a supplied value.

## Wiring and extension points

- Register a language Extension or configure the ExtensionContainer before parsing/evaluation begins.
- Add recognizers and parsers for a dialect; use parser support predicates and ordering precise enough that an earlier parser cannot steal a later construct.
- Use translator implementations for another representation of a tree instead of forcing evaluation to produce that representation.

## Pitfalls

- Share the configured language, not mutable evaluation scopes. A context contains mutable variables and auxiliary object state.
- Do not reuse a document-specific lexer to compile a sliced generic expression: domain keywords may be perfectly valid property names in that expression.
- `not` is the logical negation, the word for `!` (`not ready`, `x is not empty`); inequality is `!=`, `<>`, `ne` or `neq`. ⚠️ Until 2026-10-20 `not` lexed as `T_NE` — a document older than that writing `a not b` must be rewritten `a ne b`.
- LogicalCalculator deliberately treats null as false and Boolean as itself; arbitrary objects do not acquire implicit truthiness.
- SourceWriting and SourceReading are a paired contract. Do not introduce Java-style escaping in a writer unless the reader actually decodes it.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-el/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Test sources are available under [src/test](../jmouse-el/src/test). Inspect their setup before running integration-dependent checks.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-el/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.el`

| Type | Responsibility / contract |
|---|---|
| [AbstractObjectContainer](../jmouse-el/src/main/java/org/jmouse/el/AbstractObjectContainer.java) | 🏗️ Abstract implementation of ObjectContainer, providing a basic storage mechanism for managing extensions using a key-value structure. |
| [CursorMatcher](../jmouse-el/src/main/java/org/jmouse/el/CursorMatcher.java) | Operations: `literal`, `lambda`, `function`, `sequence`, `bean`, `variableAlias`, `placeholder`, `PlaceholderMatcher`, `matches`, `VariableAliasMatcher`, `BeanMatcher`, `methodCall`. |
| [ExpressionLanguage](../jmouse-el/src/main/java/org/jmouse/el/ExpressionLanguage.java) | 🔎 Engine for parsing, compiling, caching, and evaluating expressions. |
| [ObjectContainer](../jmouse-el/src/main/java/org/jmouse/el/ObjectContainer.java) | 🎛️ A generic container for managing various types of extensions or objects. This interface provides methods for registering, removing, retrieving, and checking objects using their associated keys. |
| [StringReaderException](../jmouse-el/src/main/java/org/jmouse/el/StringReaderException.java) | Implements/extends `RuntimeException`. |
| [StringSource](../jmouse-el/src/main/java/org/jmouse/el/StringSource.java) | Represents a tokenized string, allowing efficient access to tokenized segments. |

#### `org.jmouse.el.evaluation`

| Type | Responsibility / contract |
|---|---|
| [BasicScopeValues](../jmouse-el/src/main/java/org/jmouse/el/evaluation/BasicScopeValues.java) | 📦 A simple implementation of ScopeValues. This class represents a single scope that stores variables in an internal key-value map. |
| [BasicValuesChain](../jmouse-el/src/main/java/org/jmouse/el/evaluation/BasicValuesChain.java) | 🔗 A basic implementation of ScopedChain. This class manages a stack-based chain of scopes , allowing for hierarchical variable storage and retrieval. |
| [DefaultEvaluationContext](../jmouse-el/src/main/java/org/jmouse/el/evaluation/DefaultEvaluationContext.java) | Implements/extends `EvaluationContext, BeanLookupContext`. Operations: `getInheritance`, `getObject`, `setObject`, `getScopedChain`, `setScopedChain`, `getValueAccessor`, `getExtensions`, `setExtensions`, `getConversion`, `getAttributeResolvers`, `getVirtualProperties`, `setVirtualProperties`. |
| [EvaluationContext](../jmouse-el/src/main/java/org/jmouse/el/evaluation/EvaluationContext.java) | ⚙️ Represents the evaluation context for expression evaluation. |
| [EvaluationException](../jmouse-el/src/main/java/org/jmouse/el/evaluation/EvaluationException.java) | Implements/extends `RuntimeException`. |
| [ExpressionLanguageConversion](../jmouse-el/src/main/java/org/jmouse/el/evaluation/ExpressionLanguageConversion.java) | An extension of PredefinedConversion tailored for the expression language. |
| [ExpressionLanguageValuesResolver](../jmouse-el/src/main/java/org/jmouse/el/evaluation/ExpressionLanguageValuesResolver.java) | A custom property value resolver for expression language evaluation. |
| [ReflectionClassPropertyResolver](../jmouse-el/src/main/java/org/jmouse/el/evaluation/ReflectionClassPropertyResolver.java) | Implements/extends `VirtualProperty`. Operations: `getType`, `getName`, `writeValue`, `readValue`, `isReadable`. |
| [ScopedChain](../jmouse-el/src/main/java/org/jmouse/el/evaluation/ScopedChain.java) | 🔗 Represents a scoped chain of variable contexts. This interface allows managing nested variable scopes , enabling hierarchical lookups and modifications of values. |
| [ScopedChainValuesAccessor](../jmouse-el/src/main/java/org/jmouse/el/evaluation/ScopedChainValuesAccessor.java) | Implements/extends `AbstractAccessor`. Operations: `get`, `set`. |
| [ScopeValues](../jmouse-el/src/main/java/org/jmouse/el/evaluation/ScopeValues.java) | 🔑 Represents a single scope for variable storage. This interface allows storing, retrieving, and modifying variables within a scope. |

#### `org.jmouse.el.evaluation.converter`

| Type | Responsibility / contract |
|---|---|
| [CharacterToStringConverter](../jmouse-el/src/main/java/org/jmouse/el/evaluation/converter/CharacterToStringConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |
| [IterableToIteratorConverter](../jmouse-el/src/main/java/org/jmouse/el/evaluation/converter/IterableToIteratorConverter.java) | Implements/extends `GenericConverter , Iterator >`. Operations: `convert`, `getSupportedTypes`. |
| [IteratorToCollection](../jmouse-el/src/main/java/org/jmouse/el/evaluation/converter/IteratorToCollection.java) | Implements/extends `GenericConverter , Collection >`. Operations: `convert`, `getSupportedTypes`. |
| [StringToCharacterConverter](../jmouse-el/src/main/java/org/jmouse/el/evaluation/converter/StringToCharacterConverter.java) | Implements/extends `GenericConverter`. Operations: `convert`, `getSupportedTypes`. |

#### `org.jmouse.el.extension`

| Type | Responsibility / contract |
|---|---|
| [Arguments](../jmouse-el/src/main/java/org/jmouse/el/extension/Arguments.java) | Represents a collection of arguments for an expression or function. |
| [Calculator](../jmouse-el/src/main/java/org/jmouse/el/extension/Calculator.java) | 🎛️ Functional interface for evaluating expressions with operators. |
| [CoreExtension](../jmouse-el/src/main/java/org/jmouse/el/extension/CoreExtension.java) | A standard implementation of the Extension interface. |
| [Extension](../jmouse-el/src/main/java/org/jmouse/el/extension/Extension.java) | 🔌 Core extension point for the view engine. |
| [ExtensionContainer](../jmouse-el/src/main/java/org/jmouse/el/extension/ExtensionContainer.java) | 🛠️ Manages various extensions used in the view engine, including functions, tests, filters, operators, and parsers. |
| [ExtensionException](../jmouse-el/src/main/java/org/jmouse/el/extension/ExtensionException.java) | Implements/extends `RuntimeException`. |
| [Filter](../jmouse-el/src/main/java/org/jmouse/el/extension/Filter.java) | Represents a filter in the expression language. |
| [FilterContainer](../jmouse-el/src/main/java/org/jmouse/el/extension/FilterContainer.java) | Implements/extends `AbstractObjectContainer`. Operations: `keyFor`, `toString`. |
| [Function](../jmouse-el/src/main/java/org/jmouse/el/extension/Function.java) | Represents a function within the expression language. |
| [FunctionContainer](../jmouse-el/src/main/java/org/jmouse/el/extension/FunctionContainer.java) | Implements/extends `AbstractObjectContainer`. Operations: `keyFor`, `toString`. |
| [i18nExtension](../jmouse-el/src/main/java/org/jmouse/el/extension/i18nExtension.java) | Implements/extends `Extension`. Operations: `getFunctions`, `execute`, `getName`. |
| [IllegalLambdaExecutionException](../jmouse-el/src/main/java/org/jmouse/el/extension/IllegalLambdaExecutionException.java) | Implements/extends `ExtensionException`. |
| [Lambda](../jmouse-el/src/main/java/org/jmouse/el/extension/Lambda.java) | Represents a user-defined lambda function with named parameters, default values, and an expression body. The lambda captures its execution context using a scoped chain. |
| [MethodImporter](../jmouse-el/src/main/java/org/jmouse/el/extension/MethodImporter.java) | 🧩 Utility for importing public methods as EL view functions. |
| [NumberQualifier](../jmouse-el/src/main/java/org/jmouse/el/extension/NumberQualifier.java) | Enum values: `BYTE`, `SHORT`, `CHARACTER`, `INT`, `LONG`, `FLOAT`, `DOUBLE`. |
| [OperationException](../jmouse-el/src/main/java/org/jmouse/el/extension/OperationException.java) | Implements/extends `RuntimeException`. |
| [Operator](../jmouse-el/src/main/java/org/jmouse/el/extension/Operator.java) | 🔢 Represents an operator used in expressions. Operators have precedence levels, names, and are associated with a specific token type. |
| [OperatorContainer](../jmouse-el/src/main/java/org/jmouse/el/extension/OperatorContainer.java) | 🏗️ A container for managing Operator instances. This implementation extends AbstractObjectContainer and uses the Token.Type as the unique key for each operator. |
| [StandardExtensionContainer](../jmouse-el/src/main/java/org/jmouse/el/extension/StandardExtensionContainer.java) | 🏗️ Standard implementation of ExtensionContainer. |
| [TestContainer](../jmouse-el/src/main/java/org/jmouse/el/extension/TestContainer.java) | Implements/extends `AbstractObjectContainer`. Operations: `keyFor`, `toString`. |

#### `org.jmouse.el.extension.attribute`

| Type | Responsibility / contract |
|---|---|
| [JavaBeanAttributeResolver](../jmouse-el/src/main/java/org/jmouse/el/extension/attribute/JavaBeanAttributeResolver.java) | AttributeResolver that resolves JavaBean properties via getter methods. ☕ |
| [ListAttributeResolver](../jmouse-el/src/main/java/org/jmouse/el/extension/attribute/ListAttributeResolver.java) | Implements/extends `AttributeResolver`. Operations: `supports`, `resolve`. |
| [MapAttributeResolver](../jmouse-el/src/main/java/org/jmouse/el/extension/attribute/MapAttributeResolver.java) | Implements/extends `AttributeResolver`. Operations: `supports`, `resolve`. |

#### `org.jmouse.el.extension.calculator`

| Type | Responsibility / contract |
|---|---|
| [ComparisonCalculator](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/ComparisonCalculator.java) | Performs comparison operations such as >, =, <=, ==, !=. This calculator ensures type safety and null handling using AnyComparator. |
| [ConcatCalculator](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/ConcatCalculator.java) | Implements/extends `Calculator`. Operations: `calculate`. |
| [LogicalCalculator](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/LogicalCalculator.java) | Enum representing logical operations in expressions. Each operator evaluates boolean conditions. |
| [MathematicCalculator](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/MathematicCalculator.java) | Enum representing arithmetic operations for expressions. Each operator performs a specific calculation on numeric operands. |
| [NullCoalesceCalculator](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/NullCoalesceCalculator.java) | Implements/extends `Calculator`. Operations: `calculate`. |
| [RangeCalculator](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/RangeCalculator.java) | Implements/extends `Calculator`. Operations: `calculate`. |
| [UnaryCalculator](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/UnaryCalculator.java) | Enum representing unary operations for expressions. Unary operators operate on a single operand. |

#### `org.jmouse.el.extension.calculator.operation`

| Type | Responsibility / contract |
|---|---|
| [Calculation](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/Calculation.java) | Facade for registering and executing operation handlers. |
| [CalculationOperationException](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/CalculationOperationException.java) | Implements/extends `RuntimeException`. |
| [IllegalOperationException](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/IllegalOperationException.java) | Implements/extends `CalculationOperationException`. |
| [OperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/OperationHandler.java) | Operations: `supports`, `execute`. |
| [OperationType](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/OperationType.java) | Enum values: `PLUS`, `MINUS`, `MULTIPLY`, `DIVIDE`, `MODULUS`, `EXPONENTIAL`, `INCREMENT`, `DECREMENT`. |

#### `org.jmouse.el.extension.calculator.operation.handler`

| Type | Responsibility / contract |
|---|---|
| [ArrayOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/ArrayOperationHandler.java) | 🧮 Handles arithmetic-like operations for Java arrays. Supports dynamic addition and removal of elements. |
| [BigDecimalOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/BigDecimalOperationHandler.java) | Implements/extends `OperationHandler`. Operations: `supports`, `execute`. |
| [BigIntegerOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/BigIntegerOperationHandler.java) | Implements/extends `OperationHandler`. Operations: `supports`, `execute`. |
| [ByteOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/ByteOperationHandler.java) | OperationHandler that performs byte‐based arithmetic. |
| [CharacterOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/CharacterOperationHandler.java) | OperationHandler that performs char‐based arithmetic. |
| [CollectionOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/CollectionOperationHandler.java) | OperationHandler implementation for Collection types. |
| [DoubleOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/DoubleOperationHandler.java) | OperationHandler that performs double‐based arithmetic. |
| [FloatOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/FloatOperationHandler.java) | OperationHandler that performs float‐based arithmetic. |
| [IntegerOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/IntegerOperationHandler.java) | OperationHandler that performs integer‐based arithmetic. |
| [LongOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/LongOperationHandler.java) | OperationHandler that performs long‐based arithmetic. |
| [ShortOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/ShortOperationHandler.java) | OperationHandler that performs short‐based arithmetic. |
| [StringNumberOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/StringNumberOperationHandler.java) | Implements/extends `OperationHandler`. Operations: `supports`, `execute`. |
| [StringStringOperationHandler](../jmouse-el/src/main/java/org/jmouse/el/extension/calculator/operation/handler/StringStringOperationHandler.java) | Implements/extends `OperationHandler`. Operations: `supports`, `execute`. |

#### `org.jmouse.el.extension.filter`

| Type | Responsibility / contract |
|---|---|
| [AbstractFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/AbstractFilter.java) | An abstract base class for filters. |
| [AfterFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/AfterFilter.java) | Everything after the last occurrence of a separator — "3300&#124;mΩ" &#124; after("&#124;") is mΩ. |
| [BeforeFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/BeforeFilter.java) | Everything up to the first occurrence of a separator — "3300&#124;mΩ" &#124; before("&#124;") is 3300. |
| [ClassFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/ClassFilter.java) | EL filter that resolves a Java Class by its name. 🧩 |
| [DefaultFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/DefaultFilter.java) | A filter that provides a default value. |
| [FilterFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/FilterFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [FirstFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/FirstFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [JoinFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/JoinFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [LastFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/LastFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [LengthFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/LengthFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [LowerFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/LowerFilter.java) | A filter that converts the input value to lowercase. |
| [MapFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/MapFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [SoutFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/SoutFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [SplitFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/SplitFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [SubFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/SubFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [TrimFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/TrimFilter.java) | A filter that trims leading and trailing whitespace from a String. |
| [TypeFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/TypeFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [UpperFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/UpperFilter.java) | A filter that converts the input string to uppercase. |
| [ViaFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/ViaFilter.java) | value &#124; via("name") — the value, through the converter registered under that name. |

#### `org.jmouse.el.extension.filter.converter`

| Type | Responsibility / contract |
|---|---|
| [ToArrayFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToArrayFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToBigDecimalFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToBigDecimalFilter.java) | A filter that converts the input value to a BigDecimal. |
| [ToBigIntFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToBigIntFilter.java) | A filter that converts the input value to a BigInteger. |
| [ToBooleanFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToBooleanFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToByteFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToByteFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToCharacterFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToCharacterFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToDoubleFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToDoubleFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToFloatFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToFloatFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToInstantFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToInstantFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToIntFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToIntFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToIteratorFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToIteratorFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToListFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToListFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToLongFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToLongFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToShortFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToShortFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [ToStringFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/converter/ToStringFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |

#### `org.jmouse.el.extension.filter.datetime`

| Type | Responsibility / contract |
|---|---|
| [MinusDaysFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/datetime/MinusDaysFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |
| [PlusDaysFilter](../jmouse-el/src/main/java/org/jmouse/el/extension/filter/datetime/PlusDaysFilter.java) | Implements/extends `AbstractFilter`. Operations: `apply`, `getName`. |

#### `org.jmouse.el.extension.function`

| Type | Responsibility / contract |
|---|---|
| [ClassFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/ClassFunction.java) | EL function that resolves a Java Class by its fully qualified name. 🧩 |
| [GetVariableFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/GetVariableFunction.java) | Implements/extends `Function`. Operations: `execute`, `getName`. |
| [MaxFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/MaxFunction.java) | Implements/extends `Function`. Operations: `execute`, `getName`. |
| [MinFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/MinFunction.java) | Implements/extends `Function`. Operations: `execute`, `getName`. |
| [SetVariableFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/SetVariableFunction.java) | Implements the "set" function for the expression language. |

#### `org.jmouse.el.extension.function.datetime`

| Type | Responsibility / contract |
|---|---|
| [NowFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/datetime/NowFunction.java) | Implements/extends `Function`. Operations: `execute`, `getName`. |

#### `org.jmouse.el.extension.function.mathematic`

| Type | Responsibility / contract |
|---|---|
| [ExponentaFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/mathematic/ExponentaFunction.java) | Implements/extends `Function`. Operations: `execute`, `getName`. |
| [SquareRootFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/mathematic/SquareRootFunction.java) | Implements/extends `Function`. Operations: `execute`, `getName`. |

#### `org.jmouse.el.extension.function.reflection`

| Type | Responsibility / contract |
|---|---|
| [JavaReflectedFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/reflection/JavaReflectedFunction.java) | 🔭 Reflectively-backed EL function (overload-aware). |

#### `org.jmouse.el.extension.function.string`

| Type | Responsibility / contract |
|---|---|
| [LcfirstFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/string/LcfirstFunction.java) | Implements/extends `Function`. Operations: `execute`, `getName`. |
| [LclastFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/string/LclastFunction.java) | Implements/extends `Function`. Operations: `execute`, `getName`. |
| [UcfirstFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/string/UcfirstFunction.java) | Implements/extends `Function`. Operations: `execute`, `getName`. |
| [UclastFunction](../jmouse-el/src/main/java/org/jmouse/el/extension/function/string/UclastFunction.java) | Implements/extends `Function`. Operations: `execute`, `getName`. |

#### `org.jmouse.el.extension.operator`

| Type | Responsibility / contract |
|---|---|
| [AttributeAccessOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/AttributeAccessOperator.java) | Enum values: `ACCESS`, `T_BACKSLASH`. |
| [ComparisonOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/ComparisonOperator.java) | Represents comparison operators (>, =, <=, ==, !=). |
| [ConcatOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/ConcatOperator.java) | Represents the "~" concatenation operator (T_TILDA). |
| [FilterOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/FilterOperator.java) | Represents the "&#124;" filter operator (IS). |
| [InOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/InOperator.java) | Enum values: `IN`, `T_IN`. |
| [LogicalOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/LogicalOperator.java) | Enum representing logical operators. Logical operators evaluate boolean expressions. |
| [MathematicOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/MathematicOperator.java) | 🔢 Enum representing binary operators used in expressions. Each operator has a token type, a name, and a precedence level that determines the order of evaluation in arithmetic expressions. |
| [NullCoalesceOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/NullCoalesceOperator.java) | Represents the "??" null-coalesce operator (IS). |
| [RangeOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/RangeOperator.java) | Represents the ".." range operator (T_DOUBLE_DOT). |
| [TestOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/TestOperator.java) | Represents the "IS" test operator (IS). |
| [UnaryOperator](../jmouse-el/src/main/java/org/jmouse/el/extension/operator/UnaryOperator.java) | Enum representing **unary operators** used in expressions. Unary operators operate on a **single operand** and have high precedence. |

#### `org.jmouse.el.language.lexer`

| Type | Responsibility / contract |
|---|---|
| [LanguageToken](../jmouse-el/src/main/java/org/jmouse/el/language/lexer/LanguageToken.java) | Enum values: `T_IF`, `T_ELSE_IF`, `T_ELSE`, `T_FUNCTION`. |

#### `org.jmouse.el.language.node`

| Type | Responsibility / contract |
|---|---|
| [IfBranchNode](../jmouse-el/src/main/java/org/jmouse/el/language/node/IfBranchNode.java) | Implements/extends `ExpressionsNode`. Operations: `getCondition`, `setCondition`, `isElse`, `matches`, `evaluate`. |
| [IfNode](../jmouse-el/src/main/java/org/jmouse/el/language/node/IfNode.java) | Implements/extends `ExpressionsNode`. Operations: `addBranch`, `getBranches`, `evaluate`. |

#### `org.jmouse.el.language.parser`

| Type | Responsibility / contract |
|---|---|
| [AbstractBlockParser](../jmouse-el/src/main/java/org/jmouse/el/language/parser/AbstractBlockParser.java) | Implements/extends `AbstractParser`. Operations: `parse`, `supports`. |
| [AbstractBodyParser](../jmouse-el/src/main/java/org/jmouse/el/language/parser/AbstractBodyParser.java) | Implements/extends `AbstractBlockParser`. |
| [AbstractBranchParser](../jmouse-el/src/main/java/org/jmouse/el/language/parser/AbstractBranchParser.java) | Implements/extends `AbstractBlockParser`. |
| [AbstractMultiBodyParser](../jmouse-el/src/main/java/org/jmouse/el/language/parser/AbstractMultiBodyParser.java) | Implements/extends `AbstractBlockParser`. |
| [IfParser](../jmouse-el/src/main/java/org/jmouse/el/language/parser/IfParser.java) | Implements/extends `AbstractBranchParser`. |
| [StatementsParser](../jmouse-el/src/main/java/org/jmouse/el/language/parser/StatementsParser.java) | Reads a braced body of statements, and keeps what a person wrote around them. |

#### `org.jmouse.el.lexer`

| Type | Responsibility / contract |
|---|---|
| [BasicToken](../jmouse-el/src/main/java/org/jmouse/el/lexer/BasicToken.java) | BasicToken defines token types for the view language. |
| [Cursor](../jmouse-el/src/main/java/org/jmouse/el/lexer/Cursor.java) | DirectAccess implementation of TokenCursor that provides navigation through a list of tokens. |
| [DefaultLexer](../jmouse-el/src/main/java/org/jmouse/el/lexer/DefaultLexer.java) | Implements/extends `Lexer`. Operations: `tokenize`. |
| [DefaultTokenizer](../jmouse-el/src/main/java/org/jmouse/el/lexer/DefaultTokenizer.java) | DirectAccess implementation of Tokenizer that processes text and produces a list of type entries. |
| [ExpressionRecognizer](../jmouse-el/src/main/java/org/jmouse/el/lexer/ExpressionRecognizer.java) | Implements/extends `CompositeRecognizer`. |
| [ExpressionSplitter](../jmouse-el/src/main/java/org/jmouse/el/lexer/ExpressionSplitter.java) | Splits expressions into individual tokens such as identifiers, numbers, strings, and operators. |
| [Lexer](../jmouse-el/src/main/java/org/jmouse/el/lexer/Lexer.java) | Operations: `tokenize`. |
| [RawToken](../jmouse-el/src/main/java/org/jmouse/el/lexer/RawToken.java) | Value record: `(String value, int line, int offset, Type type)`. |
| [Splitter](../jmouse-el/src/main/java/org/jmouse/el/lexer/Splitter.java) | Defines a generic interface for splitting text into tokens or other structures. |
| [Syntax](../jmouse-el/src/main/java/org/jmouse/el/lexer/Syntax.java) | Regular-expression fragments for strings, numeric forms, identifiers, comments, newlines, symbols and logical operators used by lexer components. |
| [SyntaxErrorException](../jmouse-el/src/main/java/org/jmouse/el/lexer/SyntaxErrorException.java) | Thrown when a syntax error occurs during the parser process. |
| [Token](../jmouse-el/src/main/java/org/jmouse/el/lexer/Token.java) | Represents a lexical type in a view lexer, storing its value, type, position, and line number. |
| [TokenCursor](../jmouse-el/src/main/java/org/jmouse/el/lexer/TokenCursor.java) | TokenCursor provides an advanced API for navigating a stream of tokens. It offers methods for lookahead, shifting, retracting, expectation checking, and managing savepoints for backtracking. |
| [TokenizableSource](../jmouse-el/src/main/java/org/jmouse/el/lexer/TokenizableSource.java) | Represents a tokenizable character sequence that allows type extraction and iteration. |
| [Tokenizer](../jmouse-el/src/main/java/org/jmouse/el/lexer/Tokenizer.java) | Defines a tokenizer that splits input text into a sequence of Token.Type instances. |

#### `org.jmouse.el.lexer.recognizer`

| Type | Responsibility / contract |
|---|---|
| [CompositeRecognizer](../jmouse-el/src/main/java/org/jmouse/el/lexer/recognizer/CompositeRecognizer.java) | Asks its recognizers in priority order, and the first one that recognizes a token answers. |
| [EnumTokenRecognizer](../jmouse-el/src/main/java/org/jmouse/el/lexer/recognizer/EnumTokenRecognizer.java) | Recognizes tokens based on an enumeration type implementing Token.Type. |
| [Recognizer](../jmouse-el/src/main/java/org/jmouse/el/lexer/recognizer/Recognizer.java) | Represents a generic recognizer that attempts to identify a result from a given subject. |

#### `org.jmouse.el.lexer.support`

| Type | Responsibility / contract |
|---|---|
| [CursorLookahead](../jmouse-el/src/main/java/org/jmouse/el/lexer/support/CursorLookahead.java) | The primitives every construct-matcher is built from. 🔎 |
| [EnumFinder](../jmouse-el/src/main/java/org/jmouse/el/lexer/support/EnumFinder.java) | 🔍 Utility class for searching an enumeration constant based on string value. |
| [Finder](../jmouse-el/src/main/java/org/jmouse/el/lexer/support/Finder.java) | Operations: `find`. |
| [SourceReading](../jmouse-el/src/main/java/org/jmouse/el/lexer/support/SourceReading.java) | Reads positions and raw text back out of a TokenCursor. 📖 |
| [SourceWriting](../jmouse-el/src/main/java/org/jmouse/el/lexer/support/SourceWriting.java) | Writes values back out in a form SourceReading reads the same way. ✍️ |

#### `org.jmouse.el.node`

| Type | Responsibility / contract |
|---|---|
| [AbstractExpression](../jmouse-el/src/main/java/org/jmouse/el/node/AbstractExpression.java) | AbstractExpression is the base class for tag nodes in the AST. |
| [AbstractNode](../jmouse-el/src/main/java/org/jmouse/el/node/AbstractNode.java) | An abstract implementation of the Node interface, providing basic parent-child relationships for hierarchical structures. |
| [BasicNode](../jmouse-el/src/main/java/org/jmouse/el/node/BasicNode.java) | Represents a basic implementation of a Node with an associated Token. |
| [Expression](../jmouse-el/src/main/java/org/jmouse/el/node/Expression.java) | 🧩 Represents an expression node that can be evaluated within an EvaluationContext. |
| [Expressions](../jmouse-el/src/main/java/org/jmouse/el/node/Expressions.java) | Dispatch for ExpressionVisitor — the type switch, written once. |
| [ExpressionVisitor](../jmouse-el/src/main/java/org/jmouse/el/node/ExpressionVisitor.java) | A visitor that returns something — for turning an expression into another form. |
| [Node](../jmouse-el/src/main/java/org/jmouse/el/node/Node.java) | Represents a hierarchical node structure with parent-child relationships. |
| [Trivia](../jmouse-el/src/main/java/org/jmouse/el/node/Trivia.java) | A comment, or a blank line — what a person wrote that the grammar does not read. 🗒️ |
| [Visitor](../jmouse-el/src/main/java/org/jmouse/el/node/Visitor.java) | Expression-tree visitor callbacks for concrete node shapes; every overload defaults to no action so consumers override only relevant visits. |

#### `org.jmouse.el.node.expression`

| Type | Responsibility / contract |
|---|---|
| [ArgumentsNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/ArgumentsNode.java) | Represents a node that aggregates a list of argument expressions. |
| [ArrayNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/ArrayNode.java) | Implements/extends `ArgumentsNode`. Operations: `evaluate`, `accept`, `toSource`, `toString`. |
| [BeanAccessException](../jmouse-el/src/main/java/org/jmouse/el/node/expression/BeanAccessException.java) | Implements/extends `RuntimeException`. |
| [BeanAccessNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/BeanAccessNode.java) | Implements/extends `AbstractExpression`. Operations: `getBean`, `setBean`, `getType`, `setType`, `getAction`, `setAction`, `evaluate`, `toSource`, `toString`, `MethodCall`, `ConstantAccess`, `FieldAccess`. |
| [BinaryOperation](../jmouse-el/src/main/java/org/jmouse/el/node/expression/BinaryOperation.java) | Represents a binary operation node in the Abstract Syntax Tree (AST). |
| [ExpressionsNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/ExpressionsNode.java) | Implements/extends `AbstractExpression`. Operations: `addExpression`, `hasExpression`, `getExpressions`. |
| [FilterNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/FilterNode.java) | Represents a filter expression node that applies a filter to an expression. |
| [FilterNotFoundException](../jmouse-el/src/main/java/org/jmouse/el/node/expression/FilterNotFoundException.java) | Implements/extends `EvaluationException`. |
| [FunctionNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/FunctionNode.java) | Represents a function call expression node. |
| [FunctionNotFoundException](../jmouse-el/src/main/java/org/jmouse/el/node/expression/FunctionNotFoundException.java) | Implements/extends `EvaluationException`. |
| [InOperationNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/InOperationNode.java) | Implements/extends `AbstractExpression`. Operations: `getLeft`, `setLeft`, `getRight`, `setRight`, `evaluate`, `toSource`, `toString`. |
| [KeyValueNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/KeyValueNode.java) | Represents a key-value pair expression node. |
| [LambdaNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/LambdaNode.java) | Implements/extends `AbstractExpression`. Operations: `getParameters`, `setParameters`, `getBody`, `setBody`, `evaluate`, `toString`. |
| [LiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/LiteralNode.java) | Represents a literal expression node that encapsulates a constant value. |
| [MapNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/MapNode.java) | Represents a map literal node in the expression language. |
| [NameNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/NameNode.java) | Implements/extends `AbstractExpression`. Operations: `getName`, `setName`, `getAlias`, `setAlias`, `evaluate`, `toSource`, `toString`. |
| [NameSetNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/NameSetNode.java) | Implements/extends `ArrayNode`. Operations: `getSet`, `toString`. |
| [NullSafeFallbackNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/NullSafeFallbackNode.java) | Represents a null-coalescing expression in the view language. |
| [ParameterNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/ParameterNode.java) | Implements/extends `AbstractExpression`. Operations: `setName`, `setDefaultValue`, `getName`, `getDefaultValue`, `toString`. |
| [ParameterSetNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/ParameterSetNode.java) | Implements/extends `AbstractExpression`. Operations: `getSet`, `evaluate`, `toString`. |
| [PlaceholderNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/PlaceholderNode.java) | Implements/extends `AbstractExpression`. Operations: `getProperty`, `setProperty`, `toString`, `toSource`. |
| [PropertyNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/PropertyNode.java) | Represents a property expression node. |
| [RangeNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/RangeNode.java) | Implements/extends `AbstractExpression`. Operations: `getStart`, `setStart`, `getEnd`, `setEnd`, `evaluate`, `toString`. |
| [ScopedCallNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/ScopedCallNode.java) | Implements/extends `FunctionNode`. Operations: `getScope`, `setScope`, `getName`, `toString`. |
| [SpanNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/SpanNode.java) | Implements/extends `AbstractExpression`. Operations: `lineNumber`, `position`, `toString`, `of`, `evaluate`. |
| [TernaryNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/TernaryNode.java) | Represents a ternary (conditional) expression in the view language. |
| [TestNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/TestNode.java) | Represents a test expression node in the expression language. |
| [TestNotFoundException](../jmouse-el/src/main/java/org/jmouse/el/node/expression/TestNotFoundException.java) | Implements/extends `EvaluationException`. |
| [UnaryOperation](../jmouse-el/src/main/java/org/jmouse/el/node/expression/UnaryOperation.java) | Represents a unary operation tag node in the Abstract Syntax Tree (AST). |
| [VariableAliasNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/VariableAliasNode.java) | Implements/extends `AbstractExpression`. Operations: `getAliasName`, `setAliasName`, `getVariableName`, `setVariableName`, `evaluate`, `toString`. |

#### `org.jmouse.el.node.expression.literal`

| Type | Responsibility / contract |
|---|---|
| [BigDecimalLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/BigDecimalLiteralNode.java) | Represents a literal expression node that encapsulates a BigDecimal value. |
| [BigIntegerLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/BigIntegerLiteralNode.java) | Represents a literal expression node that encapsulates a BigInteger value. |
| [BooleanLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/BooleanLiteralNode.java) | Represents a literal expression node that encapsulates a Boolean value. |
| [ByteLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/ByteLiteralNode.java) | Represents a literal expression node that encapsulates a Byte value. |
| [CharacterLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/CharacterLiteralNode.java) | Represents a literal expression node that encapsulates a Character value. |
| [DoubleLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/DoubleLiteralNode.java) | Represents a literal expression node that encapsulates a Double value. |
| [FloatLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/FloatLiteralNode.java) | Represents a literal expression node that encapsulates a Float value. |
| [IntegerLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/IntegerLiteralNode.java) | Represents a literal expression node that encapsulates a Integer value. |
| [LongLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/LongLiteralNode.java) | Represents a literal expression node that encapsulates a Long value. |
| [NullLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/NullLiteralNode.java) | Represents a literal expression node that encapsulates a null value. |
| [ShortLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/ShortLiteralNode.java) | Represents a literal expression node that encapsulates a Short value. |
| [StringLiteralNode](../jmouse-el/src/main/java/org/jmouse/el/node/expression/literal/StringLiteralNode.java) | Represents a literal expression node for string values. |

#### `org.jmouse.el.node.expression.unary`

| Type | Responsibility / contract |
|---|---|
| [NegateUnaryOperation](../jmouse-el/src/main/java/org/jmouse/el/node/expression/unary/NegateUnaryOperation.java) | `!x` and `not x`. Implements/extends `UnaryOperation`. Operations: `evaluate`, `toSource`, `toString`. |
| [PostfixUnaryOperation](../jmouse-el/src/main/java/org/jmouse/el/node/expression/unary/PostfixUnaryOperation.java) | Represents a postfix unary operation in the Abstract Syntax Tree (AST). |
| [PrefixUnaryOperation](../jmouse-el/src/main/java/org/jmouse/el/node/expression/unary/PrefixUnaryOperation.java) | Represents a prefix unary operation in the Abstract Syntax Tree (AST). |

#### `org.jmouse.el.parser`

| Type | Responsibility / contract |
|---|---|
| [AbstractParser](../jmouse-el/src/main/java/org/jmouse/el/parser/AbstractParser.java) | Implements/extends `Parser`. |
| [ArrayParser](../jmouse-el/src/main/java/org/jmouse/el/parser/ArrayParser.java) | Parses an array expression. |
| [AutodetectFirstParser](../jmouse-el/src/main/java/org/jmouse/el/parser/AutodetectFirstParser.java) | Implements/extends `Parser`. Operations: `parse`. |
| [BeanAccessParser](../jmouse-el/src/main/java/org/jmouse/el/parser/BeanAccessParser.java) | Implements/extends `Parser`. Operations: `parse`, `supports`. |
| [DefaultParserContext](../jmouse-el/src/main/java/org/jmouse/el/parser/DefaultParserContext.java) | Implements/extends `ParserContext`. Operations: `getContextScope`, `getAttributeResolvers`, `getTagParser`, `addTagParser`, `getParser`, `addParser`, `getOperator`, `addOperator`, `getFunction`, `addFunction`, `getTest`, `addTest`. |
| [ExpressionParser](../jmouse-el/src/main/java/org/jmouse/el/parser/ExpressionParser.java) | Implements/extends `Parser`. Operations: `parse`. |
| [FilterParser](../jmouse-el/src/main/java/org/jmouse/el/parser/FilterParser.java) | Parses filters calls and their arguments if present. |
| [FunctionParser](../jmouse-el/src/main/java/org/jmouse/el/parser/FunctionParser.java) | 🧩 Parses function calls and their arguments. |
| [LambdaParser](../jmouse-el/src/main/java/org/jmouse/el/parser/LambdaParser.java) | Parses a lambda expression of the form (params) -> body or (params) -> { body } and constructs a LambdaNode. |
| [LiteralParser](../jmouse-el/src/main/java/org/jmouse/el/parser/LiteralParser.java) | Parses literal tokens (string, boolean, null, character, and numeric literals) and converts them into corresponding AST nodes. |
| [MapParser](../jmouse-el/src/main/java/org/jmouse/el/parser/MapParser.java) | Parses a map literal expression. |
| [OperatorParser](../jmouse-el/src/main/java/org/jmouse/el/parser/OperatorParser.java) | Parses arithmetic and logical expressions with operator precedence. |
| [ParseException](../jmouse-el/src/main/java/org/jmouse/el/parser/ParseException.java) | Implements/extends `RuntimeException`. |
| [Parser](../jmouse-el/src/main/java/org/jmouse/el/parser/Parser.java) | General interface for all token-based parsers in the expression language system. |
| [ParserContainer](../jmouse-el/src/main/java/org/jmouse/el/parser/ParserContainer.java) | Implements/extends `AbstractObjectContainer , Parser>`. Operations: `keyFor`, `toString`. |
| [ParserContext](../jmouse-el/src/main/java/org/jmouse/el/parser/ParserContext.java) | Represents a evaluation for managing parsers and options in a templating system. |
| [ParserOptions](../jmouse-el/src/main/java/org/jmouse/el/parser/ParserOptions.java) | Represents configurable options for the parser. |
| [PlaceholderParser](../jmouse-el/src/main/java/org/jmouse/el/parser/PlaceholderParser.java) | Implements/extends `Parser`. Operations: `parse`, `supports`. |
| [PrimaryExpressionParser](../jmouse-el/src/main/java/org/jmouse/el/parser/PrimaryExpressionParser.java) | Implements/extends `Parser`. Operations: `parse`. |
| [PropertyParser](../jmouse-el/src/main/java/org/jmouse/el/parser/PropertyParser.java) | A property path — order.customer.name, entry[quantity], meta[stock.after]. |
| [RangeParser](../jmouse-el/src/main/java/org/jmouse/el/parser/RangeParser.java) | Implements/extends `Parser`. Operations: `parse`. |
| [ScopedCallParser](../jmouse-el/src/main/java/org/jmouse/el/parser/ScopedCallParser.java) | Implements/extends `Parser`. Operations: `parse`. |
| [TagParser](../jmouse-el/src/main/java/org/jmouse/el/parser/TagParser.java) | Operations: `parse`, `getName`. |
| [TagParserContainer](../jmouse-el/src/main/java/org/jmouse/el/parser/TagParserContainer.java) | 🏗️ A container for managing TagParser instances. This implementation extends AbstractObjectContainer and uses the parser's name as its unique key. |
| [TestParser](../jmouse-el/src/main/java/org/jmouse/el/parser/TestParser.java) | Parses tests calls and their arguments if present. |
| [VariableAliasParser](../jmouse-el/src/main/java/org/jmouse/el/parser/VariableAliasParser.java) | Implements/extends `Parser`. Operations: `parse`, `supports`. |

#### `org.jmouse.el.parser.sub`

| Type | Responsibility / contract |
|---|---|
| [ArgumentsParser](../jmouse-el/src/main/java/org/jmouse/el/parser/sub/ArgumentsParser.java) | Parses a comma-separated list of arguments. |
| [KeyValueParser](../jmouse-el/src/main/java/org/jmouse/el/parser/sub/KeyValueParser.java) | Parses a comma-separated list of key-value pairs. |
| [NamesParser](../jmouse-el/src/main/java/org/jmouse/el/parser/sub/NamesParser.java) | Implements/extends `Parser`. Operations: `parse`. |
| [ParametersParser](../jmouse-el/src/main/java/org/jmouse/el/parser/sub/ParametersParser.java) | Implements/extends `Parser`. Operations: `parse`. |
| [ParenthesesParser](../jmouse-el/src/main/java/org/jmouse/el/parser/sub/ParenthesesParser.java) | Implements/extends `Parser`. Operations: `parse`. |

#### `org.jmouse.el.template`

| Type | Responsibility / contract |
|---|---|
| [ArrayContent](../jmouse-el/src/main/java/org/jmouse/el/template/ArrayContent.java) | 📋 ArrayContent is an implementation of the Content interface using a dynamically expandable character array. |
| [Block](../jmouse-el/src/main/java/org/jmouse/el/template/Block.java) | Implements/extends `Component`. |
| [Cache](../jmouse-el/src/main/java/org/jmouse/el/template/Cache.java) | Operations: `get`, `put`, `equals`, `hashCode`, `toString`, `ObjectKey`. |
| [Component](../jmouse-el/src/main/java/org/jmouse/el/template/Component.java) | Operations: `name`, `node`, `source`. |
| [Content](../jmouse-el/src/main/java/org/jmouse/el/template/Content.java) | 🔣 Represents a container for rendered content. |
| [ContentWriterException](../jmouse-el/src/main/java/org/jmouse/el/template/ContentWriterException.java) | Implements/extends `RuntimeException`. |
| [DefaultTemplate](../jmouse-el/src/main/java/org/jmouse/el/template/DefaultTemplate.java) | Implements/extends `Template`. Operations: `setInitialized`, `isInitialized`, `newContext`, `getSource`, `getRoot`, `getName`, `setMacro`, `getMacro`, `setBlock`, `getBlock`, `getParent`, `setParent`. |
| [EmptyNode](../jmouse-el/src/main/java/org/jmouse/el/template/EmptyNode.java) | Represents an empty renderable node. |
| [Engine](../jmouse-el/src/main/java/org/jmouse/el/template/Engine.java) | Operations: `getTemplate`, `getExtensions`, `getLoader`, `setLoader`, `loadTemplate`, `parseTemplate`, `newTemplate`, `getTokenCursor`, `getSource`. |
| [Inheritance](../jmouse-el/src/main/java/org/jmouse/el/template/Inheritance.java) | Represents a stack of renderable templates for managing view inheritance. |
| [InitializerVisitor](../jmouse-el/src/main/java/org/jmouse/el/template/InitializerVisitor.java) | The InitializerVisitor is responsible for traversing the view's AST and registering macros, blocks, and handling view extension directives before the rendering process. |
| [LinkedListInheritance](../jmouse-el/src/main/java/org/jmouse/el/template/LinkedListInheritance.java) | Represents a stack for managing view inheritance hierarchy. |
| [Macro](../jmouse-el/src/main/java/org/jmouse/el/template/Macro.java) | Implements/extends `Component`. |
| [NodeVisitor](../jmouse-el/src/main/java/org/jmouse/el/template/NodeVisitor.java) | Defines a visitor for processing renderable nodes. |
| [Renderer](../jmouse-el/src/main/java/org/jmouse/el/template/Renderer.java) | Defines the contract for rendering a view into a Content object. |
| [RendererVisitor](../jmouse-el/src/main/java/org/jmouse/el/template/RendererVisitor.java) | The RendererVisitor traverses the view AST and produces the final rendered output. |
| [RenderingException](../jmouse-el/src/main/java/org/jmouse/el/template/RenderingException.java) | Implements/extends `RuntimeException`. |
| [Template](../jmouse-el/src/main/java/org/jmouse/el/template/Template.java) | Represents a view in the view engine. |
| [TemplateBlock](../jmouse-el/src/main/java/org/jmouse/el/template/TemplateBlock.java) | Represents a view block. |
| [TemplateCoreExtension](../jmouse-el/src/main/java/org/jmouse/el/template/TemplateCoreExtension.java) | Core extension for the view engine that adds view-specific parsing capabilities. |
| [TemplateEngine](../jmouse-el/src/main/java/org/jmouse/el/template/TemplateEngine.java) | TemplateEngine is the core class responsible for loading, parsing, and caching templates. |
| [TemplateException](../jmouse-el/src/main/java/org/jmouse/el/template/TemplateException.java) | Implements/extends `RuntimeException`. |
| [TemplateExtension](../jmouse-el/src/main/java/org/jmouse/el/template/TemplateExtension.java) | Implements/extends `Extension`. |
| [TemplateMacro](../jmouse-el/src/main/java/org/jmouse/el/template/TemplateMacro.java) | Represents a view macro, which binds a macro name to its corresponding macro definition. |
| [TemplateRegistry](../jmouse-el/src/main/java/org/jmouse/el/template/TemplateRegistry.java) | The TemplateRegistry class maintains the definitions of blocks and macros for a view. It allows for registering, retrieving, checking existence, merging, and importing of definitions across templates. |
| [TemplateRenderer](../jmouse-el/src/main/java/org/jmouse/el/template/TemplateRenderer.java) | Default implementation of the Renderer interface. |
| [TemplateResolver](../jmouse-el/src/main/java/org/jmouse/el/template/TemplateResolver.java) | Operations: `resolve`. |
| [WriterContent](../jmouse-el/src/main/java/org/jmouse/el/template/WriterContent.java) | 📄 WriterContent is a Content implementation that streams output directly to a Writer. |

#### `org.jmouse.el.template.evaluation`

| Type | Responsibility / contract |
|---|---|
| [LoopVariables](../jmouse-el/src/main/java/org/jmouse/el/template/evaluation/LoopVariables.java) | Operations: `getIndex`, `setIndex`, `getValue`, `setValue`, `isLast`, `setLast`, `isFirst`, `setFirst`, `getKey`, `setKey`, `getIndex0`, `setIndex0`. |

#### `org.jmouse.el.template.lexer`

| Type | Responsibility / contract |
|---|---|
| [TemplateRecognizer](../jmouse-el/src/main/java/org/jmouse/el/template/lexer/TemplateRecognizer.java) | Implements/extends `CompositeRecognizer`. Operations: `recognize`, `order`. |
| [TemplateSplitter](../jmouse-el/src/main/java/org/jmouse/el/template/lexer/TemplateSplitter.java) | Splits raw text into tokens, distinguishing between plain text and templating expressions. |
| [TemplateToken](../jmouse-el/src/main/java/org/jmouse/el/template/lexer/TemplateToken.java) | Enumeration of view tokens used in the view engine. Each type has a unique type identifier and example string(s) associated with it. |
| [TemplateTokenizer](../jmouse-el/src/main/java/org/jmouse/el/template/lexer/TemplateTokenizer.java) | Implements/extends `DefaultTokenizer`. |

#### `org.jmouse.el.template.loader`

| Type | Responsibility / contract |
|---|---|
| [AbstractLoader](../jmouse-el/src/main/java/org/jmouse/el/template/loader/AbstractLoader.java) | Implements/extends `TemplateLoader`. Operations: `setPrefix`, `setSuffix`, `setCharset`, `getPrefix`, `getSuffix`, `getCharset`. |
| [ClasspathLoader](../jmouse-el/src/main/java/org/jmouse/el/template/loader/ClasspathLoader.java) | Implements/extends `AbstractLoader`. Operations: `load`. |
| [StringLoader](../jmouse-el/src/main/java/org/jmouse/el/template/loader/StringLoader.java) | Implements/extends `TemplateLoader`. Operations: `load`. |
| [TemplateLoader](../jmouse-el/src/main/java/org/jmouse/el/template/loader/TemplateLoader.java) | Operations: `load`. |
| [TemplateLoaderException](../jmouse-el/src/main/java/org/jmouse/el/template/loader/TemplateLoaderException.java) | Implements/extends `RuntimeException`. |

#### `org.jmouse.el.template.node`

| Type | Responsibility / contract |
|---|---|
| [ApplyNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/ApplyNode.java) | Implements/extends `AbstractNode`. Operations: `getChain`, `setChain`, `getBody`, `setBody`, `accept`, `toString`. |
| [BlockNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/BlockNode.java) | Implements/extends `AbstractNode`. Operations: `getName`, `setName`, `getBody`, `setBody`, `isOverride`, `setOverride`, `accept`. |
| [CacheNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/CacheNode.java) | Represents a cache directive in a view. |
| [ContainerNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/ContainerNode.java) | Implements/extends `AbstractNode`. Operations: `accept`. |
| [DoNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/DoNode.java) | Implements/extends `AbstractNode`. Operations: `getExpression`, `setExpression`, `accept`, `toString`. |
| [EmbedNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/EmbedNode.java) | Implements/extends `AbstractNode`. Operations: `getPath`, `setPath`, `getWith`, `setWith`, `getBody`, `setBody`, `accept`, `toString`. |
| [ExtendsNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/ExtendsNode.java) | Represents an "extends" node in a view. |
| [ForNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/ForNode.java) | Represents a "for" loop node in the view engine. |
| [IfNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/IfNode.java) | Implements/extends `AbstractNode`. Operations: `addBranch`, `getBranches`, `accept`, `toString`. |
| [IncludeNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/IncludeNode.java) | Implements/extends `AbstractNode`. Operations: `getPath`, `accept`, `toString`. |
| [MacroNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/MacroNode.java) | Implements/extends `AbstractNode`. Operations: `getName`, `setName`, `getArguments`, `setArguments`, `getBody`, `setBody`, `getDefaultValue`, `setDefaultValues`, `accept`, `toString`. |
| [ParentNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/ParentNode.java) | Implements/extends `AbstractNode`. Operations: `getName`, `setName`, `accept`, `toString`. |
| [PrintNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/PrintNode.java) | Implements/extends `AbstractNode`. Operations: `getExpression`, `accept`. |
| [RenderNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/RenderNode.java) | Implements/extends `AbstractNode`. Operations: `getName`, `setName`, `accept`, `toString`. |
| [ScopeNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/ScopeNode.java) | Implements/extends `AbstractNode`. Operations: `getWith`, `setWith`, `getBody`, `setBody`, `accept`, `toString`. |
| [SetNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/SetNode.java) | Implements/extends `AbstractNode`. Operations: `getVariable`, `setVariable`, `getValue`, `setValue`, `accept`, `toString`. |
| [TextNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/TextNode.java) | Represents a raw text node in a templating system. |
| [URLNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/URLNode.java) | Implements/extends `AbstractNode`. Operations: `getRawURL`, `setRawURL`, `accept`, `toString`. |
| [UseNode](../jmouse-el/src/main/java/org/jmouse/el/template/node/UseNode.java) | Implements/extends `AbstractNode`. Operations: `getPath`, `setPath`, `getNames`, `setNames`, `getAlias`, `setAlias`, `getType`, `setType`, `accept`, `toString`. |

#### `org.jmouse.el.template.node.sub`

| Type | Responsibility / contract |
|---|---|
| [ConditionBranch](../jmouse-el/src/main/java/org/jmouse/el/template/node/sub/ConditionBranch.java) | Implements/extends `AbstractNode`. Operations: `getWhen`, `getThen`, `toString`. |

#### `org.jmouse.el.template.parser`

| Type | Responsibility / contract |
|---|---|
| [RootParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/RootParser.java) | 🏗️ The sourceRoot parser responsible for processing the entire view. |
| [TemplateParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/TemplateParser.java) | 🏗️ The view parser that processes the entire view file. |

#### `org.jmouse.el.template.parser.tag`

| Type | Responsibility / contract |
|---|---|
| [ApplyParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/ApplyParser.java) | Implements/extends `TagParser`. Operations: `parse`, `getName`. |
| [BlockParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/BlockParser.java) | Parses the block tag, which defines a named, overridable section of a view. |
| [CacheParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/CacheParser.java) | Parses the cache tag, which captures the rendered output of its body under a computed cache key. |
| [DoParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/DoParser.java) | Implements/extends `TagParser`. Operations: `parse`, `getName`. |
| [EmbedParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/EmbedParser.java) | Parses the embed tag from a view. |
| [ExtendsParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/ExtendsParser.java) | Implements/extends `TagParser`. Operations: `parse`, `getName`. |
| [ForParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/ForParser.java) | Parses a "for" loop tag. |
| [IfParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/IfParser.java) | Parses an "if" tag with support for else-if and else branches. |
| [IncludeParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/IncludeParser.java) | Implements/extends `TagParser`. Operations: `parse`, `getName`. |
| [LoremParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/LoremParser.java) | Implements/extends `TagParser`. Operations: `parse`, `getName`. |
| [MacroParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/MacroParser.java) | Parses a macro definition in a view. |
| [ParentParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/ParentParser.java) | Implements/extends `TagParser`. Operations: `parse`, `getName`. |
| [RenderParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/RenderParser.java) | Implements/extends `TagParser`. Operations: `parse`, `getName`. |
| [ScopeParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/ScopeParser.java) | Implements/extends `TagParser`. Operations: `parse`, `getName`. |
| [SetParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/SetParser.java) | Implements/extends `TagParser`. Operations: `parse`, `getName`. |
| [URLParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/URLParser.java) | Implements/extends `TagParser`. Operations: `parse`, `getName`. |
| [UseParser](../jmouse-el/src/main/java/org/jmouse/el/template/parser/tag/UseParser.java) | Parses the use tag, which allows importing macros or blocks from another view into the current view's registry. |

#### `org.jmouse.el.translate`

| Type | Responsibility / contract |
|---|---|
| [Bindings](../jmouse-el/src/main/java/org/jmouse/el/translate/Bindings.java) | What a caller supplies by name — currentMember, a tenant, a date a screen picked. |
| [Capabilities](../jmouse-el/src/main/java/org/jmouse/el/translate/Capabilities.java) | What a backend can actually honour. |
| [Capability](../jmouse-el/src/main/java/org/jmouse/el/translate/Capability.java) | One thing a query may ask for that a backend may or may not be able to do. |
| [TranslationRefusedException](../jmouse-el/src/main/java/org/jmouse/el/translate/TranslationRefusedException.java) | A query a backend cannot honour. |
| [Translator](../jmouse-el/src/main/java/org/jmouse/el/translate/Translator.java) | One way out of the tree — into SQL for a vendor, into a pipeline over rows, or back into jMQ itself. |

#### `org.jmouse.el.type`

| Type | Responsibility / contract |
|---|---|
| [CarrierType](../jmouse-el/src/main/java/org/jmouse/el/type/CarrierType.java) | A type that is its carrier and nothing more: int is a Integer, string a String, any whatever is given. |
| [TypeAnnotations](../jmouse-el/src/main/java/org/jmouse/el/type/TypeAnnotations.java) | Reads the annotation a declaration may carry — : int after a name — and resolves it against the types the language registered, while the text is parsed: a misspelt type is refused at its line, never discovered at run time. |
| [TypeContainer](../jmouse-el/src/main/java/org/jmouse/el/type/TypeContainer.java) | The types a language knows, by the name a document writes. |
| [ValueType](../jmouse-el/src/main/java/org/jmouse/el/type/ValueType.java) | 🏷️ A type a declaration may be annotated with — let count : int = 1 —: a name a document writes, the Java class a value of it is carried as, and what a value bound to it must be. |
| [ValueTypeException](../jmouse-el/src/main/java/org/jmouse/el/type/ValueTypeException.java) | A value bound to a typed declaration that is not of its type and does not convert to it. |

### Packaged resources

See [packaged resources](../jmouse-el/src/main/resources/) for configuration/templates owned by this module.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [BeanAccessInvocationTest.java](../jmouse-el/src/test/java/org/jmouse/el/BeanAccessInvocationTest.java).
- [BeanAccessSourceTest.java](../jmouse-el/src/test/java/org/jmouse/el/BeanAccessSourceTest.java).
- [ConcurrentParsingTest.java](../jmouse-el/src/test/java/org/jmouse/el/ConcurrentParsingTest.java).
- [ExpressionSmokeTest.java](../jmouse-el/src/test/java/org/jmouse/el/ExpressionSmokeTest.java).
- [CursorTest.java](../jmouse-el/src/test/java/org/jmouse/el/lexer/CursorTest.java).
- [Smoke.java](../jmouse-el/src/main/java/org/jmouse/el/Smoke.java).
