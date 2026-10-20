package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Capability;
import org.jmouse.parallax.script.Meta;
import org.jmouse.parallax.script.Meta.MetaValue;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** What describes and presents a document without changing what it computes: {@code meta} and {@code view}. */
final class PresentationReader {

    private final Reading reading;

    PresentationReader(Reading reading) {
        this.reading = reading;
    }

    // ─── meta ───────────────────────────────────────────────────────────────────────────────────────

    Meta meta() {
        reading.expect(BasicToken.T_OPEN_CURLY, "'{' to open meta");
        String summary = null;
        String description = null;
        String category = null;
        List<String> tags = List.of();
        String source = null;
        List<String> authors = List.of();
        String license = null;
        List<Meta.Link> links = new ArrayList<>();
        Map<String, MetaValue> fields = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        while (true) {
            reading.skipBlank();
            if (reading.atEnd()) {
                throw reading.refusal("'meta' is opened with '{' and never closed");
            }
            if (reading.cursor.consumeIf(BasicToken.T_CLOSE_CURLY)) {
                break;
            }
            Reading.Line line = reading.line();
            if (line.isEmpty()) {
                continue;
            }
            String key = line.value(0);
            switch (key) {
                case "summary" -> summary = text(line);
                case "description" -> description = text(line);
                case "category" -> category = word(line);
                case "tags" -> tags = line.names(1);
                case "source" -> source = text(line);
                case "authors" -> authors = texts(line);
                case "license" -> license = text(line);
                case "links" -> {
                    if (line.size() != 1) {
                        throw line.refusal("links are 'links { \"label\" \"https://…\" … }'");
                    }
                    links.addAll(links());
                }
                default -> {
                    if (line.size() < 2) {
                        throw line.refusal("a meta field is 'name value'");
                    }
                    if (fields.containsKey(key)) {
                        throw line.refusal("meta field '" + key + "' is written twice");
                    }
                    int valueEnd = line.size();
                    if (isLabelled(line)) {
                        valueEnd = line.size() - 1;
                        labels.put(key, reading.literal(line.get(valueEnd)));
                    }
                    fields.put(key, value(line, 1, valueEnd));
                }
            }
        }
        return new Meta(summary, description, category, tags, source, authors, license, links, fields, labels);
    }

    /**
     * Whether a free field ends with its label: {@code cut_off "f = 1 / (2π RC)" "Cut-off"} — a text in quotes after a
     * value, the way a parameter's description follows its value.
     */
    private static boolean isLabelled(Reading.Line line) {
        return line.size() >= 3 && line.isString(line.size() - 1) && !line.value(line.size() - 2).equals(",");
    }

    private String text(Reading.Line line) {
        if (line.size() != 2 || !line.isString(1)) {
            throw line.refusal("'" + line.value(0) + "' takes one text in quotes");
        }
        return reading.literal(line.get(1));
    }

    private static String word(Reading.Line line) {
        if (line.size() < 2) {
            throw line.refusal("'" + line.value(0) + "' takes a name");
        }
        return line.joinedWord(1);
    }

    private List<String> texts(Reading.Line line) {
        List<String> texts = new ArrayList<>();
        for (int index = 1; index < line.size(); index++) {
            if (line.isString(index)) {
                texts.add(reading.literal(line.get(index)));
            } else if (!line.value(index).equals(",")) {
                throw line.refusal(index, "'" + line.value(0) + "' takes texts in quotes, separated by commas");
            }
        }
        return texts;
    }

