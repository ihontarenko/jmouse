package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor.Boundary;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
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

/**
 * What describes and presents a document without changing what it computes: {@code meta} and {@code view} — and the
 * assignments and camera a preset or a lesson's step writes, read here for both.
 */
final class PresentationReader {

    /** A preset's or a step's assignment ends at its comma. */
    private static final Boundary COMMA = (token, after, depth) -> depth == 0 && token.type() == BasicToken.T_COMMA;

    private final Reading reading;
    private final JmxValues values;

    PresentationReader(Reading reading) {
        this.reading = reading;
        this.values = new JmxValues(reading);
    }

    // ─── meta ───────────────────────────────────────────────────────────────────────────────────────

    Meta meta() {
        MetaFields fields = new MetaFields();
        reading.body("meta", () -> {
            if (reading.cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
                throw reading.refusal("'meta' holds lines, not a nested '{ … }'");
            }
            fields.read();
        });
        return fields.meta();
    }

    /** What a {@code meta} block says, gathered line by line. */
    private final class MetaFields {
        String summary;
        String description;
        String category;
        List<String> tags = List.of();
        String source;
        List<String> authors = List.of();
        String license;
        final List<Meta.Link> links = new ArrayList<>();
        final Map<String, MetaValue> fields = new LinkedHashMap<>();
        final Map<String, String> labels = new LinkedHashMap<>();

        void read() {
            TokenCursor line = reading.lineView();
            Token first = reading.cursor.current();
            String key = line.valueAt(0);
            switch (key) {
                case "summary" -> summary = text(first);
                case "description" -> description = text(first);
                case "category" -> category = word(first);
                case "tags" -> tags = namesAfterKey();
                case "source" -> source = text(first);
                case "authors" -> authors = texts();
                case "license" -> license = text(first);
                case "links" -> {
                    if (line.remaining() != 1) {
                        throw reading.refusal(first, "links are 'links { \"label\" \"https://…\" … }'");
                    }
                    reading.cursor.next();
                    links.addAll(links());
                }
                default -> {
                    int length = line.remaining();
                    if (length < 2) {
                        throw reading.refusal(first, "a meta field is 'name value'");
                    }
                    if (fields.containsKey(key)) {
                        throw reading.refusal(first, "meta field '" + key + "' is written twice");
                    }
                    int valueEnd = length;
                    // A free field may end with its label: cut_off "f = 1 / (2π RC)" "Cut-off" — a text in quotes after
                    // a value, the way a parameter's description follows its value.
                    if (length >= 3 && reading.isString(line.lookAt(length - 1)) && !line.checkValueAt(length - 2, ",")) {
                        valueEnd = length - 1;
                        labels.put(key, reading.literal(line.lookAt(valueEnd)));
                    }
                    reading.cursor.next();
                    fields.put(key, value(valueEnd - 2));
                    reading.skipLine();
                }
            }
        }

        Meta meta() {
            return new Meta(summary, description, category, tags, source, authors, license, links, fields, labels);
        }
    }

    /** {@code key "text"}: exactly one text in quotes. */
    private String text(Token first) {
        TokenCursor line = reading.lineView();
        if (line.remaining() != 2 || !reading.isString(line.lookAt(1))) {
            throw reading.refusal(first, "'" + line.valueAt(0) + "' takes one text in quotes");
        }
        String text = reading.literal(line.lookAt(1));
        reading.skipLine();
        return text;
    }

    /** {@code key name}: one word, hyphens and all. */
    private String word(Token first) {
        TokenCursor line = reading.lineView();
        if (line.remaining() < 2) {
            throw reading.refusal(first, "'" + line.valueAt(0) + "' takes a name");
        }
        reading.cursor.next();
        String word = reading.joinedWord();
        reading.skipLine();
        return word;
    }

    /** {@code key a, b, c}: names after the key, each one word. */
    private List<String> namesAfterKey() {
        reading.cursor.next();
        return reading.names();
    }

    /** {@code key "one", "other"}: texts in quotes, separated by commas. */
    private List<String> texts() {
        TokenCursor cursor = reading.cursor;
        String key = cursor.next().value();
        List<String> texts = new ArrayList<>();
        while (!reading.atLineEnd()) {
            Token token = cursor.next();
            if (reading.isString(token)) {
                texts.add(reading.literal(token));
            } else if (!token.value().equals(",")) {
                throw reading.refusal(token, "'" + key + "' takes texts in quotes, separated by commas");
            }
        }
        return texts;
    }

