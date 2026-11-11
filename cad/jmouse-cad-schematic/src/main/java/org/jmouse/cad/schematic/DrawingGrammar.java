package org.jmouse.cad.schematic;

import org.jmouse.cad.document.DocumentValues;
import org.jmouse.el.grammar.ClauseBlockParser;
import org.jmouse.el.grammar.ClauseBlockParser.Body;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ClauseLineParser;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.LineReader;
import org.jmouse.el.grammar.LineSet;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.jmouse.el.grammar.Values.ASSIGNED;
import static org.jmouse.el.grammar.Values.EXTENT;
import static org.jmouse.el.grammar.Values.FLAG;
import static org.jmouse.el.grammar.Values.FORMULA;
import static org.jmouse.el.grammar.Values.NAME;
import static org.jmouse.el.grammar.Values.NAMES;
import static org.jmouse.el.grammar.Values.POINT;
import static org.jmouse.el.grammar.Values.QUANTITY;
import static org.jmouse.el.grammar.Values.RANGE;
import static org.jmouse.el.grammar.Values.WORD;

/**
 * Every line a drawing writes — a symbol's, a portrait's, a frame's —, said as a grammar: its size, its pins and labels,
 * its inputs, its elements, each a block with a head and, when it has properties, a body; a layer drawn while a condition
 * holds; the properties of an element, written as assignments, as the mathematics they are.
 */
public final class DrawingGrammar {

    /** The word that makes an element, a property or a layer follow the part's figures. */
    public static final String LIVE = "live";

    /** The word that makes a property move by itself, as time goes. */
    public static final String ANIMATE = "animate";

    /** {@code size 70 x 130}: the drawing's extent, around its origin. */
    public static final LineGrammar SIZE = LineGrammar.line("size").slot("extent", EXTENT)
            .shaped("a drawing's size is 'size width x height'");

    /** {@code pin positive at -20, 60 ["label"]}: where a pin of the part meets the drawing. */
    public static final LineGrammar PIN = LineGrammar.line("pin").slot("name", NAME).termName("name").required("at", POINT)
            .ending("label").shaped("a pin is 'pin name at x, y [\"label\"]'");

    /** {@code label name at x, y}, {@code label value at x, y}: where the part's reference or value is written. */
    public static final LineGrammar LABEL = LineGrammar.line("label").slot("subject", WORD, "name", "value").required("at", POINT)
            .shaped("a label is 'label name|value at x, y'");

    /** {@code indicator name from x, y to x, y}: a figure the drawing shows as a bar. */
    public static final LineGrammar INDICATOR = LineGrammar.line("indicator").slot("name", NAME).termName("name")
            .required("from", POINT).required("to", POINT).shaped("an indicator is 'indicator name from x, y to x, y'");

    /**
     * {@code input capacitance 1 µF .. 9999 µF typical 470 µF}, {@code input failed}, {@code input photo picture},
     * {@code input marking text ["typical"]}: what the drawing is given by the part it draws.
     */
    public static final LineGrammar INPUT = LineGrammar.line("input").slot("name", NAME).termName("name")
            .optionalSlot("range", RANGE)
            .optional("typical", QUANTITY)
            .optional("picture", FLAG)
            .optional("text", FLAG)
            .ending("typical text")
            .check(DrawingGrammar::input)
            .shaped("an input is 'input name [low .. high [typical v]]', 'input name picture' or 'input name text [\"typical\"]'");

    /** {@code hide body, leads}: the elements a layer hides while it is shown. */
    public static final LineGrammar HIDE = LineGrammar.line("hide").slot("names", NAMES)
            .shaped("a layer hides 'hide name, name …'");

    /**
     * {@code fill = black}, {@code live character = …}, {@code animate opacity = …}: a property of an element, its value an
     * expression. An assignment is mathematics: a term keeps it as it is written.
     */
    public static final LineGrammar PROPERTY = LineGrammar.named("property", "name").prefixed(LIVE, ANIMATE)
            .slot("value", ASSIGNED).writtenAsIs()
            .shaped("a property is '[live|animate] name = value'");

    /** The shapes an element is drawn as. */
    public static final List<String> SHAPES = List.of("path", "box", "circle", "line", "glyph", "bitmap", "group");

    /** {@code live when failed { … }}: a layer drawn while its condition holds; the head a program makes a layer with. */
    public static final LineGrammar LAYER = LineGrammar.line("when").termed("layer").prefixed(LIVE).slot("condition", FORMULA)
            .check((layer, grammar) -> layer.has(LIVE) ? null : "a layer shown on a condition is always 'live when <condition> { … }'")
            .shaped("a layer is 'live when <condition> { … }'");

    /** The head of every element, by its shape. */
    private static final Map<String, LineGrammar> HEADS = new LinkedHashMap<>();

    /** Every element and layer, each a block; a group's body and a layer's hold them again. */
    private static final List<ClauseBlockParser> ELEMENTS = new ArrayList<>();

