package org.jmouse.datamap.parser;

import org.jmouse.datamap.DataMapSyntaxException;
import org.jmouse.datamap.DataMapValue;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jmouse.datamap.parser.DataMapReading.ASSIGN;
import static org.jmouse.datamap.parser.DataMapReading.COMMA;
import static org.jmouse.datamap.parser.DataMapReading.RANGE;

/**
 * Reads a field's value, to the end of its line or to its {@code source}.
 *
 * <pre>
 * value := item (',' item)*
 * item  := [word '='] bound ['..' bound]
 * bound := "text" | number [unit] | word | document#id[/field]
 * </pre>
 */
final class DataMapValueReading {

    private static final char LINK = '#';
    private static final char FIELD_OF_LINK = '/';

    private DataMapValueReading() {
    }

    /** The value from where the cursor stands. */
    static DataMapValue value(TokenCursor cursor) {
        List<DataMapValue> items = new ArrayList<>();
        items.add(item(cursor));
        while (atText(cursor, COMMA)) {
            cursor.next();
            items.add(item(cursor));
        }
        return items.size() == 1 ? items.getFirst() : new DataMapValue.Sequence(items);
    }

    /** Whether the value ends where the cursor stands: its line ends, or its citation begins. */
    static boolean atValueEnd(TokenCursor cursor) {
        return DataMapReading.atLineEnd(cursor) || DataMapReading.atSource(cursor);
    }

    private static DataMapValue item(TokenCursor cursor) {
        requireValue(cursor);
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            return rangeFrom(cursor, new DataMapValue.Text(DataMapReading.text(cursor.next())));
        }
        Token first = cursor.lookAt(0);
        String word = DataMapReading.word(cursor);
        if (atText(cursor, ASSIGN)) {
            cursor.next();
            return new DataMapValue.Assignment(word, rangeOrBound(cursor));
        }
        return rangeFrom(cursor, bound(cursor, first, word));
    }

    private static DataMapValue rangeOrBound(TokenCursor cursor) {
        requireValue(cursor);
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            return rangeFrom(cursor, new DataMapValue.Text(DataMapReading.text(cursor.next())));
        }
        Token first = cursor.lookAt(0);
        return rangeFrom(cursor, bound(cursor, first, DataMapReading.word(cursor)));
    }

    /** A bound already read, and the range it begins when {@code ..} follows it. */
    private static DataMapValue rangeFrom(TokenCursor cursor, DataMapValue from) {
        if (!atRange(cursor)) {
            return from;
        }
        cursor.next();
        requireValue(cursor);
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            return new DataMapValue.Range(from, new DataMapValue.Text(DataMapReading.text(cursor.next())));
        }
        Token first = cursor.lookAt(0);
        return new DataMapValue.Range(from, bound(cursor, first, DataMapReading.word(cursor)));
    }

    /**
     * A bound whose first word is read: a number with its unit, written joined ({@code 2.2µs}) or apart
     * ({@code 19 ns}); a link; a word.
     */
    private static DataMapValue bound(TokenCursor cursor, Token first, String word) {
        Optional<Quantity> joined = Quantity.parse(word);
        if (joined.isPresent()) {
            if (joined.get().unit().isEmpty() && DataMapReading.atWord(cursor) && !atRange(cursor)
                    && !DataMapReading.atSource(cursor)) {
                TokenCursor.Savepoint beforeUnit = cursor.savepoint();
                Optional<Quantity> apart = Quantity.parse(word + " " + DataMapReading.word(cursor));
                if (apart.isPresent()) {
                    return new DataMapValue.Measure(apart.get());
                }
                cursor.restore(beforeUnit);
            }
            return new DataMapValue.Measure(joined.get());
        }
        int link = word.indexOf(LINK);
        if (link >= 0) {
            return link(cursor, first, word, link);
        }
        if (word.contains(RANGE)) {
            throw new DataMapSyntaxException(cursor, first,
                    "'" + word + "': a range is written with its '..' apart — 2 V .. 6 V");
        }
        return new DataMapValue.Word(word);
    }

    /** {@code document.jmd#id} or {@code document.jmd#id/field}. */
    private static DataMapValue link(TokenCursor cursor, Token first, String word, int mark) {
        String document = word.substring(0, mark);
        String address = word.substring(mark + 1);
        int slash = address.indexOf(FIELD_OF_LINK);
        String id = slash < 0 ? address : address.substring(0, slash);
        String field = slash < 0 ? "" : address.substring(slash + 1);
        if (document.isEmpty() || id.isEmpty() || (slash >= 0 && field.isEmpty())) {
            throw new DataMapSyntaxException(cursor, first, "a link is written document.jmd#id or document.jmd#id/field");
        }
        return new DataMapValue.Link(document, id, field);
    }

    /** Whether {@code ..} stands where the cursor does, apart from the words around it. */
    private static boolean atRange(TokenCursor cursor) {
        Token current = cursor.lookAt(0);
        return current.type() == BasicToken.T_DOUBLE_DOT
                && !SourceReading.touches(cursor, cursor.lookAt(-1), current)
                && !SourceReading.touches(cursor, current, cursor.lookAt(1));
    }

    private static boolean atText(TokenCursor cursor, String text) {
        return !DataMapReading.atLineEnd(cursor) && cursor.lookAt(0).value().equals(text);
    }

    private static void requireValue(TokenCursor cursor) {
        if (atValueEnd(cursor) || (!cursor.isCurrent(BasicToken.T_STRING) && !DataMapReading.atWord(cursor))) {
            throw new DataMapSyntaxException(cursor, "a value is missing here");
        }
    }
}