    /** {@code links { "label" "https://…" … }}. */
    private List<Meta.Link> links() {
        List<Meta.Link> links = new ArrayList<>();
        List<TokenCursor.Savepoint> starts = reading.lineStarts("links");
        TokenCursor.Savepoint after = reading.cursor.savepoint();
        for (TokenCursor.Savepoint start : starts) {
            reading.cursor.restore(start);
            TokenCursor line = reading.lineView();
            Token first = reading.cursor.current();
            if (line.remaining() != 2 || !reading.isString(line.lookAt(0)) || !reading.isString(line.lookAt(1))) {
                throw reading.refusal(first, "a link is '\"label\" \"https://…\"'");
            }
            String url = reading.literal(line.lookAt(1));
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                throw reading.refusalAhead(1, "a link goes to an http or https address");
            }
            links.add(new Meta.Link(reading.literal(line.lookAt(0)), url));
        }
        reading.cursor.restore(after);
        return links;
    }

    /**
     * A free field's value over the tokens up to {@code last} ahead, inclusive, typed by how it is written: a text, a
     * list in brackets, a span of quantities, a quantity, names, or else the words as written. It is data, not a formula:
     * no expression is read here.
     */
    private MetaValue value(int last) {
        TokenCursor line = reading.lineView();
        int to = last + 1;
        if (to == 1 && reading.isString(line.lookAt(0))) {
            return new MetaValue.Text(reading.literal(line.lookAt(0)));
        }
        if (line.checkValueAt(0, "[") && line.checkValueAt(to - 1, "]")) {
            reading.cursor.next();
            List<MetaValue> items = new ArrayList<>();
            int start = 0;
            TokenCursor inner = reading.lineView();
            int end = to - 2;
            int comma = inner.offsetOf(start, ",");
            while (comma >= 0 && comma < end) {
                items.add(itemAt(start, comma));
                start = comma + 1;
                comma = inner.offsetOf(start, ",");
            }
            if (start < end) {
                items.add(itemAt(start, end));
            }
            return new MetaValue.Items(items);
        }
        // Where things stand is taken now: a view looks ahead of where the cursor is, and the cursor is about to move.
        Token first = line.lookAt(0);
        int end = line.lookAt(to).offset();
        int dots = line.offsetOf(0, "..");
        if (dots > 0 && dots < to - 1) {
            int dotsAt = line.lookAt(dots).offset();
            Token maximum = line.lookAt(dots + 1);
            Quantity minimumBound = spanBound(first, dotsAt);
            reading.cursor.next();
            return new MetaValue.Span(minimumBound, spanBound(maximum, end));
        }
        TokenCursor.Savepoint start = reading.cursor.savepoint();
        Optional<Quantity> quantity = values.literal(JmxValues.at(end));
        if (quantity.isPresent()) {
            return new MetaValue.Amount(quantity.get());
        }
        reading.cursor.restore(start);
        int comma = line.offsetOf(0, ",");
        if (comma >= 0 && comma < to) {
            return new MetaValue.Names(reading.namesBefore(end));
        }
        return new MetaValue.Text(reading.text(first.offset(), end).text());
    }

    /** One item of a listed value, over the tokens {@code [from, to)} ahead of where the list's first item stands. */
    private MetaValue itemAt(int from, int to) {
        TokenCursor.Savepoint start = reading.cursor.savepoint();
        reading.cursor.shift(from);
        MetaValue item = value(to - from - 1);
        reading.cursor.restore(start);
        return item;
    }

    /** One bound of a span, from a token to a place in the source: a quantity, or the span is refused. */
    private Quantity spanBound(Token from, int end) {
        return values.literal(JmxValues.at(end))
                .orElseThrow(() -> reading.refusal(from, "a span is written with numbers: '4.5 V .. 16 V'"));
    }

    // ─── view ───────────────────────────────────────────────────────────────────────────────────────

    Block.View view() {
        List<Block.ViewItem> items = new ArrayList<>();
        reading.body("view", () -> {
            if (reading.cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
                throw reading.refusal("'view' holds lines, not a nested '{ … }'");
            }
            items.add(viewItem());
        });
        return new Block.View(items);
    }

    private Block.ViewItem viewItem() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        String word = line.valueAt(0);
        switch (word) {
            case "preset" -> {
                return preset();
            }
            case "camera" -> {
                cursor.next();
                return camera();
            }
            case "explain" -> {
                if (line.remaining() != 3 || !reading.isString(line.lookAt(2))) {
                    throw reading.refusal(first, "an explanation is 'explain name \"text\"'");
                }
                Block.Explanation explanation = new Block.Explanation(line.valueAt(1), reading.literal(line.lookAt(2)));
                reading.skipLine();
                return explanation;
            }
            case "without" -> {
                cursor.next();
                EnumSet<Capability> capabilities = EnumSet.noneOf(Capability.class);
                for (String name : reading.names()) {
                    capabilities.add(Capability.ofKeyword(name).orElseThrow(() -> reading.refusal(first, "'" + name
                            + "' is not a capability: play, run, interact, resample, probe, sweep, check or execute")));
                }
                return new Block.Without(capabilities);
            }
            default -> {
                boolean primary = line.checkValueAt(1, "primary");
                int settings = primary ? 2 : 1;
                SourceText written = reading.text(line.lookAt(settings).offset(), line.lookAt(line.remaining()).offset());
                reading.skipLine();
                return new Block.Representation(word, primary, written);
            }
        }
    }

    /**
     * {@code preset "Calm" ["a slow swell"] { A = 0.3, k = 0.5 }}; inside the braces, a line opening with
     * {@code camera} says where the preset looks from.
     */
    private Block.Preset preset() {
        TokenCursor line = reading.lineView();
        Token first = reading.cursor.current();
        int length = line.remaining();
        boolean described = length == 3 && reading.isString(line.lookAt(2));
        if ((length != 2 && !described) || !reading.isString(line.lookAt(1))) {
            throw reading.refusal(first, "a preset is 'preset \"name\" [\"description\"] { parameter = value, … }'");
        }
        String name = reading.literal(line.lookAt(1));
        String description = described ? reading.literal(line.lookAt(2)) : "";
        reading.skipLine();
        Map<String, SourceText> assigned = new LinkedHashMap<>();
        Block.Camera camera = null;
        List<TokenCursor.Savepoint> starts = reading.lineStarts("preset");
        TokenCursor.Savepoint after = reading.cursor.savepoint();
        for (TokenCursor.Savepoint start : starts) {
            reading.cursor.restore(start);
            if (reading.cursor.checkValueAt(0, "camera")) {
                reading.cursor.next();
                camera = camera();
            } else {
                assigned.putAll(assignmentsOnLine("preset"));
            }
        }
        reading.cursor.restore(after);
        return new Block.Preset(name, description, assigned, Optional.ofNullable(camera));
    }

    /**
     * {@code azimuth 45 elevation 30 target [0, 0, 1]} after {@code camera}, to the end of the line: names, each followed
     * by one value — a single token or a bracketed list —, as written.
     */
    Block.Camera camera() {
        TokenCursor cursor = reading.cursor;
        Map<String, SourceText> settings = new LinkedHashMap<>();
        while (reading.lineView().remaining() >= 2) {
            String name = cursor.next().value();
            Token value = cursor.current();
            if (cursor.checkValueAt(0, "[")) {
                closeBracket(value);
            } else {
                cursor.next();
            }
            settings.put(name, reading.text(value.offset(), cursor.endOfRead()));
        }
        if (!reading.atLineEnd()) {
            throw reading.refusal(cursor.current(), "a camera is 'camera name value [name value …]'");
        }
        return new Block.Camera(settings);
    }

    /** Steps over a bracketed list, nested ones included; refused when it is never closed on the line. */
    private void closeBracket(Token open) {
        TokenCursor cursor = reading.cursor;
        int depth = 0;
        while (!reading.atLineEnd()) {
            Token token = cursor.next();
            if (token.value().equals("[") && !reading.isString(token)) {
                depth++;
            } else if (token.value().equals("]") && !reading.isString(token) && --depth == 0) {
                return;
            }
        }
        throw reading.refusal(open, "'[' is never closed on this line");
    }

    /** {@code { A = 0.3, k = 0.5 }}: assignments separated by commas, {@code ;} or lines. */
    Map<String, SourceText> assignments(String block) {
        Map<String, SourceText> assigned = new LinkedHashMap<>();
        List<TokenCursor.Savepoint> starts = reading.lineStarts(block);
        TokenCursor.Savepoint after = reading.cursor.savepoint();
        for (TokenCursor.Savepoint start : starts) {
            reading.cursor.restore(start);
            assigned.putAll(assignmentsOnLine(block));
        }
        reading.cursor.restore(after);
        return assigned;
    }

    /**
     * {@code name = value} assignments separated by commas, from the cursor to the end of the line; each value an
     * expression.
     *
     * @param block what holds them, named in a refusal
     */
    Map<String, SourceText> assignmentsOnLine(String block) {
        TokenCursor cursor = reading.cursor;
        Map<String, SourceText> assigned = new LinkedHashMap<>();
        while (true) {
            TokenCursor line = reading.lineView();
            int comma = line.offsetOf(0, ",");
            int end = comma < 0 ? line.remaining() : comma;
            if (end < 3 || !line.checkValueAt(1, "=")) {
                throw reading.refusal(cursor.current(), "'" + block + "' holds 'name = value' separated by commas");
            }
            String name = cursor.next().value();
            cursor.next();
            assigned.put(name, values.expression(COMMA));
            if (reading.atLineEnd()) {
                return assigned;
            }
            cursor.ensure(BasicToken.T_COMMA);
        }
    }
}
