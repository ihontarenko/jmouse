package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceNotation.Normalized;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ⚠️ <strong>The reference the engine-based {@link DocumentReader} is measured against, and nothing else</strong>: it
 * is how jMX was read by hand, kept only until the equivalence test has a recorded snapshot to compare with instead.
 *
 * The structure of a jMX text: documents, their heads, the top-level declarations and the blocks.
 *
 * <p>No word of jMX is a lexer token. A document opens with a kind's word followed by a quoted title; a block is a
 * block's word at the start of a statement. Everything is recognised by its position, so {@code points}, {@code set},
 * {@code state} and {@code view} stay free as names inside {@code behaviour} and {@code run}, where jMS reads them.</p>
 */
public final class HandWrittenDocumentReader {

    private final Reading            reading;
    private final DeclarationReader  declarations;
    private final CircuitReader      circuits;
    private final SymbolReader       symbols;
    private final PresentationReader presentation;
    private final StructureReader    structures;
    private final GraphReader        graphs;
    private final ScriptBlocks       scripts;
    private final DocumentRules      rules;

    public HandWrittenDocumentReader(TokenCursor cursor, ParserContext context, Normalized normalized, String document) {
        this.reading = new Reading(cursor, context, normalized, document);
        this.declarations = new DeclarationReader(reading);
        this.circuits = new CircuitReader(reading);
        this.symbols = new SymbolReader(reading);
        this.presentation = new PresentationReader(reading);
        this.structures = new StructureReader(reading);
        this.graphs = new GraphReader(reading, circuits);
        this.scripts = new ScriptBlocks(reading);
        this.rules = new DocumentRules(reading);
    }

    /** Every document of the text, in order. */
    public List<ExperienceDocument> documents() {
        List<ExperienceDocument> documents = new ArrayList<>();
        reading.skipBlank();
        while (!reading.atEnd()) {
            documents.add(document());
            reading.skipBlank();
        }
        if (documents.isEmpty()) {
            throw reading.refusal("a jMX file holds at least one document: 'kind \"Title\" { … }'");
        }
        return documents;
    }

    /** Whether the cursor sits on a document's head: a kind's word followed by a quoted title. */
    private boolean atDocument() {
        return !reading.atEnd() && reading.cursor.current().type() != BasicToken.T_STRING
                && Kind.ofKeyword(reading.cursor.current().value()).isPresent()
                && reading.cursor.isNext(BasicToken.T_STRING);
    }

    private ExperienceDocument document() {
        if (!atDocument()) {
            throw reading.refusal("a document opens with its kind and a title in quotes: 'surface \"Radial wave\" { … }'");
        }
        Token head = reading.cursor.next();
        Kind kind = Kind.ofKeyword(head.value()).orElseThrow();
        declarations.forgetTypes();
        String title = reading.literal(reading.cursor.next());
        Reading.Line rest = reading.line();
        String slug = ExperienceDocument.slugOf(title);
        Optional<String> type = Optional.empty();
        int index = 0;
        while (index < rest.size()) {
            String word = rest.value(index);
            if (word.equals("as") && index + 1 < rest.size()) {
                slug = rest.joinedWord(index + 1);
                index = rest.joinedEnd(index + 1);
            } else if (word.equals("type") && index + 1 < rest.size()) {
                type = Optional.of(rest.joinedWord(index + 1));
                index = rest.joinedEnd(index + 1);
            } else {
                throw rest.refusal(index, "after the title come 'as slug' and 'type role', then '{'");
            }
        }
        if (!slug.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            throw reading.refusal(head, "'" + slug + "' is not a slug: lowercase words and digits joined by hyphens");
        }
        reading.expect(BasicToken.T_OPEN_CURLY, "'{' to open " + kind.keyword() + " \"" + title + "\"");
        Body body = body(kind, head);
        ExperienceDocument document = new ExperienceDocument(kind, title, slug, type, body.meta, body.base,
                body.units, body.uses, body.blocks, reading.lineOf(head));
        rules.validate(document, head);
        return document;
    }

    /** What a document's braces hold, gathered while reading them. */
    private static final class Body {
        Meta                     meta     = Meta.EMPTY;
        Optional<String>         base     = Optional.empty();
        List<String>             units    = List.of();
        List<String>             uses     = new ArrayList<>();
        List<Block>              blocks   = new ArrayList<>();
    }