    static {
        LineReader anyElement = new Deferred(() -> ELEMENTS);
        LineSet properties = LineSet.of(new ClauseLineParser(PROPERTY));
        LineSet nested = LineSet.of(anyElement, new ClauseLineParser(PROPERTY));
        for (String shape : SHAPES) {
            boolean group = shape.equals("group");
            HEADS.put(shape, head(shape));
            ELEMENTS.add(new ClauseBlockParser(shape, HEADS.get(shape), group ? Body.REQUIRED : Body.OPTIONAL,
                    group ? nested : properties));
        }
        ELEMENTS.add(new ClauseBlockParser("when", LAYER, Body.REQUIRED,
                LineSet.of(anyElement, new ClauseLineParser(HIDE), new ClauseLineParser(PROPERTY))));
    }

    /** What a drawing's body holds: its lines, then its elements and layers. */
    public static LineSet body() {
        List<LineReader> readers = new ArrayList<>();
        readers.add(new Deferred(() -> ELEMENTS));
        for (LineGrammar line : List.of(SIZE, PIN, LABEL, INDICATOR, INPUT)) {
            readers.add(new ClauseLineParser(line));
        }
        return new LineSet(readers);
    }

    private DrawingGrammar() {
    }

    /** The head of an element of a shape: what a program makes an element's block with. */
    public static LineGrammar element(String shape) {
        LineGrammar head = HEADS.get(shape);
        if (head == null) {
            throw new IllegalArgumentException("a drawing has no element '" + shape + "'; it has " + String.join(", ", SHAPES));
        }
        return head;
    }

    /**
     * An element's head: {@code [live] shape [name] [at x, y] [size w x h] [radius r] [from x, y] [to x, y] [pivot x, y]},
     * a path's data in quotes after it and {@code upright} for a path that stays standing as the part turns.
     */
    private static LineGrammar head(String shape) {
        LineGrammar.Builder head = LineGrammar.line(shape).prefixed(LIVE).optionalSlot("name", NAME).termName("name")
                .optional("at", POINT).optional("size", EXTENT).optional("radius", QUANTITY).optional("from", POINT)
                .optional("to", POINT).optional("pivot", POINT);
        if (shape.equals("path")) {
            head = head.optional("upright", FLAG).ending("data", DocumentValues.PATH);
        }
        return head.check((element, grammar) -> placing(shape, element)).shaped(shapeOf(shape));
    }

    /** What an element of a shape must be placed by, as the refusal says it. */
    private static String placing(String shape, ClauseLineNode element) {
        return switch (shape) {
            case "path" -> element.ending().isEmpty() ? shapeOf(shape) : null;
            case "box", "glyph", "bitmap" -> element.has("at") && element.has("size") ? null : shapeOf(shape);
            case "circle" -> element.has("at") && element.has("radius") ? null : shapeOf(shape);
            case "line" -> element.has("from") && element.has("to") ? null : shapeOf(shape);
            default -> null;
        };
    }

    private static String shapeOf(String shape) {
        return switch (shape) {
            case "path" -> "a path is '[live] path [name] \"SVG path data\" [upright] [{ properties }]'";
            case "box", "glyph", "bitmap" -> "a " + shape + " is '[live] " + shape + " [name] at x, y size width x height', placed by its centre";
            case "circle" -> "a circle is '[live] circle [name] at x, y radius r'";
            case "line" -> "a line is '[live] line [name] from x, y to x, y'";
            default -> "a group is '[live] group [name] [pivot x, y] { elements }'";
        };
    }

    /** An input's range runs from a lower to a higher value, and its typical value lies within it. */
    private static String input(ClauseLineNode input, LineGrammar grammar) {
        Optional<double[]> range = input.slot("range").map(value -> ((org.jmouse.el.grammar.RangeNode) value))
                .filter(written -> written.low().isPresent())
                .map(written -> new double[] {quantity(written.low().get().written()), quantity(written.high().get().written())});
        if (range.isPresent() && !(range.get()[1] > range.get()[0])) {
            return "an input's range runs from a lower to a higher value";
        }
        Optional<Double> typical = input.quantity("typical");
        if (typical.isPresent() && range.isPresent() && (typical.get() < range.get()[0] || typical.get() > range.get()[1])) {
            return "an input's typical value lies within its range";
        }
        return null;
    }

    private static double quantity(String written) {
        return Quantity.parse(written).map(Quantity::value)
                .orElseThrow(() -> new IllegalArgumentException("'" + written + "' is no number with its unit"));
    }

    /** A reader of whichever of a list of readers opens, the list taken when first asked: a group holds groups. */
    private record Deferred(Supplier<List<ClauseBlockParser>> readers) implements LineReader {

        @Override
        public List<LineReader> alternatives() {
            return List.copyOf(readers.get());
        }

        @Override
        public boolean opens(TokenCursor cursor) {
            return readers.get().stream().anyMatch(reader -> reader.opens(cursor));
        }

        @Override
        public Expression read(TokenCursor cursor, ParserContext context) {
            return readers.get().stream().filter(reader -> reader.opens(cursor)).findFirst().orElseThrow().read(cursor, context);
        }

        @Override
        public boolean opensTerm(TokenCursor cursor) {
            return readers.get().stream().anyMatch(reader -> reader.opensTerm(cursor));
        }

        @Override
        public Expression readTerm(TokenCursor cursor, ParserContext context) {
            return readers.get().stream().filter(reader -> reader.opensTerm(cursor)).findFirst().orElseThrow().readTerm(cursor, context);
        }

        @Override
        public String says() {
            return "an element: " + String.join(", ", SHAPES) + " or 'live when'";
        }
    }
}
