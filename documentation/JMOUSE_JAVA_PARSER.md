# jmouse-java-parser

Structural Java parser: a `.java` text goes in; the package, imports, every type (nested ones included)
and every member come out — modifiers, annotations, type parameters, parameters, `throws`, the signature
as written, JavaDoc structured and linked to its owner, exact source spans and a stable symbol id on
each. Declaration mode keeps bodies as spans. Optional `EXPRESSIONS` mode reads executable bodies and field initializers into immutable expression, binding and reference records. Nothing is executed or
resolved: no class loading, no type lookup, no annotation processing, no inherited documentation copied.

It is built on the jME (`jmouse-el`) lexing and tree contracts — `Token`, `Token.Type`, `Tokenizer`,
`Lexer`, `TokenizableSource`, `TokenCursor`/`Cursor` with savepoints, `Node`/`AbstractNode`, `Trivia`,
`Visitor` — and adds the Java grammar, source ranges and an immutable record projection on top. No
Spring, persistence or product code. Depends on `jmouse-el` only (which brings `jmouse-core`).

Coordinates: `org.jmouse:jmouse-java-parser:1.0.1`. Tracked as `JMF-347`; first adopter Raccoon
(`COON-23`).

## Why a module of its own

`PROJECT.md` of Raccoon asks for a reusable Java parser whose grammar and dependencies stay out of
`jmouse-core`. The jME expression grammar does not parse Java declarations, so the Java grammar lives
here; what *is* shared — the token, cursor, source and node contracts — is taken from `jmouse-el`
rather than re-invented.

## Used contracts

- `JavaParser` — the facade, stateless and thread-safe.
  - `parse(sourceName, text) → JavaSourceFile` — the immutable record projection.
  - `parseTree(sourceName, text) → CompilationUnitNode` — the jME node tree.
  - `REVISION` — the algorithm revision (`4`), for consumers that freeze results.
- Lexing (`org.jmouse.java.parser.lexer`)
  - `JavaTokenKind implements Token.Type` — identifiers, keywords, literals, punctuation; operators are
    single characters on purpose so `>>` can close two levels of type arguments.
  - `JavaTokenizer implements Tokenizer<JavaSourceText, Token>` — produces jME `Token`s; handles Unicode
    escapes (translated first, JLS §3.3, with an offset map back to the original), line/block/doc
    comments, strings, text blocks, character literals. Never throws; malformed literals are problems.
  - `JavaLexer implements Lexer` — `tokenize(text) → TokenCursor` (a jME `Cursor`).
  - `JavaSourceText implements TokenizableSource` — the original text plus what tokenising recorded: one
    entry per token (entry *n* = ordinal *n*, carrying the token's **original** length — the token's end),
    comments, lexical problems, and a `LineIndex` for line/column lookups.
  - `JavaComment(kind, text, span)` — `LINE`, `BLOCK`, `DOCUMENTATION`.
- Tree (`org.jmouse.java.parser.node`) — `JavaNode extends AbstractNode` (sealed)
  - `CompilationUnitNode` (root; holds comments, JavaDocs, problems; `toSourceFile()`), `PackageNode`,
    `ModuleNode`, `ImportNode`, `TypeNode` (children: record components, then members), `MemberNode`
    (leaf: field variable, method, constructor, enum constant, initializer, record component).
  - Comments written before a declaration are its `getLeadingTrivia()`, exactly as written, JavaDoc
    included.
  - `JavaNodeVisitor extends Visitor` — typed `visit` per node; `JavaNode.accept` dispatches to it, and a
    plain jME `Visitor` still walks the tree through `visit(Node)`.
- Records (`org.jmouse.java.parser.model`) — a projection of the tree, not a second model:
  `TypeNode.declaration()` assembles its members from the children on each call.
  - `JavaSourceFile` → `PackageDeclaration`, `ModuleDeclaration`, `ImportDeclaration`, `TypeDeclaration`;
    `declarations()` flattens depth-first; `isComplete()` is `problems().isEmpty()`.
  - `Declaration` (sealed): `TypeDeclaration`, `FieldDeclaration`, `MethodDeclaration`,
    `ConstructorDeclaration`, `EnumConstantDeclaration`, `InitializerDeclaration`,
    `RecordComponentDeclaration`.
  - `JavaDoc(span, rawText, description, blockTags, inlineTags, owner)`; `BlockTag` (`param` with name,
    `return`, `throws`/`exception` with type, `see`, `since`, `deprecated`, `author`, `OTHER` kept
    generically); `InlineTag` (`link`, `linkplain`, `code`, `literal`, `inheritDoc`, `value`, `OTHER`);
    `ReferenceState` (`UNRESOLVED` for every reference and `{@inheritDoc}` — no text is ever invented).
  - `JavaDocOwner(symbolId, kind, qualifiedName, signature, span)` — the explicit link from comment to
    owner. The comment's span and the owner's span are separate.
