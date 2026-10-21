package org.jmouse.sheet.parser;

import org.jmouse.sheet.SheetSyntaxException;
import org.jmouse.sheet.SheetValue;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.QuantityParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jmouse.sheet.parser.SheetReading.ASSIGN;
import static org.jmouse.sheet.parser.SheetReading.COMMA;
import static org.jmouse.sheet.parser.SheetReading.RANGE;

/**
 * Reads a field's value, to the end of its line or to its {@code source}.
 *
 * <pre>
 * value := item (',' item)*
 * item  := [word '='] bound ['..' bound]
 * bound := "text" | number [unit] | word | document#id[/field]
 * </pre>
 */
final class SheetValueReading {

    private static final char LINK = '#';
    private static final char FIELD_OF_LINK = '/';

    private SheetValueReading() {
    }

    /** The value from where the cursor stands. */
    static SheetValue value(TokenCursor cursor, ParserContext context) {
        List<SheetValue> items = new ArrayList<>();
        items.add(item(cursor, context));
        while (atText(cursor, COMMA)) {
            cursor.next();
            items.add(item(cursor, context));
        }
        return items.size() == 1 ? items.getFirst() : new SheetValue.Sequence(items);
    }

    /** Whether the value ends where the cursor stands: its line ends, or its citation begins. */
    static boolean atValueEnd(TokenCursor cursor) {
        return SheetReading.atLineEnd(cursor) || SheetReading.atSource(cursor);
    }

    private static SheetValue item(TokenCursor cursor, ParserContext context) {
        requireValue(cursor);
        if (!cursor.isCurrent(BasicToken.T_STRING)) {
            TokenCursor.Savepoint start = cursor.savepoint();
            String name = SheetReading.word(cursor);
            if (atText(cursor, ASSIGN)) {
                cursor.next();
                return new SheetValue.Assignment(name, rangeOrBound(cursor, context));
            }
            cursor.restore(start);
        }
        return rangeOrBound(cursor, context);
    }

    /** A bound, and the range it begins when {@code ..} follows it. */
    private static SheetValue rangeOrBound(TokenCursor cursor, ParserContext context) {
        SheetValue from = bound(cursor, context);
        if (!atRange(cursor)) {
            return from;
        }
        cursor.next();
        return new SheetValue.Range(from, bound(cursor, context));
    }

    /**
     * A text; a number with its unit, written joined ({@code 2.2µs}) or apart ({@code 19 ns}), read by the engine's
     * {@link QuantityParser}; a link; a word.
     */
    private static SheetValue bound(TokenCursor cursor, ParserContext context) {
        requireValue(cursor);
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            return new SheetValue.Text(SheetReading.text(cursor.next()));
        }
        Optional<QuantityLiteralNode> quantity = ((QuantityParser) context.getParser(QuantityParser.class))
                .read(cursor, context);
        if (quantity.isPresent()) {
            return new SheetValue.Measure(quantity.get().getQuantity());
        }
        Token first = cursor.lookAt(0);
        String word = SheetReading.word(cursor);
        int link = word.indexOf(LINK);
        if (link >= 0) {
            return link(cursor, first, word, link);
        }
        if (word.contains(RANGE)) {
            throw new SheetSyntaxException(cursor, first,
                    "'" + word + "': a range is written with its '..' apart — 2 V .. 6 V");
        }
        return new SheetValue.Word(word);
    }

    /** {@code document.sht#id} or {@code document.sht#id/field}. */
    private static SheetValue link(TokenCursor cursor, Token first, String word, int mark) {
        String document = word.substring(0, mark);
        String address = word.substring(mark + 1);
        int slash = address.indexOf(FIELD_OF_LINK);
        String id = slash < 0 ? address : address.substring(0, slash);
        String field = slash < 0 ? "" : address.substring(slash + 1);
        if (document.isEmpty() || id.isEmpty() || (slash >= 0 && field.isEmpty())) {
            throw new SheetSyntaxException(cursor, first, "a link is written document.sht#id or document.sht#id/field");
        }
        return new SheetValue.Link(document, id, field);
    }

    /** Whether {@code ..} stands where the cursor does, apart from the words around it. */
    private static boolean atRange(TokenCursor cursor) {
        Token current = cursor.lookAt(0);
        return current.type() == BasicToken.T_DOUBLE_DOT
                && !SourceReading.touches(cursor, cursor.lookAt(-1), current)
                && !SourceReading.touches(cursor, current, cursor.lookAt(1));
    }

    private static boolean atText(TokenCursor cursor, String text) {
        return !SheetReading.atLineEnd(cursor) && cursor.lookAt(0).value().equals(text);
    }

    private static void requireValue(TokenCursor cursor) {
        if (atValueEnd(cursor) || (!cursor.isCurrent(BasicToken.T_STRING) && !SheetReading.atWord(cursor))) {
            throw new SheetSyntaxException(cursor, "a value is missing here");
        }
    }
}
