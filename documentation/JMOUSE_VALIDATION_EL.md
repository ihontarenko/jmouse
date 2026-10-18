# jmouse-validation-el

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-validation-el`. Build/dependency declaration: [pom.xml](../jmouse-validation-el/pom.xml).

## What it is

jMV is a declarative validation language that parses documents, compiles checks/guards/messages once and judges record maps repeatedly. It includes source/draft/writer tooling so validation can be authored and round-tripped without inventing another validation runtime.

Document syntax lives here; the underlying constraints and registry live in jmouse-validation. A document parser and the expression language for guards/messages are deliberately separate vocabularies. Persistence is provided separately by validation-jpa.

## The map

| Type | Responsibility |
| --- | --- |
| [JmvReader](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/JmvReader.java) | Parses source into a validation document with file/span context. |
| [JmvLoader](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/loader/JmvLoader.java) | Resolves locations, eagerly parses/compiles documents and returns name/origin lookup. |
| [ValidationDocumentNode](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/node/ValidationDocumentNode.java) | Root named validation document tree. |
| [JmvCompiler](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/translate/JmvCompiler.java) | Translates the document tree into CompiledValidation using constraint signatures and an expression language. |
| [CompiledValidation](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CompiledValidation.java) | Reusable compiled document; creates one RecordJudgement per validation run. |
| [RecordJudgement](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/RecordJudgement.java) | Per-record state and constraint evaluation context. |
| [CompiledLine](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CompiledLine.java) | One field line, including its local stop behavior. |
| [CheckSignatures](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CheckSignatures.java) | Maps named checks and argument shapes to executable constraints. |
| [ValidationOutcome](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/ValidationOutcome.java) | Validation result with collected validation errors. |
| [ValidationDraft](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/builder/ValidationDraft.java) | Builder representation for authored validation documents. |
| [JmvWriter](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/translate/JmvWriter.java) | Translates a document tree back to jMV source. |
| [ValidationTranslator](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/translate/ValidationTranslator.java) | Shared translation seam implemented by compiler and writer. |

## Execution and ownership

1. Resolve sources through JmvLoader or parse explicit text with JmvReader.
2. Compile the tree once with JmvCompiler using the intended expression vocabulary and constraint registry.
3. Call CompiledValidation.validate(recordMap) for each input; each run has its own judgement state.
4. Inspect ValidationOutcome and map its errors into the product response; use the writer/draft model when documents are edited or exported.

## Public API and behavior

JmvReader.parse(source, file) returns the AST without executing any validation.

JmvLoader.load returns Loaded containing compiled documents and source origins; get(name) returns Optional.

The declared validation name is the document identity, independent of the source filename.

Validation collects errors by default. stop ends a field line; gate can end the document before its main body. These are not additional ValidationPolicy enum values.

## Wiring and extension points

- Provide JmvSources for another source backend and configure CheckSignatures/ConstraintTypeRegistry for another check vocabulary.
- Use a separate ExpressionLanguage to widen guard/message expressions without widening document keywords.
- Use translator capabilities to reject unsupported transformations explicitly instead of dropping checks or source semantics.

## Pitfalls

- Load/compile before records arrive. Parsing per submission repeats expensive work and moves authored-document errors to the wrong user.
- Two sources claiming one validation name are refused with both origins; classpath ordering must not decide which validation wins.
- CompiledValidation is reusable; mutable evaluation state lives in RecordJudgement.
- Do not compile expression slices with the document recognizer: field names containing document keywords must remain valid expression names.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-validation-el/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Existing executable examples/smokes: [BranchingSmoke](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/BranchingSmoke.java), [BuilderSmoke](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/BuilderSmoke.java), [CollectAllSmoke](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/CollectAllSmoke.java), [JmvSmoke](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/JmvSmoke.java), [RoundTripSmoke](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/RoundTripSmoke.java).

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-validation-el/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-validation` | compile | no |
| `org.jmouse:jmouse-el` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.validator.el`

| Type | Responsibility / contract |
|---|---|
| [JmvExtension](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/JmvExtension.java) | What the .jmv language adds to the expression engine. |
| [JmvReader](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/JmvReader.java) | Reads .jmv source into a document. 📥 |

#### `org.jmouse.validator.el.builder`

| Type | Responsibility / contract |
|---|---|
| [UnshowableValidationException](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/builder/UnshowableValidationException.java) | A document the form cannot show, said in a way a screen can act on. 🚫 |
| [ValidationDraft](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/builder/ValidationDraft.java) | A validation document as a form fills it in — rows, not text. 🧾 |
| [ValidationDrafts](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/builder/ValidationDrafts.java) | Rows into a document, and a document back into rows. 🔁 |

#### `org.jmouse.validator.el.lexer`

| Type | Responsibility / contract |
|---|---|
| [JmvRecognizer](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/lexer/JmvRecognizer.java) | Recognizes the three vocabularies a .jmv file is written in. |
| [JmvToken](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/lexer/JmvToken.java) | The words a .jmv validation file adds on top of the expression language. |

#### `org.jmouse.validator.el.loader`

| Type | Responsibility / contract |
|---|---|
| [ClasspathJmvSources](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/loader/ClasspathJmvSources.java) | .jmv documents found on the classpath. 📚 |
| [JmvLoader](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/loader/JmvLoader.java) | Reads every .jmv a set of locations names, compiles each one, and hands back a lookup. 📚 |
| [JmvLoadException](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/loader/JmvLoadException.java) | A set of .jmv documents that cannot be loaded, said in a way somebody can act on. 🚫 |
| [JmvSource](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/loader/JmvSource.java) | One .jmv document, and where it came from. 📄 |
| [JmvSources](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/loader/JmvSources.java) | Where .jmv documents come from — the one thing JmvLoader does not know how to do. 📁 |

#### `org.jmouse.validator.el.node`

| Type | Responsibility / contract |
|---|---|
| [CheckBlockNode](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/node/CheckBlockNode.java) | A gate { … } or an always { … }. |
| [CheckLineNode](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/node/CheckLineNode.java) | Everything asked of one field on one line — part_number : required stop, size(3, 32). |
| [CheckNode](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/node/CheckNode.java) | One check on one field — size(3, 32), required stop, url(host: 'mouser.com'). |
| [InvariantNode](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/node/InvariantNode.java) | An assertion about the record rather than about one field — invariant min_stock_threshold <= quantity : 'Above stock'. |
| [ValidationDocumentNode](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/node/ValidationDocumentNode.java) | One .jmv file: validation "innoventa/part" { … }. |
| [WhenBranchNode](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/node/WhenBranchNode.java) | One branch of a when — the guarded one, or the otherwise. |
| [WhenNode](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/node/WhenNode.java) | Checks that apply only while a condition holds — when mount_type == 'SMD' { … otherwise { … }}. |

#### `org.jmouse.validator.el.parser`

| Type | Responsibility / contract |
|---|---|
| [AlwaysParser](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/AlwaysParser.java) | Reads always { … } — the unguarded block. |
| [CheckLineParser](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/CheckLineParser.java) | Reads field : check[, check]… and any continuation line carrying the line's message. |
| [CursorMatcher](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/CursorMatcher.java) | The one question every jMV parser's supports has to ask, asked in one place. |
| [ExpressionSlice](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/ExpressionSlice.java) | Cuts an expression out of the source exactly as it was typed , without parsing it. ✂️ |
| [GateParser](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/GateParser.java) | Reads gate { … } — the block whose failure answers for the whole document. |
| [InvariantParser](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/InvariantParser.java) | Reads invariant : . |
| [JmvBlockParser](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/JmvBlockParser.java) | Base for every { … } block in the validation language. |
| [JmvParserPriority](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/JmvParserPriority.java) | The order the validation parsers are offered a statement in, and why that order is what it is. |
| [JmvSyntaxException](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/JmvSyntaxException.java) | A .jmv file that cannot be read, said in a way the person who wrote it can act on. 📍 |
| [ValidationDocumentParser](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/ValidationDocumentParser.java) | Reads a whole .jmv file — the trivia around the block as well as the block. |
| [ValidationParser](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/ValidationParser.java) | Reads the validation "name" { … } block — the whole of a file, once its trivia is past. |
| [WhenParser](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/parser/WhenParser.java) | Reads when { … [otherwise { … }]}. |

#### `org.jmouse.validator.el.runtime`

| Type | Responsibility / contract |
|---|---|
| [CheckSignature](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CheckSignature.java) | What one check written in a .jmv means in terms of a constraint. 🧾 |
| [CheckSignatures](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CheckSignatures.java) | Every check a .jmv may write, and what each one means. 📚 |
| [CompiledCheck](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CompiledCheck.java) | One check, ready to be applied to a value. ✅ |
| [CompiledGuard](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CompiledGuard.java) | A when … { … otherwise { … }}, compiled. |
| [CompiledInvariant](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CompiledInvariant.java) | An assertion about the record — invariant min_stock_threshold <= quantity : '…'. |
| [CompiledItem](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CompiledItem.java) | Something a compiled document evaluates against a record. |
| [CompiledLine](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CompiledLine.java) | Everything asked of one field, compiled. 📏 |
| [CompiledValidation](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/CompiledValidation.java) | A .jmv document, compiled once and applied to record after record. 🗂️ |
| [RecordJudgement](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/RecordJudgement.java) | One record being judged: what it holds, what has been said about it, and how to ask an expression. ⚖️ |
| [ValidationError](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/ValidationError.java) | Value record: `(String field, String message)`. |
| [ValidationOutcome](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/runtime/ValidationOutcome.java) | What a document had to say about one record. 📋 |

#### `org.jmouse.validator.el.translate`

| Type | Responsibility / contract |
|---|---|
| [JmvCapability](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/translate/JmvCapability.java) | What a destination for a .jmv tree may or may not be able to honour. |
| [JmvCompiler](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/translate/JmvCompiler.java) | Turns a parsed .jmv into something that judges records. ⚙️ |
| [JmvWriter](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/translate/JmvWriter.java) | Renders a validation tree back into .jmv source. ✍️ |
| [ValidationTranslator](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/translate/ValidationTranslator.java) | A Translator that knows it is translating a validation document. 🧭 |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [BranchingSmoke.java](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/BranchingSmoke.java).
- [BuilderSmoke.java](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/BuilderSmoke.java).
- [CollectAllSmoke.java](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/CollectAllSmoke.java).
- [JmvSmoke.java](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/JmvSmoke.java).
- [RoundTripSmoke.java](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/RoundTripSmoke.java).
- [ShapeSmoke.java](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/ShapeSmoke.java).
- [SmokeReport.java](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/SmokeReport.java).
- [StopSmoke.java](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/StopSmoke.java).
- [TriviaSmoke.java](../jmouse-validation-el/src/main/java/org/jmouse/validator/el/smoke/TriviaSmoke.java).
