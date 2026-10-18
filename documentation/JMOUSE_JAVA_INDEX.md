# jmouse-java-index

Reusable source-code index and resolvers, without I/O, Spring or a global materialized graph.
Depends on `jmouse-java-parser`. Coordinates: `org.jmouse:jmouse-java-index:1.0.1`.

## File contributions

`FileIndex.of(JavaSourceFile)` flattens declarations, type references and optional executable bodies.
`FileIndex.REVISION` is `3`; parser revision is separately part of any persistent cache key.
The original six-argument constructor remains available, with empty member/body lists.

- `declarations`, `references`, `imports`, `problems`: declaration-level data.
- `members`: `MemberSymbol` records with source, symbol/container ids, name, kind, written return/field
  type, parameters, modifiers and declaration-name span.
- `bodies`: optional parser bodies, including expressions, bindings and unresolved references.
- `memberNamedAt(offset)` and `bodyReferenceAt(offset)`: caret lookup with exclusive end offsets.

Use `JavaParser.Level.EXPRESSIONS` before indexing to obtain body references. Declaration-only parsing
remains supported for existing consumers.

## Storage seam and resolution

`SymbolTable.findTypes(qualifiedName)` is the existing type lookup seam.
The source-compatible default methods `findMembers(type)`, `supertypes(type)` and
`declaringFile(type)` supply the additional information required for member resolution.
An adapter that wants member resolution implements all three; the defaults describe no known members.

`TypeResolver.resolve(file, IndexedReference)` retains its existing contract and explicit
Resolved / Ambiguous / TypeVariable / Unresolved results.

`MemberResolver.resolve(file, BodyReference)` returns the sealed `MemberResolution`:

- `Resolved(MemberSymbol)`: definite declaration;
- `Ambiguous(candidates)`: multiple possible declarations;
- `Local(LocalBinding)`: a lexical binding rather than a member;
- `Unresolved(name, reason)`: missing symbols, unsupported inference or no applicable declaration.

Member lookup uses the receiver's type, lexical shadowing, enclosing types, inherited members,
visibility and static imports. Receiver inference follows explicit parameter/local/field types, `var`
initializers, `new`, casts, array elements and resolved method return types. Overloads use argument
types, primitive widening, basic boxing/unboxing, reference subtyping and varargs phases.
Unknown argument or receiver types do not become guessed declarations. Ambiguous references are
not counted as definite usages. Method references without a target functional type can resolve only
when lookup leaves a single declaration.

## In-memory project index

`CodeIndex` implements SymbolTable. `put` replaces a file's contribution, `remove` forgets it.
It keeps per-file data and name-to-candidate-file lookups rather than a global graph.
Reads are concurrent; writes exclude reads.

`FileSymbols.of(file)` extracts lightweight type/name/candidate metadata. The optional
`CodeIndex(List<FileSymbols>, Function<String, FileIndex>)` constructor restores that catalog without
loading full declarations or bodies. `fileSymbols(source)` reads metadata only; `file(source)` calls
the loader on first use and caches the immutable contribution. Replacement/removal invalidates both
the loaded contribution and its catalog entry, including files that were never loaded.

- Existing type operations: `searchTypes`, `type`, `resolve`, `usages(TypeSymbol)`,
  `directSubtypes`, `allSubtypes`.
- `resolveMember(source, reference)` resolves a body reference on demand.
- `usages(MemberSymbol)` returns definite `MemberUsage(source, reference)` results in source order,
  first limiting the search to files that write the member name.

## Limits

This is source navigation, not a compiler. Generic method/type-argument substitution, complete
functional target typing, local/anonymous class scopes and pattern-flow scopes are not implemented. Missing classpath knowledge remains unresolved or ambiguous.
The parser reports unsupported body syntax explicitly. External declarations can be supplied lazily through ExternalSymbols.
Persistence lives in `jmouse-java-index-store`: a storage contract and a standard disk adapter, with
caller-owned lifecycle. Index filenames/extensions use complete words, for example `index-<uuid>.segment`.

## Binary declarations and completion (A6/A7)

- ClassFileIndexer.index(source, bytes) reads declaration-only class metadata through the BOM-managed ASM,
  without loading or executing classes. It returns FileIndex and a generated read-only Java API view with real name spans.
- ExternalSymbols accepts a caller-owned lookup from canonical class name to FileIndex. CodeIndex.externalSymbols
  installs this classpath; project source declarations take precedence. SymbolTable.file has a compatible empty default.
- Generic signatures, nested classes, fields, methods, constructors, record components, enum constants and parent types
  are represented. Bounds substitution, full target typing and decompiled method bodies remain outside this implementation.
- JRT lookup, local Maven resolution, archive selection, binary cache lifecycle and API-document routing belong to Atomic.
  The library performs no filesystem access, class loading or product lifecycle management.
- MemberResolver.complete(file, context) returns visible receiver members using the same access, static and inheritance
  rules as resolution. Atomic recovers a completion context with a temporary parse, then adds visible locals/project types.

## Phase-one hardening (2026-10-18)

FileIndex revision is **3**; its former six/eight-argument constructors remain source-compatible.
The javaDocs component retains parser-owned structured comments, references, owners and spans.
SourceReferences.of(file) projects typed HEADER / IMPORT / DOCUMENTATION / BODY occurrences,
including READS_WRITES and METHOD_REFERENCE, without claiming semantic resolution. Documentation
spans address complete source tags. Import on-demand spelling remains explicit. Local bindings
and expression context remain in the complete FileIndex payload.

Wildcard lookup deduplicates a TypeSymbol including its source, not merely its SymbolId. Equal
canonical names in separate sources remain ambiguous. FileIndex.withSource rebases member sources
without dropping documentation. FileIndex delegates validity checks to parser ParseValidity.
MemberResolver.resolve returns Unresolved for a body with recovery diagnostics; callers can still
display the retained syntactic occurrences with reliable=false. Declaration-level navigation remains
available independently. A valid body in the same file is unaffected by another body's diagnostic.

JavaIndexContractTest is the persistent regression suite for ambiguity, imports, overloads,
inheritance, lexical shadowing, partial bodies, documentation, type variables, method references
and JDK binary declarations. Database persistence lives in jmouse-java-index-jpa; source roots,
classpath composition and access policy remain caller concerns.
