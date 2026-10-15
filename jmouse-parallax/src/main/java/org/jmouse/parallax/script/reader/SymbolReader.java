package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.SymbolLabel;
import org.jmouse.parallax.script.Block.SymbolPin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 * symbol's port to the component's pin; a port they leave out is the pin of the same name.
 */
final class SymbolReader {

    /** The shapes drawn from their sides alone. */
    private static final List<String> SHAPES = List.of("box", "triangle");

    /** What a drawing's {@code label} lines place. */
    private static final List<String> LABEL_SUBJECTS = List.of("name", "value");

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

    /** {@code uses resistor-symbol, fuse-symbol}, then the port map in braces when there is one. */
    private Block.Symbol reference(Reading.Line header) {
        List<String> variants = variants(header);
        Map<String, String> ports = new LinkedHashMap<>();
        if (reading.cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            for (Reading.Line line : reading.blockLines("symbol uses")) {
                int portEnd = line.joinedEnd(0);
                if (portEnd >= line.size() || line.joinedEnd(portEnd) != line.size()) {
                    throw line.refusal("a port line is 'port pin': the symbol's port, then the component's pin it is");
                }
                String port = line.joinedWord(0);
                if (ports.put(port, line.joinedWord(portEnd)) != null) {
                    throw line.refusal("port '" + port + "' is mapped twice");
                }
            }
        }
        return new Block.Symbol(variants, ports, Optional.empty(), Map.of(), 0, 0, List.of(), List.of(), List.of(), List.of(), List.of());
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
        return new Block.Symbol(List.of(), Map.of(), Optional.of(shape), sides, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of());
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

        /** Whether a word opens a drawing line. */
        static boolean opens(String word) {
            return org.jmouse.parallax.script.Kind.DRAWING_LINES.contains(word);
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
                    boolean standing = line.size() == 3 && "upright".equals(line.value(2));
                    if ((line.size() != 2 && !standing) || !line.isString(1)) {
                        throw line.refusal("a path is 'path \"SVG path data\"', or 'path \"…\" upright' for one that stays upright as the part turns");
                    }
                    (standing ? upright : paths).add(reading.literal(line.get(1)));
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
                default -> throw line.refusal("'" + line.value(0) + "' is not part of a drawing: size, path, pin, label or indicator");
            }
        }

        private double[] point(Reading.Line line, int from, int to) {
            List<Integer> commas = line.commas(from, to);
            if (commas.size() != 1) {
                throw line.refusal(from, "a point is 'x, y'");
            }
            return new double[] {Values.quantity(line, from, commas.getFirst()), Values.quantity(line, commas.getFirst() + 1, to)};
        }

        Block.Symbol symbol() {
            return new Block.Symbol(List.of(), Map.of(), Optional.empty(), Map.of(), width, height, paths, pins, labels, indicators, upright);
        }
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