    private Body body(Kind kind, Token head) {
        Body body = new Body();
        Set<String> written = new HashSet<>();
        SymbolReader.Drawing drawing = kind == Kind.SYMBOL ? symbols.drawing() : null;
        while (true) {
            reading.skipBlank();
            if (reading.atEnd()) {
                throw reading.refusal(head, kind.keyword() + " \"…\" is opened with '{' and never closed");
            }
            if (reading.cursor.consumeIf(BasicToken.T_CLOSE_CURLY)) {
                break;
            }
            if (atDocument()) {
                throw reading.refusal("a " + kind.keyword() + " does not hold other documents: one object is one file, "
                        + "and a collection lists them by slug");
            }
            if (drawing != null && symbols.atDrawingLine()) {
                drawing.read(reading.line());
                continue;
            }
            Token keyword = reading.cursor.next();
            String name = keyword.value();
            if (reading.isString(keyword)) {
                throw reading.refusal(keyword, "a text in quotes cannot start a line here");
            }
            switch (name) {
                case "extends" -> body.base = Optional.of(reading.wordAfter(keyword, "extends"));
                case "units" -> body.units = reading.namesAfter(keyword, "units");
                case "use" -> body.uses.addAll(reading.namesAfter(keyword, "use"));
                case "type" -> declarations.type(reading.line());
                default -> {
                    if (!kind.allows(name)) {
                        throw reading.refusal(keyword, "'" + name + "' is not a block of a " + kind.keyword()
                                + "; it holds " + String.join(", ", kind.blocks().stream().sorted().toList()));
                    }
                    if (!Kind.REPEATABLE_BLOCKS.contains(name) && !written.add(name)) {
                        throw reading.refusal(keyword, "'" + name + "' is written twice");
                    }
                    if (name.equals("meta")) {
                        body.meta = presentation.meta();
                    } else {
                        body.blocks.add(kind == Kind.GRAPH && name.equals("expect") ? graphs.check(keyword) : block(name, keyword));
                    }
                }
            }
        }
        if (drawing != null) {
            body.blocks.add(drawing.symbol());
        }
        return body;
    }

    private Block block(String name, Token keyword) {
        return switch (name) {
            case "parameters" -> declarations.parameters();
            case "inputs" -> declarations.inputs();
            case "outputs" -> declarations.outputs();
            case "state" -> declarations.state();
            case "equations" -> declarations.equations();
            case "heat" -> declarations.heat();
            case "image" -> declarations.image();
            case "display" -> declarations.display();
            case "measure" -> declarations.measure();
            case "leads" -> declarations.leads();
            case "pins" -> declarations.pins();
            case "nodes" -> declarations.nodes();
            case "formulas" -> scripts.formulas(keyword);
            case "behaviour" -> scripts.behaviour();
            case "run" -> scripts.run();
            case "view" -> presentation.view();
            case "expect" -> circuits.expectation(keyword);
            case "bench" -> circuits.sharedBench(keyword);
            case "symbol" -> symbols.symbol();
            case "parts" -> circuits.parts();
            case "nets" -> circuits.nets();
            case "layout" -> circuits.layout();
            case "ports" -> circuits.ports();
            case "probes" -> circuits.probes();
            case "analysis" -> circuits.analysis(keyword);
            case "sheet" -> circuits.sheet(keyword);
            case "maps" -> structures.maps();
            case "axiom" -> structures.axiom(keyword);
            case "rules" -> structures.rules();
            case "turtle" -> structures.turtle(presentation);
            case "steps" -> structures.steps();
            case "vertices" -> structures.vertices(keyword);
            case "edges" -> structures.edges(keyword);
            case "points" -> structures.points();
            case "items" -> structures.items(keyword);
            case "step" -> structures.courseStep(keyword);
            case "lesson" -> structures.lesson(keyword);
            case "section" -> structures.section(keyword);
            case "objects" -> structures.objectKinds(keyword);
            case "representations" -> structures.representations(keyword);
            case "opens" -> structures.opens(keyword);
            case "clock" -> graphs.clock(keyword);
            case "panes" -> graphs.panes(keyword);
            case "lines" -> graphs.lines();
            default -> throw reading.refusal(keyword, "'" + name + "' is not a block jMX knows");
        };
    }
}