- `JavaDocParser` (`org.jmouse.java.parser.javadoc`) — usable on its own for one comment.
- Positions (`org.jmouse.java.parser.source`): `SourcePosition(offset, line, column)`, `SourceSpan(start,
  end)`, `LineIndex`, `ParseProblem(kind, message, span)`, `ProblemKind`.

### Coordinate system

Offsets are zero-based UTF-16 code units of the text exactly as given (before escape translation), end
exclusive. Lines are one-based and end at `\n`, `\r` or `\r\n`. Columns are one-based UTF-16 code units;
a tab is one column. Every span ends after a non-terminator character, so `end.line` is the last line.

### Symbol ids

JavaDoc-reference style, unique within a file:

| Declaration | Id |
|---|---|
| package | `org.example` |
| type (nesting with dots) | `org.example.Outer.Inner` |
| field, enum constant, record component | `org.example.Type#name` |
| method | `org.example.Type#method(String,int[],T...)` |
| constructor / compact constructor | `org.example.Type#Type(String)` — compact uses the component types |
| initializer | `org.example.Type#<initializer>[1]`, `#<static-initializer>[1]` |

Parameter types are as written, never resolved: annotations, `final`, type arguments and whitespace
dropped; `[]` and `...` kept. A duplicate id in one file gets `~2`, `~3`. Default-package files have no
prefix.

### Signatures

The header text from the first annotation or modifier to just before the body / `;` / initializer /
`default` value, whitespace runs collapsed to one space. A field's signature is its statement header
followed by the variable name (one `FieldDeclaration` per declarator, sharing the statement span).

## Errors and recovery

Parsing never throws for malformed input. The loop over a file or a type body takes a cursor savepoint
before each declaration; a failure is reported as a `ParseProblem` with its position, the cursor is
restored, and the declaration is skipped through its `;` or the end of its first brace block. An
unclosed body keeps the declaration and reports `UNBALANCED_DELIMITER`; an unterminated string closes at
its line end so the damage stays inside one statement. Problems are sorted by position. Types nest at
most 64 deep.

## Declaration baseline verification (initial release)

`mvn -o -q install -pl jmouse-java-parser -am` — 38 JUnit tests, no network:

- every construct: package/imports (static, wildcard), all five type kinds, sealed/non-sealed/permits,
  nested types, multi-declarator fields, generic methods with bounds, varargs, receiver parameters,
  legacy array returns, constructors, compact constructors, initializers, enum constants with arguments
  and bodies, annotation elements with defaults, interface default/static/private methods, modules,
  exact CRLF spans;
- tricky lexing: text blocks with braces and quotes, escaped quotes, comments with delimiter
  look-alikes, `>>`/`>>>` in nested generics and expressions, Unicode escapes;
- JavaDoc: description, every block and inline tag, owner link, separate spans, orphans;
- recovery: broken members, a broken type header, unclosed bodies, broken enum constants, missing `;`,
  stray top-level tokens;
- the jME tree: typed and plain visitor traversal, leading trivia, projection equality;
- real world: every `.java` under `jmouse-core/src/main/java` — **561 files, 50 758 lines, 0 problems**,
  3 030 JavaDoc comments; 425 classes, 162 interfaces, 13 enums, 78 records, 12 annotation types,
  2 841 methods, 405 constructors, 6 compact constructors, 650 fields, 130 record components,
  338 enum constants, 2 static initializers; about 0.5 s.

## Not in this module

- Full Java semantic analysis, pattern bindings, target-type inference for lambdas, and local/anonymous class declarations.
- Type resolution, classpath, inherited JavaDoc. A semantic mode can mark references resolved later
  without the model changing shape (`ReferenceState`).
- Complete switch-expression grammar and generic method inference remain outside the body reader. Unsupported syntax is reported explicitly.

## jmouse-el gaps

What the jME contracts could not carry, how this module bridges it, and the smallest change in
`jmouse-el` that would close each gap. Apart from the three `Cursor` defects closed by JMF-349 (below),
nothing in `jmouse-el` or `jmouse-core` was changed for this module.

**Closed by JMF-349 (2026-10-05)** — fixed in `jmouse-el`'s `Cursor`, covered by `CursorTest`:

| Was | Now | Effect on callers |
|---|---|---|
| `Cursor.previous()` returned the current token and then decremented (`tokens.get(cursor--)`) | steps back, then returns the token now current — the mirror of `next()` | none: nothing in the reactor called it |
| `Cursor.lookAt(offset)` threw for a negative position (`lookAt(-1)` at the start) | clamped at both ends: before the start answers the first token (`T_SOL` in a lexed stream), past the end the last (`T_EOL`); `null` only for an empty stream | `isPrevious` at position 0 now answers instead of throwing; here `tokenAt` dropped its `Math.max(position, 0)` and `endOfPrevious` reads `lookAt(-1)` |
| `TokenCursor.expect(...)` tested the *next* token (`isNext`) but consumed the current one | tests the current token, the one it consumes — the same check as `ensure` | its one caller, the template `RootParser` fallback, meets it only at `T_EOL`, where both forms behaved the same; a stray template token is now reported as itself instead of skipped when `T_EOL` followed it |

