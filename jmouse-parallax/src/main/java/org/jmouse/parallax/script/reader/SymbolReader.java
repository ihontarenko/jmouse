package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor.Boundary;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.SymbolElement;
import org.jmouse.parallax.script.Block.SymbolInput;
import org.jmouse.parallax.script.Block.SymbolLabel;
import org.jmouse.parallax.script.Block.SymbolMotion;
import org.jmouse.parallax.script.Block.SymbolPin;
import org.jmouse.parallax.script.Block.SymbolProperty;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.units.Quantity;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * How a part is drawn, in three forms:
 *
 * <pre>
 * symbol uses resistor-symbol, fuse-symbol { a terminal_one;  b terminal_two }   symbol documents, the default first
 * symbol box { left d, clk edge;  right q;  bottom nclr }                         a shape, pins placed by side
 * symbol { size 60 x 40;  path "M…";  pin a at -30, 0;  label name at 0, -26 }    a drawing of its own
 * </pre>
 *
 * A {@code symbol} document's body is the third form without the braces. The lines after {@code uses} map a
 * symbol's port to the component's pin — a port they leave out is the pin of the same name — or bind one of the
 * symbol's inputs to an expression over the part: {@code closed = pressed}.
 *
 * <p>A drawing may live: {@code input closed} declares what it is drawn from, and its elements — {@code path},
 * {@code box}, {@code circle}, {@code line}, {@code group}, and {@code live when <condition> { … }} — carry properties
 * written {@code name = value}, {@code live name = …} when the value follows the inputs and {@code animate name = …}
 * when it moves in time by itself. A block holding anything that changes is written {@code live} too.</p>
 */
final class SymbolReader {

    /** The shapes drawn from their sides alone. */
    private static final List<String> SHAPES = List.of("box", "triangle");

    /** What a drawing's {@code label} lines place. */
    private static final List<String> LABEL_SUBJECTS = List.of("name", "value");

    /** The words that open a drawing's elements. */
    private static final Set<String> ELEMENT_SHAPES = Set.of("path", "box", "circle", "line", "glyph", "bitmap", "group");

    /** The words a living drawing adds to {@link Kind#DRAWING_LINES}. */
    private static final Set<String> LIVING_LINES = Set.of("input", "live", "box", "circle", "line", "glyph", "bitmap", "group");

    /** The words that place an element, each followed by its value. */
    private static final Set<String> GEOMETRY_WORDS = Set.of("at", "size", "radius", "from", "to", "pivot");

    /** The marks before a property. */
    private static final Map<String, SymbolMotion> MOTIONS = Map.of("live", SymbolMotion.LIVE, "animate", SymbolMotion.ANIMATED);

    /** How many significant digits a quantity keeps once it is in SI: enough for any written value, none of the binary noise. */
    private static final MathContext SI_DIGITS = new MathContext(12);

    /** The most tokens a unit after a number is read from inside a drawing's expression. */
    private static final int LONGEST_UNIT = 3;

    /** A point's x ends at its comma. */
    private static final Boundary COMMA = (token, after, depth) -> depth == 0 && token.type() == BasicToken.T_COMMA;

    private final Reading reading;
    private final JmxValues values;

    SymbolReader(Reading reading) {
        this.reading = reading;
        this.values = new JmxValues(reading);
    }

    /** The block after the word {@code symbol}. */
    Block.Symbol symbol() {
        if (!reading.atLineEnd() && reading.cursor.checkValueAt(0, "uses")) {
            return reference();
        }
        if (!reading.atLineEnd()) {
            return shape();
        }
        Drawing drawing = new Drawing();
        List<TokenCursor.Savepoint> starts = reading.lineStarts("symbol");
        TokenCursor.Savepoint after = reading.cursor.savepoint();
        for (TokenCursor.Savepoint start : starts) {
            reading.cursor.restore(start);
            drawing.read();
        }
        reading.cursor.restore(after);
        return drawing.symbol();
    }