    private List<Meta.Link> links() {
        List<Meta.Link> links = new ArrayList<>();
        for (Reading.Line line : reading.blockLines("links")) {
            if (line.size() != 2 || !line.isString(0) || !line.isString(1)) {
                throw line.refusal("a link is '\"label\" \"https://…\"'");
            }
            String url = reading.literal(line.get(1));
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                throw line.refusal(1, "a link goes to an http or https address");
            }
            links.add(new Meta.Link(reading.literal(line.get(0)), url));
        }
        return links;
    }

    /** A free field's value, typed by how it is written. */
    private MetaValue value(Reading.Line line, int from, int to) {
        if (to - from == 1 && line.isString(from)) {
            return new MetaValue.Text(reading.literal(line.get(from)));
        }
        if (line.value(from).equals("[") && line.value(to - 1).equals("]")) {
            List<MetaValue> items = new ArrayList<>();
            int start = from + 1;
            for (int comma : line.commas(from + 1, to - 1)) {
                items.add(value(line, start, comma));
                start = comma + 1;
            }
            if (start < to - 1) {
                items.add(value(line, start, to - 1));
            }
            return new MetaValue.Items(items);
        }
        int dots = line.indexOf(from, "..");
        if (dots > from && dots < to - 1) {
            return new MetaValue.Span(quantity(line, from, dots), quantity(line, dots + 1, to));
        }
        Values.Written written = Values.written(line, from, to);
        if (written.quantity().isPresent()) {
            return new MetaValue.Amount(written.quantity().get());
        }
        if (!line.commas(from, to).isEmpty()) {
            return new MetaValue.Names(line.names(from, to));
        }
        return new MetaValue.Text(written.expression().text());
    }

    private static Quantity quantity(Reading.Line line, int from, int to) {
        return Values.written(line, from, to).quantity()
                .orElseThrow(() -> line.refusal(from, "a span is written with numbers: '4.5 V .. 16 V'"));
    }

    // ─── view ───────────────────────────────────────────────────────────────────────────────────────

    Block.View view() {
        reading.expect(BasicToken.T_OPEN_CURLY, "'{' to open view");
        List<Block.ViewItem> items = new ArrayList<>();
        while (true) {
            reading.skipBlank();
            if (reading.atEnd()) {
                throw reading.refusal("'view' is opened with '{' and never closed");
            }
            if (reading.cursor.consumeIf(BasicToken.T_CLOSE_CURLY)) {
                return new Block.View(items);
            }
            Reading.Line line = reading.line();
            if (!line.isEmpty()) {
                items.add(viewItem(line));
            }
        }
    }

    private Block.ViewItem viewItem(Reading.Line line) {
        String word = line.value(0);
        switch (word) {
            case "preset" -> {
                return preset(line);
            }
            case "camera" -> {
                return camera(line, 1);
            }
            case "explain" -> {
                if (line.size() != 3 || !line.isString(2)) {
                    throw line.refusal("an explanation is 'explain name \"text\"'");
                }
                return new Block.Explanation(line.value(1), reading.literal(line.get(2)));
            }
            case "without" -> {
                EnumSet<Capability> capabilities = EnumSet.noneOf(Capability.class);
                for (String name : line.names(1)) {
                    capabilities.add(Capability.ofKeyword(name).orElseThrow(() -> line.refusal("'" + name
                            + "' is not a capability: play, run, interact, resample, probe, sweep, check or execute")));
                }
                return new Block.Without(capabilities);
            }
            default -> {
                boolean primary = line.value(1).equals("primary");
                return new Block.Representation(word, primary, line.text(primary ? 2 : 1));
            }
        }
    }

    /**
     * {@code preset "Calm" ["a slow swell"] { A = 0.3, k = 0.5 }}; inside the braces, a line opening with
     * {@code camera} says where the preset looks from.
     */
    private Block.Preset preset(Reading.Line line) {
        boolean described = line.size() == 3 && line.isString(2);
        if ((line.size() != 2 && !described) || !line.isString(1)) {
            throw line.refusal("a preset is 'preset \"name\" [\"description\"] { parameter = value, … }'");
        }
        Map<String, SourceText> values = new LinkedHashMap<>();
        Block.Camera camera = null;
        for (Reading.Line inner : reading.blockLines("preset")) {
            if (inner.value(0).equals("camera") && !inner.isString(0)) {
                camera = camera(inner, 1);
            } else {
                values.putAll(assignments(inner, 0, "preset"));
            }
        }
        return new Block.Preset(reading.literal(line.get(1)), described ? reading.literal(line.get(2)) : "", values,
                Optional.ofNullable(camera));
    }

    /**
     * {@code camera azimuth 45 elevation 30 target [0, 0, 1]}: names, each followed by one value — a single token or
     * a bracketed list.
     *
     * @param firstSetting where the first setting's name is: after the word {@code camera}
     */
    static Block.Camera camera(Reading.Line line, int firstSetting) {
        Map<String, SourceText> settings = new LinkedHashMap<>();
        int index = firstSetting;
        while (index + 1 < line.size()) {
            int valueEnd = index + 2;
            if (line.value(index + 1).equals("[")) {
                valueEnd = closingBracket(line, index + 1) + 1;
            }
            settings.put(line.value(index), line.text(index + 1, valueEnd));
            index = valueEnd;
        }
        if (index < line.size()) {
            throw line.refusal(index, "a camera is 'camera name value [name value …]'");
        }
        return new Block.Camera(settings);
    }

    /** The index of the bracket that closes the one at {@code open}. */
    private static int closingBracket(Reading.Line line, int open) {
        int depth = 0;
        for (int index = open; index < line.size(); index++) {
            if (line.value(index).equals("[") && !line.isString(index)) {
                depth++;
            } else if (line.value(index).equals("]") && !line.isString(index) && --depth == 0) {
                return index;
            }
        }
        throw line.refusal(open, "'[' is never closed on this line");
    }

    /** {@code { A = 0.3, k = 0.5 }}: assignments separated by commas, {@code ;} or lines. */
    Map<String, SourceText> assignments(String block) {
        Map<String, SourceText> values = new LinkedHashMap<>();
        for (Reading.Line line : reading.blockLines(block)) {
            values.putAll(assignments(line, 0, block));
        }
        return values;
    }

    /**
     * {@code name = value} assignments separated by commas, from token {@code from} to the end of the line.
     *
     * @param block what holds them, named in a refusal
     */
    static Map<String, SourceText> assignments(Reading.Line line, int from, String block) {
        Map<String, SourceText> values = new LinkedHashMap<>();
        int start = from;
        List<Integer> commas = new ArrayList<>(line.commas(from, line.size()));
        commas.add(line.size());
        for (int comma : commas) {
            if (comma - start < 3 || !line.value(start + 1).equals("=")) {
                throw line.refusal(start, "'" + block + "' holds 'name = value' separated by commas");
            }
            values.put(line.value(start), line.text(start + 2, comma));
            start = comma + 1;
        }
        return values;
    }
}
