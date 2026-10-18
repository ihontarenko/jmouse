# jmouse-spice

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-spice`. Build/dependency declaration: [pom.xml](../jmouse-spice/pom.xml).

## What it is

A structural SPICE netlist reader, typed syntax tree and printer built with jME token/parser primitives. It preserves generic cards it does not model explicitly, so consumers can inspect and rewrite a deck without discarding unfamiliar directives.

Parsing validates structure and reads card/value forms. This module does not solve circuits. Simulation/semantic analysis belongs to consumers such as parallax, which translate this syntax into their own circuit model.

## The map

| Type | Responsibility |
| --- | --- |
| [SpiceParser](../jmouse-spice/src/main/java/org/jmouse/spice/parser/SpiceParser.java) | Reads named or unnamed source into a SpiceDeck and configures card parsers. |
| [SpiceLexer](../jmouse-spice/src/main/java/org/jmouse/spice/lexer/SpiceLexer.java) | Tokenizes SPICE line/card syntax while retaining positions. |
| [SpiceTokenKind](../jmouse-spice/src/main/java/org/jmouse/spice/lexer/SpiceTokenKind.java) | Token vocabulary and case-insensitive word helpers. |
| [SpiceDeck](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceDeck.java) | Root deck with title and statement sequence. |
| [SpiceElement](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceElement.java) | Typed element/card representation. |
| [SpiceStatement](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceStatement.java) | Statement representation for directives and grouped constructs. |
| [SpiceValue](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceValue.java) | Value representation retaining authored syntax where needed. |
| [SpiceWaveform](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceWaveform.java) | Source-waveform representation. |
| [SpiceVisitor](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceVisitor.java) | Traversal seam for deck/card inspection. |
| [SpicePrinter](../jmouse-spice/src/main/java/org/jmouse/spice/printer/SpicePrinter.java) | Writes a deck/tree back to SPICE source. |
| [SpiceNumbers](../jmouse-spice/src/main/java/org/jmouse/spice/SpiceNumbers.java) | Reads engineering suffixes and writes finite values with stable significant digits. |
| [SpiceParseException](../jmouse-spice/src/main/java/org/jmouse/spice/SpiceParseException.java) | Structural parsing refusal with source position. |

## Execution and ownership

1. Create SpiceParser and call parse(text) or parse(name, text).
2. The parser tokenizes, scans model declarations and installs card parsers before reading the statement sequence.
3. Inspect or transform typed nodes; generic cards remain represented when no specialized node is available.
4. Print the resulting deck with SpicePrinter for source export; perform electrical semantic validation in the consuming module.

## Public API and behavior

parse(name, text) uses the supplied name in error reporting; unnamed text is identified as <netlist>.

Model names are gathered before card parsing because a bipolar transistor card can refer to a model declared later.

SpiceNumbers.read returns Optional<Engineering>; value is normalized to SI while suffix retains the authored letters.

SpiceNumbers.write rejects non-finite values and formats finite numbers to fifteen significant digits.

## Wiring and extension points

- Use SpiceVisitor to derive analysis or translation without changing parsing for every consumer.
- Add specialized CardParser/element parser support when a card needs structured semantics; retain generic fallback for other cards.
- Build simulation mappings outside the structural reader so syntax preservation does not depend on simulator capabilities.

## Pitfalls

- SPICE M means milli, independent of case; mega is MEG. A bare 1M is not one million.
- A suffix may contain ignored unit letters, but 1k5 is not the European 1.5 k notation here and is not accepted as a number.
- Successful parsing establishes structural validity, not that a simulator supports every model or directive.
- Missing required element values, unclosed parentheses and unmatched subcircuit endings are structural errors with line/column information.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-spice/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

Test sources are available under [src/test](../jmouse-spice/src/test). Inspect their setup before running integration-dependent checks.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-spice/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-el` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.spice`

| Type | Responsibility / contract |
|---|---|
| [SpiceNumbers](../jmouse-spice/src/main/java/org/jmouse/spice/SpiceNumbers.java) | Numbers as SPICE writes them: a decimal, then an optional scale factor, then any letters SPICE ignores. |
| [SpiceParseException](../jmouse-spice/src/main/java/org/jmouse/spice/SpiceParseException.java) | A SPICE text that cannot be read: where, and what is wrong there. |
| [SpicePosition](../jmouse-spice/src/main/java/org/jmouse/spice/SpicePosition.java) | Where something stands in a SPICE text: its 1-based line and column, as an editor counts them. |

#### `org.jmouse.spice.lexer`

| Type | Responsibility / contract |
|---|---|
| [SpiceLexer](../jmouse-spice/src/main/java/org/jmouse/spice/lexer/SpiceLexer.java) | The jME Lexer for SPICE: a text in, a TokenCursor over its tokens out. A text that is not a TokenizableSource already is wrapped as one under the name . Stateless and safe to share. |
| [SpiceSplitter](../jmouse-spice/src/main/java/org/jmouse/spice/lexer/SpiceSplitter.java) | Splits a SPICE text into jME RawTokens: the lexical rules of SPICE, settled once, so that nothing after the splitter knows what a line is. |
| [SpiceTokenizer](../jmouse-spice/src/main/java/org/jmouse/spice/lexer/SpiceTokenizer.java) | The jME Tokenizer for SPICE: the SpiceSplitter's raw tokens as SpiceTokenKinds, each recorded on the source as its entry, and one SpiceTokenKind#END_OF_TEXT after them. |
| [SpiceTokenKind](../jmouse-spice/src/main/java/org/jmouse/spice/lexer/SpiceTokenKind.java) | The jME Token.Type token types of SPICE. |

#### `org.jmouse.spice.node`

| Type | Responsibility / contract |
|---|---|
| [SpiceArgument](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceArgument.java) | One argument of a card as written, for what a node keeps without a field of its own: an element's area, an AC specification, a controlled source's control, the words of a dot card the reader has no node for. |
| [SpiceCard](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceCard.java) | A dot card: .model, .subckt, .param, an analysis, .end and the rest. |
| [SpiceDeck](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceDeck.java) | A whole SPICE text: its title, then its statements — the deck's children — in order, up to .end. |
| [SpiceElement](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceElement.java) | An element line: its name first, the name's letter saying what it is. Nodes are written as SPICE writes them, in the case they were written; SPICE compares them without regard to case. |
| [SpiceExpression](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceExpression.java) | An expression as SPICE writes one — between braces, or between single quotes as some SPICEs allow — kept as its text. SPICE's expression dialects differ from one simulator to the next; reading one is the caller's, in its own expression language. |
| [SpiceNode](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceNode.java) | A node of a SPICE syntax tree: a jME org.jmouse.el.node.Node, so a deck is walked, its comments carried as org.jmouse.el.node.Trivia and its children reached the way any jME tree is — and, for SPICE's own nodes, a SpiceVisitor that sees each kind by its type. |
| [SpiceParameter](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceParameter.java) | name=value: a .param, a model's parameter, a subcircuit's default or an instance's value. |
| [SpiceStatement](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceStatement.java) | One statement of a deck: an element line, or a dot card. |
| [SpiceValue](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceValue.java) | A value as a SPICE card writes it: a number with its scale factor, a parameter's name, or an expression. |
| [SpiceVisitor](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceVisitor.java) | Sees each kind of SpiceNode by its type: node.accept(visitor) calls the method for the node's kind. A method for every kind, none defaulted, so a visitor that compiles has decided what each kind means to it. |
| [SpiceWaveform](../jmouse-spice/src/main/java/org/jmouse/spice/node/SpiceWaveform.java) | A source's transient function: SIN(VO VA FREQ TD THETA PHASE), PULSE(V1 V2 TD TR TF PW PER), PWL(…), EXP(…), SFFM(…), AM(…) — its values in the order SPICE gives them, as many as were written. |

#### `org.jmouse.spice.parser`

| Type | Responsibility / contract |
|---|---|
| [SpiceParser](../jmouse-spice/src/main/java/org/jmouse/spice/parser/SpiceParser.java) | Reads a SPICE text into its SpiceDeck: the SpiceLexer cuts it into tokens, and a jME ParserContext holding one parser per kind of card reads them, each card's parser chosen by the card's first word. |

#### `org.jmouse.spice.printer`

| Type | Responsibility / contract |
|---|---|
| [SpicePrinter](../jmouse-spice/src/main/java/org/jmouse/spice/printer/SpicePrinter.java) | Writes a SPICE syntax tree as text — the one way SPICE is written, whether the tree was read or built. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [SpiceParserTest.java](../jmouse-spice/src/test/java/org/jmouse/spice/SpiceParserTest.java).
