# jmouse-mapper-el

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-mapper-el`. Build/dependency declaration: [pom.xml](../jmouse-mapper-el/pom.xml).

## What it is

jMM reads mapping documents and turns them into the TypeMappingRule objects consumed by jmouse-mapper. It adds authoring, loading, validation, builders and translators around the existing engine; it does not implement a second recursive mapper.

Document parsing owns declarations and source spans. JmmBinder compiles sliced value expressions with a plain ExpressionLanguage and binds them into rules. Mapping execution remains in jmouse-mapper.

## The map

| Type | Responsibility |
| --- | --- |
| [JmmReader](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/JmmReader.java) | Parses and validates a source document, then binds it into a rule source. |
| [JmmBinder](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/JmmBinder.java) | Converts document nodes into type/property mapping rules with expressions compiled at bind time. |
| [JmmValidator](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/JmmValidator.java) | Checks target property names, source paths and fragment use before mapping starts. |
| [JmmRuleSource](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/JmmRuleSource.java) | Rule-source adapter consumed by the mapper registry. |
| [JmmLoader](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/loader/JmmLoader.java) | Eagerly resolves multiple source locations into one validated source of rules. |
| [JmmSources](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/loader/JmmSources.java) | Pluggable source-location lookup. |
| [ClasspathJmmSources](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/loader/ClasspathJmmSources.java) | Resolves mapping source documents from classpath locations. |
| [MappingDocumentNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/MappingDocumentNode.java) | Root document tree containing declared targets, rule blocks and fragments. |
| [SourcePath](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/SourcePath.java) | Distinguishes a simple source path from a bound name or expression. |
| [BlockBindings](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/BlockBindings.java) | Compiled local let bindings used while evaluating a block. |
| [MappingDraft](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/builder/MappingDraft.java) | Builder representation for authoring mapping documents. |
| [JmmSourceTranslator](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/translate/JmmSourceTranslator.java) | Translates the document tree back to textual source. |

## Execution and ownership

1. Resolve all requested locations during application wiring through JmmLoader or provide source text to JmmReader.
2. Parse into MappingDocumentNode with a document identifier so syntax/validation errors retain their origin.
3. Validate named source/target types and property rules, resolve fragments, and bind expressions once.
4. Install the resulting JmmRuleSource in the mapper rule registry; ordinary mapper calls no longer read or parse documents.

## Public API and behavior

JmmReader.parse(source, file) exposes the AST without binding. read overloads return or populate JmmRuleSource.

JmmLoader.load(List<String>) refuses two files claiming the same target; rules for a target must have one owning document.

JmmBinder accepts an ExpressionLanguage, ClassLoader and optional Conversion so class resolution and named conversion use explicit collaborators.

The ViaFunction bridges expression execution to a named converter; the binder registers it on its expression language.

## Wiring and extension points

- Implement JmmSources for a location system other than classpath resources.
- Use MappingDraft and translators for editing/export; retain source spans and refuse unsupported representations rather than silently dropping semantics.
- Customize the expression-language extensions independently of the jMM document lexer.

## Pitfalls

- Never put reading behind a rule cache miss on the mapping path: invalid files should fail loading, not a production object mapping.
- A bare bound let name is not necessarily a source property; SourcePath and block bindings determine the interpretation.
- JmmValidator deliberately does not claim to prove arbitrary expression property access valid. It checks shapes where a typo can be determined without guessing.
- Do not compile value slices with JmmRecognizer; words reserved in a document remain ordinary identifiers inside expressions.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-mapper-el/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Existing executable examples/smokes: [ConvertedPairProbe](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/ConvertedPairProbe.java), [JmmSmoke](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/JmmSmoke.java), [JmmTranslatorSmoke](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/JmmTranslatorSmoke.java), [LoadedMappingProbe](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/LoadedMappingProbe.java), [MappingBuilderProbe](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/MappingBuilderProbe.java).

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-mapper-el/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-mapper` | compile | no |
| `org.jmouse:jmouse-el` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.mapper.el`

| Type | Responsibility / contract |
|---|---|
| [JmmBinder](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/JmmBinder.java) | Turns a parsed document into the rules the mapping engine already understands. 🔗 |
| [JmmExtension](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/JmmExtension.java) | What the .jmm language adds to the expression engine. |
| [JmmReader](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/JmmReader.java) | Reads .jmm source into rules the mapping engine can use. 📥 |
| [JmmRuleSource](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/JmmRuleSource.java) | What a mapper asks when it wants to know whether a .jmm file has anything to say about a type pair. 📄 |
| [JmmValidator](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/JmmValidator.java) | Checks a document against the types it names, before a single object is mapped. 🔍 |
| [SourcePath](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/SourcePath.java) | Whether a rule's right-hand side is a bare path read straight off the source. 🛣️ |
| [ViaFunction](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/ViaFunction.java) | via("name") standing alone — the source, through the converter registered under that name. |

#### `org.jmouse.mapper.el.builder`

| Type | Responsibility / contract |
|---|---|
| [MappableTypes](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/builder/MappableTypes.java) | What a builder offers in its two selects, and what a chosen pair is made of. 📇 |
| [MappingDraft](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/builder/MappingDraft.java) | A mapping as a form fills it in — rows, not text. 🧾 |
| [MappingDrafts](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/builder/MappingDrafts.java) | Rows into a document, and a document back into rows. 🔁 |
| [UnshowableMappingException](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/builder/UnshowableMappingException.java) | A document holds something the form has no row for. 🚧 |

#### `org.jmouse.mapper.el.lexer`

| Type | Responsibility / contract |
|---|---|
| [JmmRecognizer](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/lexer/JmmRecognizer.java) | Recognizes the three vocabularies a .jmm file is written in. |
| [JmmToken](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/lexer/JmmToken.java) | The words a .jmm mapping file adds on top of the expression language. |

#### `org.jmouse.mapper.el.loader`

| Type | Responsibility / contract |
|---|---|
| [ClasspathJmmSources](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/loader/ClasspathJmmSources.java) | .jmm documents found on the classpath. 📚 |
| [JmmLoader](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/loader/JmmLoader.java) | Every .jmm document a set of locations names, read into one rule source. 📚 |
| [JmmSource](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/loader/JmmSource.java) | One .jmm document, and where it came from. 📄 |
| [JmmSources](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/loader/JmmSources.java) | Where .jmm documents come from — the one thing JmmLoader does not know how to do. 📁 |

#### `org.jmouse.mapper.el.node`

| Type | Responsibility / contract |
|---|---|
| [AssertionNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/AssertionNode.java) | One line of a refuse block: condition : "message". |
| [FragmentNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/FragmentNode.java) | A fragment name { … } — rules shared across targets, declared at file level. |
| [FromNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/FromNode.java) | The rules for one source of the enclosing target: from OrderRequest { … }. |
| [IncludeNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/IncludeNode.java) | One include name line — a block pulling in a file-level fragment. |
| [LetNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/LetNode.java) | A named sub-expression: let full = ucfirst(a) ~ " " ~ ucfirst(b). |
| [MappingDocumentNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/MappingDocumentNode.java) | One .jmm file: mapping "name" { … }. |
| [RefuseNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/RefuseNode.java) | A refuse source&#124;target before&#124;after { … } block. |
| [RuleBlockNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/RuleBlockNode.java) | A run of rules, the bindings they may use, and the fragments they pull in. |
| [RuleNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/RuleNode.java) | One rule: targetProperty : expression [when condition], or targetProperty : ignore. |
| [TargetNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/TargetNode.java) | Everything about building one type: target Order { … }. |
| [UnmappedNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/UnmappedNode.java) | An unmapped fail or unmapped ignore line. |
| [UseNode](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/node/UseNode.java) | One import: use shop.api.OrderRequest. |

#### `org.jmouse.mapper.el.parser`

| Type | Responsibility / contract |
|---|---|
| [AlwaysParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/AlwaysParser.java) | Reads always { … } — the rules that hold whatever the source is. |
| [FragmentParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/FragmentParser.java) | Reads fragment name { … } — rules shared across targets, declared at file level. |
| [FromParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/FromParser.java) | Reads from Source { … } or from Source : expression. |
| [IncludeParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/IncludeParser.java) | Reads include fragmentName — pulls a fragment's rules into the block it is written in. |
| [JmmBlockParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/JmmBlockParser.java) | Base for every { … } block in the mapping language. |
| [JmmParserPriority](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/JmmParserPriority.java) | The order the mapping parsers are offered a statement in. |
| [JmmSpans](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/JmmSpans.java) | Where a node came from, taken the one way. 📍 |
| [JmmSyntaxException](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/JmmSyntaxException.java) | A .jmm file that cannot be read, said in a way the person who wrote it can act on. 📍 |
| [LetParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/LetParser.java) | Reads let name = expression — a sub-expression named for reuse within one block. |
| [MappingDocumentParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/MappingDocumentParser.java) | Reads a whole .jmm file — the trivia around the block as well as the block. |
| [MappingParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/MappingParser.java) | Reads the mapping "name" { … } block — the whole of a file, once its trivia is past. |
| [RefuseParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/RefuseParser.java) | Reads refuse source&#124;target before&#124;after { condition : "message" … }. |
| [RuleBlockParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/RuleBlockParser.java) | Reads the body of a rule block — the inside of always, from and fragment. |
| [RuleBlocks](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/RuleBlocks.java) | Sorting a parsed block's statements into the three things a rule block holds. 🗃️ |
| [RuleParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/RuleParser.java) | Reads property : value [when condition] or property : ignore. |
| [RuleValueReader](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/RuleValueReader.java) | Reads what stands to the right of a rule's : — and returns it exactly as it was typed , without parsing it. 📐 |
| [Separators](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/Separators.java) | Skips what separates one construction from the next. |
| [TargetParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/TargetParser.java) | Reads target Type { … } — everything about building one type. |
| [TypeNames](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/TypeNames.java) | Reads a Java type name as the file writes it. |
| [UnmappedParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/UnmappedParser.java) | Reads unmapped fail or unmapped ignore. |
| [UseParser](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/parser/UseParser.java) | Reads use fully.qualified.Type — brings a type into the file under its simple name. |

#### `org.jmouse.mapper.el.translate`

| Type | Responsibility / contract |
|---|---|
| [JmmCapability](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/translate/JmmCapability.java) | What a .jmm destination may declare it can honour — one capability per construct the language has. |
| [JmmJsonTranslator](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/translate/JmmJsonTranslator.java) | A mapping document as JSON — the shape, for something that is not the mapper. |
| [JmmSourceTranslator](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/translate/JmmSourceTranslator.java) | A mapping document written back out as .jmm — and therefore the language's formatter. |

### Packaged resources

See [packaged resources](../jmouse-mapper-el/src/main/resources/) for configuration/templates owned by this module.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [ConvertedPairProbe.java](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/ConvertedPairProbe.java).
- [JmmSmoke.java](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/JmmSmoke.java).
- [JmmTranslatorSmoke.java](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/JmmTranslatorSmoke.java).
- [LoadedMappingProbe.java](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/LoadedMappingProbe.java).
- [MappingBuilderProbe.java](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/MappingBuilderProbe.java).
- [SourceReadProbe.java](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/SourceReadProbe.java).
- [ViaFilterProbe.java](../jmouse-mapper-el/src/main/java/org/jmouse/mapper/el/examples/ViaFilterProbe.java).