    /**
     * {@code uses resistor-symbol, fuse-symbol}, then in braces when there are any: port lines ({@code a terminal_one})
     * and input bindings ({@code closed = pressed}).
     */
    private Block.Symbol reference() {
        List<String> variants = variants();
        Map<String, String> ports = new LinkedHashMap<>();
        Map<String, SourceText> bindings = new LinkedHashMap<>();
        if (reading.cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            List<TokenCursor.Savepoint> starts = reading.lineStarts("symbol uses");
            TokenCursor.Savepoint after = reading.cursor.savepoint();
            for (TokenCursor.Savepoint start : starts) {
                reading.cursor.restore(start);
                portOrBinding(ports, bindings);
            }
            reading.cursor.restore(after);
        }
        return new Block.Symbol(variants, ports, Optional.empty(), Map.of(), 0, 0, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), bindings);
    }

    private void portOrBinding(Map<String, String> ports, Map<String, SourceText> bindings) {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        if (line.remaining() >= 2 && line.checkValueAt(1, "=")) {
            if (line.remaining() < 3) {
                throw reading.refusalAhead(1, "an input binding is 'input = expression': 'closed = pressed'");
            }
            String input = cursor.next().value();
            cursor.next();
            if (bindings.put(input, values.expression(JmxValues.LINE)) != null) {
                throw reading.refusal(first, "input '" + input + "' is bound twice");
            }
            reading.requireLineEnd();
            return;
        }
        String shape = "a line here is 'port pin' — the symbol's port, then the component's pin it is — "
                + "or 'input = expression' — what one of the symbol's inputs is drawn from";
        String port = reading.joinedWord();
        if (reading.atLineEnd()) {
            throw reading.refusal(first, shape);
        }
        String pin = reading.joinedWord();
        if (!reading.atLineEnd()) {
            throw reading.refusal(first, shape);
        }
        if (ports.put(port, pin) != null) {
            throw reading.refusal(first, "port '" + port + "' is mapped twice");
        }
    }

    /** {@code uses a-symbol, b-symbol}: one slug between commas, none twice. */
    private List<String> variants() {
        TokenCursor cursor = reading.cursor;
        String shape = "a symbol reference is 'symbol uses slug[, slug…]': the symbols this part may be drawn with, "
                + "the default first";
        Token uses = cursor.next();
        List<String> variants = new ArrayList<>();
        Token previous = uses;
        while (true) {
            if (reading.atLineEnd() || cursor.isCurrent(BasicToken.T_COMMA)) {
                throw reading.refusal(reading.atLineEnd() ? previous : cursor.current(), shape);
            }
            Token from = cursor.current();
            String slug = reading.joinedWord();
            if (!reading.atLineEnd() && !cursor.isCurrent(BasicToken.T_COMMA)) {
                throw reading.refusal(from, shape);
            }
            if (variants.contains(slug)) {
                throw reading.refusal(from, "'" + slug + "' is named twice");
            }
            variants.add(slug);
            if (reading.atLineEnd()) {
                return variants;
            }
            previous = cursor.next();
        }
    }

    /** {@code box { left d, clk edge;  right q }}: a shape and the pins on each of its sides. */
    private Block.Symbol shape() {
        Token first = reading.cursor.current();
        String shape = reading.joinedWord();
        if (!SHAPES.contains(shape)) {
            throw reading.refusal(first, "'" + shape + "' is not a shape: a part is drawn as 'symbol box { … }', "
                    + "'symbol triangle { … }', or from a symbol document with 'symbol uses <slug> { port pin … }'");
        }
        reading.skipLine();
        Map<String, List<String>> sides = new LinkedHashMap<>();
        List<TokenCursor.Savepoint> starts = reading.lineStarts("symbol");
        TokenCursor.Savepoint after = reading.cursor.savepoint();
        for (TokenCursor.Savepoint start : starts) {
            reading.cursor.restore(start);
            String side = reading.cursor.next().value();
            List<String> entries = new ArrayList<>();
            StringBuilder entry = new StringBuilder();
            // d, clk edge → ["d", "clk edge"]: each pin with the words that follow it.
            while (!reading.atLineEnd()) {
                Token token = reading.cursor.next();
                String word = reading.isString(token) ? reading.literal(token) : token.value();
                if (word.equals(",")) {
                    entries.add(entry.toString().strip());
                    entry.setLength(0);
                } else {
                    entry.append(entry.isEmpty() ? "" : " ").append(word);
                }
            }
            if (!entry.isEmpty()) {
                entries.add(entry.toString().strip());
            }
            sides.put(side, List.copyOf(entries));
        }
        reading.cursor.restore(after);
        return new Block.Symbol(List.of(), Map.of(), Optional.of(shape), sides, 0, 0, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    /** The lines of a drawn symbol, collected one at a time. */
    final class Drawing {

        private double width;
        private double height;
        private final List<String>      paths  = new ArrayList<>();
        private final List<String>      upright = new ArrayList<>();
        private final List<SymbolPin>   pins   = new ArrayList<>();
        private final List<SymbolLabel> labels = new ArrayList<>();
        private final List<Block.SymbolIndicator> indicators = new ArrayList<>();
        private final List<SymbolInput> inputs = new ArrayList<>();
        private final List<SymbolElement> elements = new ArrayList<>();

        /** Whether a word opens a drawing line. */
        static boolean opens(String word) {
            return Kind.DRAWING_LINES.contains(word) || LIVING_LINES.contains(word);
        }

        /** The drawing line the cursor stands on, and an element's braces after it when it has them. */
        void read() {
            TokenCursor cursor = reading.cursor;
            TokenCursor line = reading.lineView();
            Token first = cursor.current();
            int length = line.remaining();
            switch (first.value()) {
                case "size" -> {
                    if (line.offsetOf(1, "x") < 0) {
                        throw reading.refusal(first, "a size is 'size width x height'");
                    }
                    cursor.next();
                    width = values.quantity(JmxValues.words(Set.of("x")));
                    cursor.next();
                    height = values.quantity(JmxValues.LINE);
                }
                case "path" -> {
                    boolean plain = length == 2 && reading.isString(line.lookAt(1));
                    boolean standing = length == 3 && reading.isString(line.lookAt(1)) && line.checkValueAt(2, "upright");
                    boolean opened = cursor.lookAt(length).type() == BasicToken.T_OPEN_CURLY;
                    if ((plain || standing) && !opened) {
                        (standing ? upright : paths).add(reading.literal(line.lookAt(1)));
                        reading.skipLine();
                    } else {
                        elements.add(element());
                    }
                }
                case "pin" -> {
                    if (line.offsetOf(2, "at") != 2) {
                        throw reading.refusal(first, "a symbol pin is 'pin name at x, y [\"label\"]'");
                    }
                    String name = line.valueAt(1);
                    if (pins.stream().anyMatch(pin -> pin.pin().equals(name))) {
                        throw reading.refusal(first, "pin '" + name + "' is drawn twice");
                    }
                    boolean labelled = reading.isString(line.lookAt(length - 1));
                    String label = labelled ? reading.literal(line.lookAt(length - 1)) : "";
                    int labelAt = labelled ? line.lookAt(length - 1).offset() : -1;
                    cursor.shift(3);
                    double[] point = point(labelAt);
                    reading.skipLine();
                    pins.add(new SymbolPin(name, point[0], point[1], label));
                }
                case "label" -> {
                    if (line.offsetOf(2, "at") != 2 || !LABEL_SUBJECTS.contains(line.valueAt(1))) {
                        throw reading.refusal(first, "a label is 'label name at x, y' or 'label value at x, y'");
                    }
                    String subject = line.valueAt(1);
                    cursor.shift(3);
                    double[] point = point(-1);
                    labels.add(new SymbolLabel(subject, point[0], point[1]));
                }
                case "indicator" -> {
                    int from = line.offsetOf(2, "from");
                    int to = line.offsetOf(from + 1, "to");
                    if (from != 2 || to < 0) {
                        throw reading.refusal(first, "an indicator is 'indicator name from x, y to x, y'");
                    }
                    String name = line.valueAt(1);
                    int toAt = line.lookAt(to).offset();
                    cursor.shift(3);
                    double[] start = point(toAt);
                    cursor.next();
                    double[] end = point(-1);
                    indicators.add(new Block.SymbolIndicator(name, Math.min(start[0], end[0]), Math.min(start[1], end[1]),
                            Math.max(start[0], end[0]), Math.max(start[1], end[1])));
                }
                case "input" -> inputs.add(input());
                case "live", "box", "circle", "line", "glyph", "bitmap", "group" -> elements.add(element());
                default -> throw reading.refusal(first, "'" + first.value() + "' is not part of a drawing: size, path, box, "
                        + "circle, line, glyph, group, live, input, pin, label or indicator");
            }
        }

        /** {@code input closed}, {@code input current 0 .. 20 mA}, {@code input screen picture}. */
        private SymbolInput input() {
            TokenCursor cursor = reading.cursor;
            TokenCursor line = reading.lineView();
            Token first = cursor.current();
            int length = line.remaining();
            if (length < 2 || reading.isString(line.lookAt(1))) {
                throw reading.refusal(first, "an input is 'input name [low .. high]' or 'input name picture': "
                        + "'input closed', 'input current 0 .. 20 mA', 'input screen picture'");
            }
            String name = line.valueAt(1);
            if (inputs.stream().anyMatch(input -> input.name().equals(name))) {
                throw reading.refusalAhead(1, "input '" + name + "' is declared twice");
            }
            if (length == 2) {
                reading.skipLine();
                return new SymbolInput(name, 0, 1, Optional.empty(), false);
            }
            if (length == 3 && line.checkValueAt(2, "picture")) {
                reading.skipLine();
                return new SymbolInput(name, 0, 1, Optional.empty(), true);
            }
            if (line.offsetOf(2, "..") < 0) {
                throw reading.refusalAhead(2, "an input's range is 'low .. high': 'input current 0 .. 20 mA'");
            }
            Token rangeStart = line.lookAt(2);
            int endAt = line.lookAt(length).offset();
            cursor.shift(2);
            double low = values.quantity(JmxValues.words(Set.of("..")));
            cursor.next();
            double high = values.quantity(JmxValues.LINE);
            if (!(high > low)) {
                throw reading.refusal(rangeStart, "an input's range runs from a lower to a higher value");
            }
            return new SymbolInput(name, low, high, Optional.of(reading.text(rangeStart.offset(), endAt)), false);
        }

        /**
         * One element, from its line to the end of its braces when it has any:
         * {@code [live] shape [name] [at x, y] [size w x h] [radius r] [from x, y] [to x, y] [pivot x, y] ["path data"]}
         * or {@code live when <condition>}.
         */
        private SymbolElement element() {
            TokenCursor cursor = reading.cursor;
            TokenCursor line = reading.lineView();
            Token first = cursor.current();
            boolean live = line.checkValueAt(0, "live");
            int position = live ? 1 : 0;
            boolean opened = cursor.lookAt(line.remaining()).type() == BasicToken.T_OPEN_CURLY;
            String shape = line.valueAt(position);
            if (shape.equals("when")) {
                return layer(first, live, position, opened);
            }
            if (!ELEMENT_SHAPES.contains(shape)) {
                throw reading.refusalAhead(position, "'" + shape + "' is not an element: path, box, circle, line, glyph, "
                        + "group or 'live when'");
            }
            cursor.shift(position + 1);
            return shaped(first, live, shape, opened);
        }

        /** {@code live when <condition> { … }}: a layer drawn while its condition holds. */
        private SymbolElement layer(Token first, boolean live, int position, boolean opened) {
            TokenCursor line = reading.lineView();
            if (!live) {
                throw reading.refusalAhead(position, "a layer shown on a condition is always 'live when <condition> { … }'");
            }
            if (line.remaining() <= position + 1 || !opened) {
                throw reading.refusalAhead(position, "a layer is 'live when <condition> { … }': the condition, then its "
                        + "elements in braces");
            }
            reading.cursor.shift(position + 1);
            SymbolProperty condition = property(first, "when", SymbolMotion.LIVE);
            Body body = body();
            return new SymbolElement("when", "", true, "", Map.of(), condition, body.properties, body.hidden,
                    body.children, first.lineNumber());
        }

        /** A shape or a group, placed by the words after it, with what its braces hold. */
        private SymbolElement shaped(Token first, boolean live, String shape, boolean opened) {
            Placing placing = placing(first, shape);
            Body body = opened ? body() : new Body();
            if (!shape.equals("group") && !body.children.isEmpty()) {
                throw reading.refusal(first, "only a group or a 'live when' holds other elements");
            }
            if (!body.hidden.isEmpty()) {
                throw reading.refusal(first, "'hide' stands only in a 'live when' layer");
            }
            if (shape.equals("group") && !opened) {
                throw reading.refusal(first, "a group holds its elements in braces: 'group lever pivot -13, 0 { … }'");
            }
            return new SymbolElement(shape, placing.name(), live, placing.path(), placing.geometry(), null, body.properties,
                    List.of(), body.children, first.lineNumber());
        }

        /** What an element's line says after its shape: its name, a path's data, and where it stands. */
        private record Placing(String name, String path, Map<String, List<Double>> geometry) {
        }

        private Placing placing(Token first, String shape) {
            TokenCursor cursor = reading.cursor;
            String name = "";
            if (!reading.atLineEnd() && !cursor.isCurrent(BasicToken.T_STRING) && !GEOMETRY_WORDS.contains(cursor.current().value())) {
                name = cursor.next().value();
            }
            String path = "";
            Map<String, List<Double>> geometry = new LinkedHashMap<>();
            while (!reading.atLineEnd()) {
                Token token = cursor.current();
                if (reading.isString(token)) {
                    if (!shape.equals("path") || !path.isEmpty()) {
                        throw reading.refusal(token, "only a path carries path data, once");
                    }
                    path = reading.literal(cursor.next());
                    continue;
                }
                String word = token.value();
                if (!GEOMETRY_WORDS.contains(word) || geometry.containsKey(word)) {
                    throw reading.refusal(token, "'" + word + "' does not place a " + shape
                            + ": it is placed with at, size, radius, from, to and pivot, each once");
                }
                cursor.next();
                geometry.put(word, geometryValue(word));
            }
            requireGeometry(first, shape, path, geometry);
            return new Placing(name, path, geometry);
        }

        /** {@code at x, y}, {@code size w x h}, {@code radius r}: up to the next geometry word, a path's data, or the end. */
        private List<Double> geometryValue(String word) {
            TokenCursor line = reading.lineView();
            Token from = reading.cursor.current();
            int end = line.remaining();
            for (int offset = 0; offset < line.remaining(); offset++) {
                if (reading.isString(line.lookAt(offset)) || GEOMETRY_WORDS.contains(line.valueAt(offset))) {
                    end = offset;
                    break;
                }
            }
            int endAt = line.lookAt(end).offset();
            return switch (word) {
                case "radius" -> List.of(values.quantity(JmxValues.at(endAt)));
                case "size" -> {
                    int by = line.offsetOf(0, "x");
                    if (by < 0 || by >= end) {
                        throw reading.refusal(from, "a size is 'size width x height'");
                    }
                    double sizeWidth = values.quantity(JmxValues.words(Set.of("x")));
                    reading.cursor.next();
                    yield List.of(sizeWidth, values.quantity(JmxValues.at(endAt)));
                }
                default -> {
                    double[] point = point(endAt);
                    yield List.of(point[0], point[1]);
                }
            };
        }

        private void requireGeometry(Token first, String shape, String path, Map<String, List<Double>> geometry) {
            switch (shape) {
                case "path" -> {
                    if (path.isEmpty()) {
                        throw reading.refusal(first, "a path is 'path [name] \"SVG path data\"'");
                    }
                }
                case "box" -> {
                    if (!geometry.containsKey("at") || !geometry.containsKey("size")) {
                        throw reading.refusal(first, "a box is 'box [name] at x, y size width x height', placed by its centre");
                    }
                }
                case "glyph" -> {
                    if (!geometry.containsKey("at") || !geometry.containsKey("size")) {
                        throw reading.refusal(first, "a glyph is 'glyph [name] at x, y size width x height': a character cell, "
                                + "placed by its centre, whose character is its 'character' property");
                    }
                }
                case "bitmap" -> {
                    if (!geometry.containsKey("at") || !geometry.containsKey("size")) {
                        throw reading.refusal(first, "a bitmap is 'bitmap [name] at x, y size width x height': a screen of "
                                + "pixels, placed by its centre, whose 'pixels' is a picture input and whose 'columns' and "
                                + "'rows' say its matrix");
                    }
                }
                case "circle" -> {
                    if (!geometry.containsKey("at") || !geometry.containsKey("radius")) {
                        throw reading.refusal(first, "a circle is 'circle [name] at x, y radius r'");
                    }
                }
                case "line" -> {
                    if (!geometry.containsKey("from") || !geometry.containsKey("to")) {
                        throw reading.refusal(first, "a line is 'line [name] from x, y to x, y'");
                    }
                }
                default -> {
                }
            }
        }

        /** What an element's braces hold: its properties, the layers' {@code hide} lines, and a group's elements. */
        private final class Body {
            private final List<SymbolProperty> properties = new ArrayList<>();
            private final List<String> hidden = new ArrayList<>();
            private final List<SymbolElement> children = new ArrayList<>();
        }

        /** The lines between an element's braces; the cursor ends after the closing one. */
        private Body body() {
            Body body = new Body();
            reading.body("'{' to open the element",
                    () -> reading.refusal("an element is opened with '{' and never closed"),
                    () -> bodyLine(body));
            return body;
        }

        /** One line of an element's braces: a property, a {@code hide}, or an element inside a group. */
        private void bodyLine(Body body) {
            TokenCursor cursor = reading.cursor;
            TokenCursor line = reading.lineView();
            Token first = cursor.current();
            if (line.remaining() == 0) {
                throw reading.refusal("braces open only after an element: 'group lever { … }'");
            }
            int equals = line.offsetOf(0, "=");
            if (equals == 1 || (equals == 2 && MOTIONS.containsKey(line.valueAt(0)))) {
                SymbolMotion motion = equals == 1 ? SymbolMotion.STILL : MOTIONS.get(line.valueAt(0));
                String name = line.valueAt(equals - 1);
                if (body.properties.stream().anyMatch(property -> property.name().equals(name))) {
                    throw reading.refusalAhead(equals - 1, "'" + name + "' is written twice here");
                }
                cursor.shift(equals + 1);
                body.properties.add(property(first, name, motion));
            } else if (line.checkValueAt(0, "hide")) {
                if (line.remaining() < 2) {
                    throw reading.refusal(first, "'hide' names the elements it hides: 'hide halo'");
                }
                cursor.next();
                body.hidden.addAll(reading.names());
            } else {
                body.children.add(element());
            }
        }

        /** A property's values to the end of the line, as written and as jMF, split at the commas outside parentheses. */
        private SymbolProperty property(Token first, String name, SymbolMotion motion) {
            TokenCursor cursor = reading.cursor;
            if (reading.atLineEnd()) {
                throw reading.refusal(cursor.lookAt(-1), "'" + name + "' has no value");
            }
            int start = cursor.current().offset();
            List<String> formulas = new ArrayList<>();
            List<Token> segment = new ArrayList<>();
            int depth = 0;
            while (!reading.atLineEnd()) {
                Token token = cursor.next();
                if (token.type() == BasicToken.T_OPEN_PAREN || token.type() == BasicToken.T_OPEN_BRACKET) {
                    depth++;
                } else if (token.type() == BasicToken.T_CLOSE_PAREN || token.type() == BasicToken.T_CLOSE_BRACKET) {
                    depth--;
                } else if (depth == 0 && token.type() == BasicToken.T_COMMA) {
                    formulas.add(formula(segment, token));
                    segment = new ArrayList<>();
                    continue;
                }
                segment.add(token);
            }
            Token end = cursor.lookAt(0);
            formulas.add(formula(segment, end));
            return new SymbolProperty(name, reading.text(start, reading.cursor.endOfRead()), formulas, motion);
        }

        /**
         * {@code x, y} up to a place in the source — or to the line's end, for {@code -1} —, each a number with at most
         * its unit.
         */
        private double[] point(int end) {
            Token from = reading.cursor.current();
            TokenCursor line = reading.lineView();
            int limit = line.remaining();
            if (end >= 0) {
                for (int offset = 0; offset < line.remaining(); offset++) {
                    if (line.lookAt(offset).offset() >= end) {
                        limit = offset;
                        break;
                    }
                }
            }
            int commas = 0;
            int depth = 0;
            for (int offset = 0; offset < limit; offset++) {
                Token.Type type = line.lookAt(offset).type();
                if (type == BasicToken.T_OPEN_PAREN || type == BasicToken.T_OPEN_BRACKET) {
                    depth++;
                } else if (type == BasicToken.T_CLOSE_PAREN || type == BasicToken.T_CLOSE_BRACKET) {
                    depth--;
                } else if (depth == 0 && type == BasicToken.T_COMMA) {
                    commas++;
                }
            }
            if (commas != 1) {
                throw reading.refusal(from, "a point is 'x, y'");
            }
            int endAt = line.lookAt(limit).offset();
            double x = values.quantity(COMMA);
            reading.cursor.next();
            return new double[] {x, values.quantity(JmxValues.at(endAt))};
        }

        Block.Symbol symbol() {
            return new Block.Symbol(List.of(), Map.of(), Optional.empty(), Map.of(), width, height, paths, pins, labels,
                    indicators, upright, inputs, elements, Map.of());
        }
    }

    /**
     * A drawing's expression as jMF: every number followed by a unit in SI ({@code 60 ms} → {@code 0.06},
     * {@code -30 deg} → {@code -0.5236}) and {@code ->} — {@code curve(current, 0 mA -> 0, 20 mA -> 1)} — a comma.
     *
     * @param tokens the expression's tokens
     * @param after  the token after them, where the last one's written text ends
     */
    private String formula(List<Token> tokens, Token after) {
        StringBuilder formula = new StringBuilder();
        int index = 0;
        while (index < tokens.size()) {
            Token token = tokens.get(index);
            if (reading.isString(token)) {
                throw reading.refusal(token, "a drawing's value is a number, an input, a word such as 'burned' or an "
                        + "expression of them — not a text in quotes");
            }
            Optional<Double> number = Values.number(token.value());
            if (number.isPresent()) {
                int unitTokens = unitAfter(tokens, index + 1);
                if (unitTokens > 0) {
                    StringBuilder unit = new StringBuilder();
                    for (Token unitToken : tokens.subList(index + 1, index + 1 + unitTokens)) {
                        unit.append(reading.isString(unitToken) ? reading.literal(unitToken) : unitToken.value());
                    }
                    double inSi = Quantity.of(number.get(), unit.toString()).orElseThrow().value();
                    formula.append('(').append(new BigDecimal(inSi, SI_DIGITS).stripTrailingZeros().toPlainString()).append(") ");
                    index += 1 + unitTokens;
                    continue;
                }
            }
            if (token.value().equals("->")) {
                formula.append(", ");
            } else {
                Token next = index + 1 < tokens.size() ? tokens.get(index + 1) : after;
                formula.append(reading.text(token.offset(), next.offset()).text());
                if (!formula.isEmpty() && !Character.isWhitespace(formula.charAt(formula.length() - 1))) {
                    formula.append(' ');
                }
            }
            index++;
        }
        return formula.toString().strip();
    }

    /** How many tokens after a number make its unit, the longest reading first; 0 when none does. */
    private int unitAfter(List<Token> tokens, int from) {
        for (int count = Math.min(LONGEST_UNIT, tokens.size() - from); count >= 1; count--) {
            StringBuilder unit = new StringBuilder();
            for (Token token : tokens.subList(from, from + count)) {
                unit.append(reading.isString(token) ? reading.literal(token) : token.value());
            }
            if (Quantity.isUnit(unit.toString())) {
                return count;
            }
        }
        return 0;
    }

    Drawing drawing() {
        return new Drawing();
    }

    /** Whether the cursor sits on a drawing line of a {@code symbol} document. */
    boolean atDrawingLine() {
        return !reading.atEnd() && reading.cursor.current().type() != BasicToken.T_STRING
                && Drawing.opens(reading.cursor.current().value());
    }
}
