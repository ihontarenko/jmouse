package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.SymbolElement;
import org.jmouse.parallax.script.Block.SymbolInput;
import org.jmouse.parallax.script.Block.SymbolLabel;
import org.jmouse.parallax.script.Block.SymbolMotion;
import org.jmouse.parallax.script.Block.SymbolPin;
import org.jmouse.parallax.script.Block.SymbolProperty;
import org.jmouse.parallax.script.Quantity;
import org.jmouse.parallax.script.SourceText;

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
    private static final Set<String> ELEMENT_SHAPES = Set.of("path", "box", "circle", "line", "glyph", "group");

    /** The words a living drawing adds to {@link org.jmouse.parallax.script.Kind#DRAWING_LINES}. */
    private static final Set<String> LIVING_LINES = Set.of("input", "live", "box", "circle", "line", "glyph", "group");

    /** The words that place an element, each followed by its value. */
    private static final Set<String> GEOMETRY_WORDS = Set.of("at", "size", "radius", "from", "to", "pivot");

    /** The marks before a property. */
    private static final Map<String, SymbolMotion> MOTIONS = Map.of("live", SymbolMotion.LIVE, "animate", SymbolMotion.ANIMATED);

    /** How many significant digits a quantity keeps once it is in SI: enough for any written value, none of the binary noise. */
    private static final MathContext SI_DIGITS = new MathContext(12);

    /** The most tokens a unit after a number is read from inside a drawing's expression. */
    private static final int LONGEST_UNIT = 3;

    private final Reading reading;

    SymbolReader(Reading reading) {
        this.reading = reading;
    }

    /** The block after the word {@code symbol}. */
    Block.Symbol symbol() {
        Reading.Line header = reading.line();
        if (!header.isEmpty() && header.value(0).equals("uses")) {
            return reference(header);
        }
        if (!header.isEmpty()) {
            return shape(header);
        }
        Drawing drawing = new Drawing();
        reading.blockLines("symbol").forEach(drawing::read);
        return drawing.symbol();
    }

    /**
     * {@code uses resistor-symbol, fuse-symbol}, then in braces when there are any: port lines ({@code a terminal_one})
     * and input bindings ({@code closed = pressed}).
     */
    private Block.Symbol reference(Reading.Line header) {
        List<String> variants = variants(header);
        Map<String, String> ports = new LinkedHashMap<>();
        Map<String, SourceText> bindings = new LinkedHashMap<>();
        if (reading.cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            for (Reading.Line line : reading.blockLines("symbol uses")) {
                if (line.size() >= 2 && line.value(1).equals("=")) {
                    if (line.size() < 3) {
                        throw line.refusal(1, "an input binding is 'input = expression': 'closed = pressed'");
                    }
                    if (bindings.put(line.value(0), line.text(2, line.size())) != null) {
                        throw line.refusal("input '" + line.value(0) + "' is bound twice");
                    }
                    continue;
                }
                int portEnd = line.joinedEnd(0);
                if (portEnd >= line.size() || line.joinedEnd(portEnd) != line.size()) {
                    throw line.refusal("a line here is 'port pin' — the symbol's port, then the component's pin it is — "
                            + "or 'input = expression' — what one of the symbol's inputs is drawn from");
                }
                String port = line.joinedWord(0);
                if (ports.put(port, line.joinedWord(portEnd)) != null) {
                    throw line.refusal("port '" + port + "' is mapped twice");
                }
            }
        }
        return new Block.Symbol(variants, ports, Optional.empty(), Map.of(), 0, 0, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), bindings);
    }

    /** {@code uses a-symbol, b-symbol}: one slug between commas, none twice. */
    private static List<String> variants(Reading.Line header) {
        List<Integer> ends = new ArrayList<>(header.commas(1, header.size()));
        ends.add(header.size());
        List<String> variants = new ArrayList<>();
        int from = 1;
        for (int end : ends) {
            if (from >= end || header.joinedEnd(from) != end) {
                throw header.refusal(Math.min(from, header.size() - 1),
                        "a symbol reference is 'symbol uses slug[, slug…]': the symbols this part may be drawn with, "
                                + "the default first");
            }
            String slug = header.joinedWord(from);
            if (variants.contains(slug)) {
                throw header.refusal(from, "'" + slug + "' is named twice");
            }
            variants.add(slug);
            from = end + 1;
        }
        return variants;
    }

    /** {@code box { left d, clk edge;  right q }}: a shape and the pins on each of its sides. */
    private Block.Symbol shape(Reading.Line header) {
        String shape = header.joinedWord(0);
        if (!SHAPES.contains(shape)) {
            throw header.refusal("'" + shape + "' is not a shape: a part is drawn as 'symbol box { … }', "
                    + "'symbol triangle { … }', or from a symbol document with 'symbol uses <slug> { port pin … }'");
        }
        Map<String, List<String>> sides = new LinkedHashMap<>();
        for (Reading.Line line : reading.blockLines("symbol")) {
            List<String> words = new ArrayList<>(line.words(1, line.size()));
            sides.put(line.value(0), List.copyOf(splitByCommas(words)));
        }
        return new Block.Symbol(List.of(), Map.of(), Optional.of(shape), sides, 0, 0, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    /** {@code d, clk edge} → {@code ["d", "clk edge"]}: each pin with the words that follow it. */
    private static List<String> splitByCommas(List<String> words) {
        List<String> entries = new ArrayList<>();
        StringBuilder entry = new StringBuilder();
        for (String word : words) {
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
        return entries;
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
            return org.jmouse.parallax.script.Kind.DRAWING_LINES.contains(word) || LIVING_LINES.contains(word);
        }

        void read(Reading.Line line) {
            switch (line.value(0)) {
                case "size" -> {
                    int by = line.indexOf(1, "x");
                    if (by < 0) {
                        throw line.refusal("a size is 'size width x height'");
                    }
                    width = Values.quantity(line, 1, by);
                    height = Values.quantity(line, by + 1, line.size());
                }
                case "path" -> {
                    boolean plain = line.size() == 2 && line.isString(1);
                    boolean standing = line.size() == 3 && line.isString(1) && "upright".equals(line.value(2));
                    if ((plain || standing) && !reading.cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
                        (standing ? upright : paths).add(reading.literal(line.get(1)));
                    } else {
                        elements.add(element(line));
                    }
                }
                case "pin" -> {
                    int at = line.indexOf(2, "at");
                    if (at != 2) {
                        throw line.refusal("a symbol pin is 'pin name at x, y [\"label\"]'");
                    }
                    if (pins.stream().anyMatch(pin -> pin.pin().equals(line.value(1)))) {
                        throw line.refusal("pin '" + line.value(1) + "' is drawn twice");
                    }
                    int labelAt = line.isString(line.size() - 1) ? line.size() - 1 : line.size();
                    double[] point = point(line, at + 1, labelAt);
                    String label = labelAt < line.size() ? reading.literal(line.get(labelAt)) : "";
                    pins.add(new SymbolPin(line.value(1), point[0], point[1], label));
                }
                case "label" -> {
                    int at = line.indexOf(2, "at");
                    if (at != 2 || !LABEL_SUBJECTS.contains(line.value(1))) {
                        throw line.refusal("a label is 'label name at x, y' or 'label value at x, y'");
                    }
                    double[] point = point(line, at + 1, line.size());
                    labels.add(new SymbolLabel(line.value(1), point[0], point[1]));
                }
                case "indicator" -> {
                    int from = line.indexOf(2, "from");
                    int to = line.indexOf(from + 1, "to");
                    if (from != 2 || to < 0) {
                        throw line.refusal("an indicator is 'indicator name from x, y to x, y'");
                    }
                    double[] first = point(line, from + 1, to);
                    double[] second = point(line, to + 1, line.size());
                    indicators.add(new Block.SymbolIndicator(line.value(1), Math.min(first[0], second[0]), Math.min(first[1], second[1]),
                            Math.max(first[0], second[0]), Math.max(first[1], second[1])));
                }
                case "input" -> inputs.add(input(line));
                case "live", "box", "circle", "line", "glyph", "group" -> elements.add(element(line));
                default -> throw line.refusal("'" + line.value(0) + "' is not part of a drawing: size, path, box, circle, "
                        + "line, glyph, group, live, input, pin, label or indicator");
            }
        }

        /** {@code input closed}, {@code input current 0 .. 20 mA}. */
        private SymbolInput input(Reading.Line line) {
            if (line.size() < 2 || line.isString(1)) {
                throw line.refusal("an input is 'input name [low .. high]': 'input closed', 'input current 0 .. 20 mA'");
            }
            String name = line.value(1);
            if (inputs.stream().anyMatch(input -> input.name().equals(name))) {
                throw line.refusal(1, "input '" + name + "' is declared twice");
            }
            if (line.size() == 2) {
                return new SymbolInput(name, 0, 1, Optional.empty());
            }
            int dots = line.indexOf(2, "..");
            if (dots < 0) {
                throw line.refusal(2, "an input's range is 'low .. high': 'input current 0 .. 20 mA'");
            }
            double low = Values.quantity(line, 2, dots);
            double high = Values.quantity(line, dots + 1, line.size());
            if (!(high > low)) {
                throw line.refusal(2, "an input's range runs from a lower to a higher value");
            }
            return new SymbolInput(name, low, high, Optional.of(line.text(2, line.size())));
        }

        /**
         * One element, from its line to the end of its braces when it has any:
         * {@code [live] shape [name] [at x, y] [size w x h] [radius r] [from x, y] [to x, y] [pivot x, y] ["path data"]}
         * or {@code live when <condition>}.
         */
        private SymbolElement element(Reading.Line line) {
            boolean live = line.value(0).equals("live");
            int position = live ? 1 : 0;
            boolean opened = reading.cursor.isCurrent(BasicToken.T_OPEN_CURLY);
            String shape = line.value(position);
            if (shape.equals("when")) {
                return layer(line, live, position, opened);
            }
            if (!ELEMENT_SHAPES.contains(shape)) {
                throw line.refusal(position, "'" + shape + "' is not an element: path, box, circle, line, glyph, group or 'live when'");
            }
            return shaped(line, live, shape, position + 1, opened);
        }

        /** {@code live when <condition> { … }}: a layer drawn while its condition holds. */
        private SymbolElement layer(Reading.Line line, boolean live, int position, boolean opened) {
            if (!live) {
                throw line.refusal(position, "a layer shown on a condition is always 'live when <condition> { … }'");
            }
            if (line.size() <= position + 1 || !opened) {
                throw line.refusal(position, "a layer is 'live when <condition> { … }': the condition, then its elements in braces");
            }
            SymbolProperty condition = property(line, "when", position + 1, line.size(), SymbolMotion.LIVE);
            Body body = body();
            return new SymbolElement("when", "", true, "", Map.of(), condition, body.properties, body.hidden,
                    body.children, line.first().lineNumber());
        }

        /** A shape or a group, placed by the words after it, with what its braces hold. */
        private SymbolElement shaped(Reading.Line line, boolean live, String shape, int from, boolean opened) {
            Placing placing = placing(line, shape, from);
            Body body = opened ? body() : new Body();
            if (!shape.equals("group") && !body.children.isEmpty()) {
                throw line.refusal("only a group or a 'live when' holds other elements");
            }
            if (!body.hidden.isEmpty()) {
                throw line.refusal("'hide' stands only in a 'live when' layer");
            }
            if (shape.equals("group") && !opened) {
                throw line.refusal("a group holds its elements in braces: 'group lever pivot -13, 0 { … }'");
            }
            return new SymbolElement(shape, placing.name(), live, placing.path(), placing.geometry(), null, body.properties,
                    List.of(), body.children, line.first().lineNumber());
        }

        /** What an element's line says after its shape: its name, a path's data, and where it stands. */
        private record Placing(String name, String path, Map<String, List<Double>> geometry) {
        }

        private Placing placing(Reading.Line line, String shape, int from) {
            int position = from;
            String name = "";
            if (position < line.size() && !line.isString(position) && !GEOMETRY_WORDS.contains(line.value(position))) {
                name = line.value(position);
                position++;
            }
            String path = "";
            Map<String, List<Double>> geometry = new LinkedHashMap<>();
            while (position < line.size()) {
                if (line.isString(position)) {
                    if (!shape.equals("path") || !path.isEmpty()) {
                        throw line.refusal(position, "only a path carries path data, once");
                    }
                    path = reading.literal(line.get(position));
                    position++;
                    continue;
                }
                String word = line.value(position);
                if (!GEOMETRY_WORDS.contains(word) || geometry.containsKey(word)) {
                    throw line.refusal(position, "'" + word + "' does not place a " + shape
                            + ": it is placed with at, size, radius, from, to and pivot, each once");
                }
                int end = nextGeometryWord(line, position + 1);
                geometry.put(word, geometryValue(line, word, position + 1, end));
                position = end;
            }
            requireGeometry(line, shape, path, geometry);
            return new Placing(name, path, geometry);
        }

        private static int nextGeometryWord(Reading.Line line, int from) {
            for (int index = from; index < line.size(); index++) {
                if (line.isString(index) || GEOMETRY_WORDS.contains(line.value(index))) {
                    return index;
                }
            }
            return line.size();
        }

        /** {@code at x, y}, {@code size w x h}, {@code radius r}. */
        private List<Double> geometryValue(Reading.Line line, String word, int from, int to) {
            return switch (word) {
                case "radius" -> List.of(Values.quantity(line, from, to));
                case "size" -> {
                    int by = line.indexOf(from, "x");
                    if (by < 0 || by >= to) {
                        throw line.refusal(from, "a size is 'size width x height'");
                    }
                    yield List.of(Values.quantity(line, from, by), Values.quantity(line, by + 1, to));
                }
                default -> {
                    double[] point = point(line, from, to);
                    yield List.of(point[0], point[1]);
                }
            };
        }

        private static void requireGeometry(Reading.Line line, String shape, String path, Map<String, List<Double>> geometry) {
            switch (shape) {
                case "path" -> {
                    if (path.isEmpty()) {
                        throw line.refusal("a path is 'path [name] \"SVG path data\"'");
                    }
                }
                case "box" -> {
                    if (!geometry.containsKey("at") || !geometry.containsKey("size")) {
                        throw line.refusal("a box is 'box [name] at x, y size width x height', placed by its centre");
                    }
                }
                case "glyph" -> {
                    if (!geometry.containsKey("at") || !geometry.containsKey("size")) {
                        throw line.refusal("a glyph is 'glyph [name] at x, y size width x height': a character cell, placed by its centre, "
                                + "whose character is its 'character' property");
                    }
                }
                case "circle" -> {
                    if (!geometry.containsKey("at") || !geometry.containsKey("radius")) {
                        throw line.refusal("a circle is 'circle [name] at x, y radius r'");
                    }
                }
                case "line" -> {
                    if (!geometry.containsKey("from") || !geometry.containsKey("to")) {
                        throw line.refusal("a line is 'line [name] from x, y to x, y'");
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
            reading.expect(BasicToken.T_OPEN_CURLY, "'{' to open the element");
            Body body = new Body();
            while (true) {
                reading.skipBlank();
                if (reading.atEnd()) {
                    throw reading.refusal("an element is opened with '{' and never closed");
                }
                if (reading.cursor.consumeIf(BasicToken.T_CLOSE_CURLY)) {
                    return body;
                }
                Reading.Line line = reading.line();
                if (line.isEmpty()) {
                    throw reading.refusal("braces open only after an element: 'group lever { … }'");
                }
                int equals = line.indexOf(0, "=");
                if (equals == 1 || (equals == 2 && MOTIONS.containsKey(line.value(0)))) {
                    SymbolMotion motion = equals == 1 ? SymbolMotion.STILL : MOTIONS.get(line.value(0));
                    String name = line.value(equals - 1);
                    if (body.properties.stream().anyMatch(property -> property.name().equals(name))) {
                        throw line.refusal(equals - 1, "'" + name + "' is written twice here");
                    }
                    body.properties.add(property(line, name, equals + 1, line.size(), motion));
                } else if (line.value(0).equals("hide")) {
                    if (line.size() < 2) {
                        throw line.refusal("'hide' names the elements it hides: 'hide halo'");
                    }
                    line.names(1).forEach(body.hidden::add);
                } else {
                    body.children.add(element(line));
                }
            }
        }

        /** A property's values, as written and as jMF, split at the commas outside parentheses. */
        private SymbolProperty property(Reading.Line line, String name, int from, int to, SymbolMotion motion) {
            if (from >= to) {
                throw line.refusal(Math.max(0, from - 1), "'" + name + "' has no value");
            }
            List<String> formulas = new ArrayList<>();
            int start = from;
            for (int comma : line.commas(from, to)) {
                formulas.add(formula(line, start, comma));
                start = comma + 1;
            }
            formulas.add(formula(line, start, to));
            return new SymbolProperty(name, line.text(from, to), formulas, motion);
        }

        private double[] point(Reading.Line line, int from, int to) {
            List<Integer> commas = line.commas(from, to);
            if (commas.size() != 1) {
                throw line.refusal(from, "a point is 'x, y'");
            }
            return new double[] {Values.quantity(line, from, commas.getFirst()), Values.quantity(line, commas.getFirst() + 1, to)};
        }

        Block.Symbol symbol() {
            return new Block.Symbol(List.of(), Map.of(), Optional.empty(), Map.of(), width, height, paths, pins, labels,
                    indicators, upright, inputs, elements, Map.of());
        }
    }

    /**
     * A drawing's expression as jMF: every number followed by a unit in SI ({@code 60 ms} → {@code 0.06},
     * {@code -30 deg} → {@code -0.5236}) and {@code ->} — {@code curve(current, 0 mA -> 0, 20 mA -> 1)} — a comma.
     */
    private String formula(Reading.Line line, int from, int to) {
        StringBuilder formula = new StringBuilder();
        int index = from;
        while (index < to) {
            if (line.isString(index)) {
                throw line.refusal(index, "a drawing's value is a number, an input, a word such as 'burned' or an "
                        + "expression of them — not a text in quotes");
            }
            String value = line.value(index);
            Optional<Double> number = Values.number(value);
            if (number.isPresent()) {
                int unitTokens = unitAfter(line, index + 1, to);
                if (unitTokens > 0) {
                    String unit = String.join("", line.words(index + 1, index + 1 + unitTokens));
                    double inSi = Quantity.of(number.get(), unit).orElseThrow().value();
                    formula.append('(').append(new BigDecimal(inSi, SI_DIGITS).stripTrailingZeros().toPlainString()).append(") ");
                    index += 1 + unitTokens;
                    continue;
                }
            }
            if (value.equals("->")) {
                formula.append(", ");
            } else {
                formula.append(line.text(index, index + 1).text());
                if (!formula.isEmpty() && !Character.isWhitespace(formula.charAt(formula.length() - 1))) {
                    formula.append(' ');
                }
            }
            index++;
        }
        return formula.toString().strip();
    }

    /** How many tokens after a number make its unit, the longest reading first; 0 when none does. */
    private static int unitAfter(Reading.Line line, int from, int to) {
        for (int tokens = Math.min(LONGEST_UNIT, to - from); tokens >= 1; tokens--) {
            if (Quantity.isUnit(String.join("", line.words(from, from + tokens)))) {
                return tokens;
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
