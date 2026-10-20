package org.jmouse.parallax.script.legacy;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.ParameterFlag;
import org.jmouse.parallax.script.Capabilities;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.legacy.ExperienceNotation.Normalized;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.script.el.ScriptParseException;
import org.jmouse.script.el.node.ScriptNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The structure of a jMX text: documents, their heads, the top-level declarations and the blocks.
 *
 * <p>No word of jMX is a lexer token. A document opens with a kind's word followed by a quoted title; a block is a
 * block's word at the start of a statement. Everything is recognised by its position, so {@code points}, {@code set},
 * {@code state} and {@code view} stay free as names inside {@code behaviour} and {@code run}, where jMS reads them.</p>
 */
public final class DocumentReader {

    private final Reading            reading;
    private final DeclarationReader  declarations;
    private final CircuitReader      circuits;
    private final SymbolReader       symbols;
    private final PresentationReader presentation;
    private final StructureReader    structures;
    private final GraphReader        graphs;

    public DocumentReader(TokenCursor cursor, ParserContext context, Normalized normalized, String document) {
        this.reading = new Reading(cursor, context, normalized, document);
        this.declarations = new DeclarationReader(reading);
        this.circuits = new CircuitReader(reading);
        this.symbols = new SymbolReader(reading);
        this.presentation = new PresentationReader(reading);
        this.structures = new StructureReader(reading);
        this.graphs = new GraphReader(reading, circuits);
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
        validate(document, head);
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
                case "extends" -> body.base = Optional.of(word(keyword, "extends"));
                case "units" -> body.units = names(keyword, "units");
                case "use" -> body.uses.addAll(names(keyword, "use"));
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
            case "formulas" -> formulas(keyword);
            case "behaviour" -> new Block.Behaviour(script("behaviour"));
            case "run" -> run();
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

    /** {@code formulas { jMF definitions }}. A shared library is pulled in at the top with {@code use <slug>}. */
    private Block.Formulas formulas(Token keyword) {
        if (reading.at(BasicToken.T_OPEN_CURLY)) {
            return new Block.Formulas(Optional.of(reading.rawBlock("formulas")));
        }
        throw reading.refusal(keyword, "formulas are '{ definitions }'; to pull in a shared library write "
                + "'use <slug>' at the top of the document");
    }

    private Block.Run run() {
        int start = reading.cursor.current().offset();
        ScriptNode script = script("run");
        int end = reading.cursor.lookAt(-1).offset() + 1;
        return new Block.Run(script, reading.text(start + 1, end - 1));
    }

    /** A brace-delimited jMS body, read by jMS's own statement reader. */
    private ScriptNode script(String name) {
        if (!reading.at(BasicToken.T_OPEN_CURLY)) {
            throw reading.refusal("'" + name + "' is followed by '{ … }' holding jMS");
        }
        Node holder = BasicNode.forToken(reading.cursor.current());
        try {
            reading.context.getParser(StatementsParser.class).parse(reading.cursor, holder, reading.context);
        } catch (ScriptParseException failure) {
            throw new ExperienceParseException(reading.document(), failure.at().line(), failure.at().column(), failure.detail());
        } catch (RuntimeException failure) {
            throw reading.refusal("in '" + name + "': " + failure.getMessage());
        }
        ScriptNode script = new ScriptNode();
        script.setName(name);
        for (Node statement : holder.getChildren().getFirst().getChildren()) {
            script.addExpression((Expression) statement);
        }
        return script;
    }

    private String word(Token keyword, String what) {
        Reading.Line line = reading.line();
        if (line.isEmpty()) {
            throw reading.refusal(keyword, "'" + what + "' takes a name");
        }
        return line.joinedWord(0);
    }

    private List<String> names(Token keyword, String what) {
        List<String> names = reading.line().names(0);
        if (names.isEmpty()) {
            throw reading.refusal(keyword, "'" + what + "' takes names separated by commas");
        }
        return names;
    }

    /** Rules that need the whole document: time is either an input or what evolves the state, never both. */
    private void validate(ExperienceDocument document, Token head) {
        boolean animated = document.parameters().stream().anyMatch(parameter -> parameter.has(ParameterFlag.ANIMATE));
        if (animated && Capabilities.evolves(document)) {
            throw reading.refusal(head, "'" + document.title() + "' evolves its state in time, so no parameter can be "
                    + "'animate': time is either an input or what runs the state, not both");
        }
        if (document.kind() == Kind.COMPONENT) {
            for (Block.Expectation expectation : document.blocks(Block.Expectation.class)) {
                if (expectation.bench().isEmpty() && expectation.sharedBench() == null) {
                    throw new ExperienceParseException(reading.document(), expectation.line(), 0, "expectation '"
                            + expectation.title() + "' of a component needs a bench: 'bench for 3 ms { parts }'");
                }
            }
        }
        for (Block.Output output : document.outputs()) {
            if (output.components().stream().anyMatch(SourceText::isEmpty)) {
                throw reading.refusal(head, "output '" + output.name() + "' has an empty expression");
            }
        }
        if (document.kind() == Kind.GRAPH) {
            validateGraph(document, head);
        }
    }

    /** A clock moves a parameter or names its own range; a line is drawn in a pane the graph has. */
    private void validateGraph(ExperienceDocument document, Token head) {
        document.block(Block.Clock.class).ifPresent(clock -> {
            boolean declared = document.parameters().stream().anyMatch(parameter -> parameter.name().equals(clock.parameter()));
            if (!declared && clock.range().isEmpty()) {
                throw reading.refusal(head, "the clock moves '" + clock.parameter() + "', which is not a parameter: "
                        + "declare it, or give the clock its range — 'clock " + clock.parameter() + " from 0 to 1'");
            }
        });
        List<Block.Pane> written = document.block(Block.Panes.class).map(Block.Panes::panes).orElse(List.of());
        Set<String> panes = written.stream().map(Block.Pane::name).collect(Collectors.toSet());
        Set<String> circuitPanes = written.stream().filter(pane -> pane.kind() == Block.PaneKind.CIRCUIT)
                .map(Block.Pane::name).collect(Collectors.toSet());
        for (Block.GraphLine line : document.block(Block.Lines.class).map(Block.Lines::lines).orElse(List.of())) {
            if (line.from().isPresent() && !circuitPanes.contains(line.from().get())) {
                throw new ExperienceParseException(reading.document(), line.line(), 0, "line '" + line.name()
                        + "' reads its signal from '" + line.from().get() + "', which is not a circuit pane of this graph");
            }
            if (line.pane().isPresent() && !panes.contains(line.pane().get())) {
                throw new ExperienceParseException(reading.document(), line.line(), 0, "line '" + line.name()
                        + "' is drawn in '" + line.pane().get() + "', which is not a pane of this graph");
            }
        }
    }
}
