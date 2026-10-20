package org.jmouse.datamap;

import org.jmouse.el.language.line.LineReader;
import org.jmouse.el.language.line.LineSyntaxException;
import org.jmouse.el.language.line.TokenLine;
import org.jmouse.el.lexer.BasicToken;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads a jMD document.
 *
 * <pre>
 * map "Title" as slug {
 *     name value [source word named …]
 *     id ["title"] [source word named …] {
 *         …
 *     }
 * }
 * </pre>
 *
 * <p>A value is a text in quotes; a number with, at most, its unit ({@code 2.2 µs}, {@code -40 °C}); a bare word; a
 * link to another document's entry or field ({@code 74hc595.jmd#supply-voltage/maximum}); a range
 * ({@code 2 V .. 6 V}); a name given a value ({@code 14 = data}); or several of those separated by commas.
 * {@code #} standing apart begins a comment.</p>
 */
public final class DataMapReader {

    private static final String MAP = "map";
    private static final String AS = "as";
    private static final String SOURCE = "source";
    private static final String RANGE = "..";
    private static final String ASSIGN = "=";
    private static final String COMMA = ",";
    private static final char LINK = '#';
    private static final char FIELD_OF_LINK = '/';

    private final LineReader reader;
    private final Set<String> ids = new HashSet<>();

    private DataMapReader(String document, String text) {
        this.reader = new LineReader(document, text);
    }

    /**
     * @param document its name, for a refusal to say which document it is about
     * @throws LineSyntaxException for a document that does not read, saying where
     */
    public static DataMap read(String document, String text) {
        return new DataMapReader(document, text).dataMap();
    }

    private DataMap dataMap() {
        reader.skipBlank();
        TokenLine header = reader.line();
        if (header.isEmpty() || !header.joinedWord(0).equals(MAP)) {
            throw header.refusal("a jMD document begins with: map \"Title\" as slug {");
        }
        int title = header.joinedEnd(0);
        if (!header.isString(title)) {
            throw header.refusal(title, "the map's title is a text in quotes");
        }
        int as = title + 1;
        if (!header.value(as).equals(AS) || as + 1 >= header.size()) {
            throw header.refusal(as, "after the title comes: as slug");
        }
        String slug = header.joinedWord(as + 1);
        if (header.joinedEnd(as + 1) != header.size()) {
            throw header.refusal(header.joinedEnd(as + 1), "the header ends with the slug, before '{'");
        }
        Body body = body();
        reader.skipBlank();
        if (!reader.atEnd()) {
            throw reader.refusal("a document is one map: nothing follows its closing '}'");
        }
        return new DataMap(slug, header.literal(title), body.fields(), body.entries());
    }

    private record Body(List<DataMapField> fields, List<DataMapEntry> entries) {
    }

    /** A block from its {@code {} to its {@code }}, both consumed. */
    private Body body() {
        reader.expect(BasicToken.T_OPEN_CURLY, "'{'");
        List<DataMapField> fields = new ArrayList<>();
        List<DataMapEntry> entries = new ArrayList<>();
        Set<String> names = new HashSet<>();
        while (true) {
            reader.skipBlank();
            if (reader.atEnd()) {
                throw reader.refusal("a block is not closed: '}' is missing");
            }
            if (reader.at(BasicToken.T_CLOSE_CURLY)) {
                reader.expect(BasicToken.T_CLOSE_CURLY, "'}'");
                return new Body(fields, entries);
            }
            TokenLine line = reader.line();
            if (line.isEmpty()) {
                throw reader.refusal("a block opens after an entry's id, not on its own");
            }
            if (reader.at(BasicToken.T_OPEN_CURLY)) {
                entries.add(entry(line));
            } else {
                DataMapField field = field(line);
                if (!names.add(field.name())) {
                    throw line.refusal("'" + field.name() + "' is written twice in one block");
                }
                fields.add(field);
            }
        }
    }

    /** {@code id ["title"] [source …] { … }} */
    private DataMapEntry entry(TokenLine line) {
        String id = name(line, "an entry's id");
        if (!ids.add(id)) {
            throw line.refusal("two entries are called '" + id + "': an id names one entry in its document");
        }
        int next = line.joinedEnd(0);
        int source = line.indexOf(next, SOURCE);
        int end = source < 0 ? line.size() : source;
        Optional<String> title = Optional.empty();
        if (next < end) {
            if (!line.isString(next) || next + 1 != end) {
                throw line.refusal(next, "after an entry's id comes, at most, its title in quotes, then '{'");
            }
            title = Optional.of(line.literal(next));
        }
        Optional<DataMapSource> citation = source < 0 ? Optional.empty() : Optional.of(source(line, source));
        Body body = body();
        return new DataMapEntry(id, title, citation, body.fields(), body.entries());
    }

    /** {@code name value [source …]} */
    private DataMapField field(TokenLine line) {
        String name = name(line, "a field's name");
        int from = line.joinedEnd(0);
        int source = line.indexOf(from, SOURCE);
        int end = source < 0 ? line.size() : source;
        if (from >= end) {
            throw line.refusal("'" + name + "' has no value");
        }
        DataMapValue value = value(line, from, end);
        return new DataMapField(name, value, source < 0 ? Optional.empty() : Optional.of(source(line, source)));
    }

    private static String name(TokenLine line, String what) {
        if (line.isString(0)) {
            throw line.refusal(what + " is a word, not a text in quotes");
        }
        String name = line.joinedWord(0);
        if (name.indexOf(LINK) >= 0 || name.indexOf(FIELD_OF_LINK) >= 0) {
            throw line.refusal(what + " has neither '#' nor '/' in it: those are a link's");
        }
        return name;
    }

    /** {@code source page 7 table 6.3}, to the end of the line. */
    private static DataMapSource source(TokenLine line, int at) {
        Map<String, String> citation = new LinkedHashMap<>();
        int index = at + 1;
        if (index >= line.size()) {
            throw line.refusal(at, "a source names where: source page 7 table 6.3");
        }
        while (index < line.size()) {
            String word = line.joinedWord(index);
            index = line.joinedEnd(index);
            if (index >= line.size()) {
                throw line.refusal(index - 1, "'" + word + "' in a source names nothing");
            }
            String named = line.isString(index) ? line.literal(index) : line.joinedWord(index);
            index = line.isString(index) ? index + 1 : line.joinedEnd(index);
            citation.put(word, named);
        }
        return new DataMapSource(citation);
    }

    private static DataMapValue value(TokenLine line, int from, int to) {
        List<Integer> commas = line.commas(from, to);
        if (!commas.isEmpty()) {
            List<DataMapValue> items = new ArrayList<>();
            int start = from;
            for (int comma : commas) {
                items.add(single(line, start, comma));
                start = comma + 1;
            }
            items.add(single(line, start, to));
            return new DataMapValue.Sequence(items);
        }
        return single(line, from, to);
    }

    /** One item of a value: a name given a value, a range, or a plain value. */
    private static DataMapValue single(TokenLine line, int from, int to) {
        int assign = within(line.indexOf(from, ASSIGN), to);
        if (assign >= 0) {
            if (assign == from || line.joinedEnd(from) != assign) {
                throw line.refusal(from, "a name given a value is written: name = value");
            }
            return new DataMapValue.Assignment(line.joinedWord(from), plainOrRange(line, assign + 1, to));
        }
        return plainOrRange(line, from, to);
    }

    private static DataMapValue plainOrRange(TokenLine line, int from, int to) {
        int range = within(line.indexOf(from, RANGE), to);
        if (range >= 0) {
            return new DataMapValue.Range(plain(line, from, range), plain(line, range + 1, to));
        }
        return plain(line, from, to);
    }

    private static DataMapValue plain(TokenLine line, int from, int to) {
        if (from >= to) {
            throw line.refusal(from, "a value is missing here");
        }
        if (line.isString(from)) {
            if (to != from + 1) {
                throw line.refusal(from + 1, "a text in quotes is a value of its own");
            }
            return new DataMapValue.Text(line.literal(from));
        }
        Optional<Quantity> quantity = Quantity.parse(line.text(from, to));
        if (quantity.isPresent()) {
            return new DataMapValue.Measure(quantity.get());
        }
        if (line.joinedEnd(from) != to) {
            throw line.refusal(from, "'" + line.text(from, to) + "' is not a value: a word, a number with its unit,"
                    + " a text in quotes or a link");
        }
        String word = line.joinedWord(from);
        int link = word.indexOf(LINK);
        if (link < 0 && word.contains(RANGE)) {
            throw line.refusal(from, "'" + word + "': a range is written with its '..' apart — 2 V .. 6 V");
        }
        return link < 0 ? new DataMapValue.Word(word) : link(line, from, word, link);
    }

    /** {@code document.jmd#id} or {@code document.jmd#id/field}. */
    private static DataMapValue link(TokenLine line, int at, String word, int mark) {
        String document = word.substring(0, mark);
        String address = word.substring(mark + 1);
        int slash = address.indexOf(FIELD_OF_LINK);
        String id = slash < 0 ? address : address.substring(0, slash);
        String field = slash < 0 ? "" : address.substring(slash + 1);
        if (document.isEmpty() || id.isEmpty() || (slash >= 0 && field.isEmpty())) {
            throw line.refusal(at, "a link is written document.jmd#id or document.jmd#id/field");
        }
        return new DataMapValue.Link(document, id, field);
    }

    /** The index when it lies before {@code to}; else -1. */
    private static int within(int index, int to) {
        return index >= 0 && index < to ? index : -1;
    }
}