**Still open:**

| Gap | Bridge here | Smallest closing change |
|---|---|---|
| `Token` has a start offset and line but no end offset and no column; with Unicode escapes the value length is not the original length | `JavaSourceText` records an entry per token (entry *n* = ordinal *n*) whose length is the original length; columns come from `LineIndex` | add `int length` (original length) to `Token`, or document that `TokenizableSource.Entry` *n* belongs to ordinal *n* |
| `SyntaxErrorException` needs a `TokenizableSource`, has a fixed message and no span; parsing that recovers needs a problem *value* | `ParseProblem` + internal `SyntaxFailure` | a span-carrying problem record in `jmouse-el` the exception wraps |
| `Lexer.tokenize` returns only a cursor — comments and lexical problems have no channel | they ride on the `TokenizableSource` (`JavaSourceText.comments()/problems()`) | a lexer result type, or a `comments()` hook on `TokenizableSource` |
| `TokenizableSource.entry()` mutates the source; tokenising twice duplicates entries | `JavaSourceText` refuses a second run | a `reset()` or a tokenizer that returns entries instead of writing them |
| `StringSource.getLineNumber` scans from the start on every call (O(n) per lookup) and reads `buffer[i + 1]` past the text on a trailing `\r` | `JavaSourceText` uses a line-start index | build line starts once (as `LineIndex` does) |
| `Node` is mutable — public `add`, `setParent`, a live `getChildren()` list | the parser hands over a finished tree; records are projected from it | an unmodifiable `getChildren()` view, or a `freeze()` |
| `Trivia(kind, text)` has no position, so a comment cannot be cited from the tree | the structured `JavaDoc` (with span and owner) lives on the declaration; `JavaSourceFile.comments()` keeps spans | add `int offset` (and length) to `Trivia` |
| `Visitor` lists the EL node types only; `Node.accept` dispatches statically to `visit(Node)` | `JavaNodeVisitor extends Visitor`; `JavaNode.accept` dispatches by type | none needed — `accept` is overridable; documenting the pattern would do |
| No source-range type anywhere: `SourceSpan(document, line, column)` exists twice (access-policy, script-el) and is a point | `SourcePosition` / `SourceSpan` / `LineIndex` here | promote a position + range + line index into `jmouse-core` text, as a confirmed shared contract |

⚠️ `-am` rebuilds and installs `jmouse-core` and `jmouse-el` from the working tree, including whatever
uncommitted changes they carry at that moment.


## Optional expressions (revision 3)

`parser.parse(name, text, JavaParser.Level.EXPRESSIONS)` returns the same declarations plus
`JavaSourceFile.bodies()`. The existing two-argument parse and parseTree remain declaration-only.
The old JavaSourceFile constructor is retained; it supplies an empty body list.

- `ExecutableBody`: owning declaration, enclosing type, source span, expression roots, local bindings,
  unresolved body references. Methods, constructors, initializer blocks and field initializers are read.
- `Expression`: kind, written name, exact name/full spans, receiver and arguments; includes calls,
  fields, object creation, method references, operators, conditionals, arrays, casts and lambdas.
- `LocalBinding`: name/type/name span/visibility span/initializer. A null type requests inference.
  Parameters, block locals, control-header locals and lambda parameters carry lexical visibility.
- `BodyReference`: expression, owner/type context, and role CALLS, READS, WRITES, READS_WRITES,
  INSTANTIATES or METHOD_REFERENCE. References remain unresolved in the parser.
- Recovery retains expressions before and after an incomplete statement; nesting is bounded.
  `UNSUPPORTED_SYNTAX` distinguishes unsupported constructs from syntax errors. Local/anonymous
  types are reported and skipped; this mode is a navigation reader, not a Java compiler.

Expression records are attached to the optional record result; declaration-tree visitors are unchanged.
No classpath, symbol resolution, I/O or product dependencies have been added to the parser.

## Phase-one hardening (2026-10-18)

Parser revision is now **4**. ParseProblem retains its three-argument constructor and adds
Phase.LEXICAL / STRUCTURE / BODY. JavaSourceFile.isStructureComplete() separates body recovery
from declaration damage. isDeclarationComplete(symbolId) and isBodyComplete(owner) expose a
shared conservative ParseValidity policy. isComplete() retains its original all-diagnostics meaning.

Primitive/void class literals are supported. Unsupported switch expressions are skipped as one
bounded construct with an explicit diagnostic, avoiding cascades through their arms. Their body
is incomplete; this does not invent references from skipped syntax. Local/anonymous classes,
full switch expressions, pattern-flow scopes and full generic inference remain unsupported.

CorpusReport in src/test is an explicit command-line tool accepting source roots; it is not a
machine-specific test or a precision benchmark. Persistent BodyValidityTest covers these policies.
