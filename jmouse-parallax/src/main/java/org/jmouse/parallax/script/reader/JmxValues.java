package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.QuantityParser;
import org.jmouse.formula.el.FormulaExtensionContainer;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The values of a jMX line, read by jMF's grammar on the engine: {@code 2 pi f}, {@code 8 / 3},
 * {@code 100 * abs(a - b) / rating}, and a number with its unit as one operand — {@code 100 nF}, {@code 200 ppm/K}.
 *
 * <p>Where a value ends is the line's grammar to say, not the expression's: in {@code t = 0 in 0 .. 2 pi step 0.01}
 * only the grammar knows {@code step} is its word and {@code pi} a factor. So a value is read through a
 * {@link BoundedTokenCursor} that ends at the words the grammar writes next — outside parentheses, and not where they
 * are called — and at the end of the line; jMF's parser reads it as it reads any formula. The expression is kept as the
 * author wrote it, cut out of the source, for jMF to compile with the document's names.</p>
 */
final class JmxValues {

    private final Reading reading;
    private final ParserContext context;
    private final QuantityParser quantities = new QuantityParser();

    JmxValues(Reading reading) {
        this.reading = reading;
        FormulaExtensionContainer formulas = new FormulaExtensionContainer();
        formulas.addParser(quantities);
        this.context = new DefaultParserContext(formulas);
    }

    /**
     * A value as written, and the quantity it is when it is a number with at most its unit.
     *
     * @param next the words the line's grammar may write after the value
     */
    Values.Written written(Set<String> next) {
        TokenCursor cursor = reading.cursor;
        TokenCursor.Savepoint start = cursor.savepoint();
        SourceText expression = expression(next);
        TokenCursor.Savepoint end = cursor.savepoint();
        cursor.restore(start);
        Token first = cursor.current();
        // The number ends where its unit begins: one token, or two when its sign stands apart (- 5 V).
        Token unitStart = cursor.lookAt(cursor.isCurrent(BasicToken.T_MINUS, BasicToken.T_PLUS) ? 2 : 1);
        Optional<Quantity> quantity = quantities.read(bounded(next), context).map(QuantityLiteralNode::getQuantity)
                .filter(ignored -> cursor.position() == end.getPosition());
        cursor.restore(end);
        if (quantity.isPresent() && !quantity.get().unit().isEmpty()) {
            // A number with its unit keeps the number as its expression and the unit in its quantity.
            expression = reading.text(first.offset(), unitStart.offset());
        }
        return new Values.Written(expression, quantity);
    }

    /**
     * One expression from where the cursor stands, as written; the cursor is left where it ends.
     *
     * @param next the words the line's grammar may write after the expression
     */
    SourceText expression(Set<String> next) {
        Token first = reading.cursor.current();
        if (reading.atLineEnd() || reading.cursor.isCurrent(BasicToken.T_STRING)) {
            throw reading.refusal(first, "a value is missing");
        }
        if (reading.cursor.isCurrent(BasicToken.T_OPEN_BRACKET)) {
            listed(next);
            return reading.text(first.offset(), reading.cursor.endOfRead());
        }
        try {
            context.getParser(ExpressionParser.class).parse(bounded(next), context);
        } catch (ExperienceParseException refusal) {
            throw refusal;
        } catch (RuntimeException unreadable) {
            throw reading.refusal(first, "'" + reading.lineFrom(first) + "' is not a value: " + unreadable.getMessage());
        }
        return reading.text(first.offset(), reading.cursor.endOfRead());
    }

    /**
     * {@code [a, b, c]} — values listed in brackets, each an expression or a list itself, as a matrix's rows are:
     * {@code [[1, 2], [2, 4]]}. jMF has no lists; they are jMX's, and what reads the value reads them.
     *
     * @param next the words the line's grammar may write after the list
     * @return each listed value as written
     */
    List<SourceText> listed(Set<String> next) {
        reading.cursor.next();
        List<SourceText> listed = new ArrayList<>();
        listed.add(expression(next));
        while (reading.cursor.isCurrent(BasicToken.T_COMMA)) {
            reading.cursor.next();
            listed.add(expression(next));
        }
        reading.expect(BasicToken.T_CLOSE_BRACKET, "']' to close the listed values");
        return listed;
    }

    /**
     * {@code minimum .. maximum}, each bound an expression.
     *
     * @param next the words the line's grammar may write after the range
     */
    Block.Range range(Set<String> next) {
        Token from = reading.cursor.current();
        SourceText minimum = expression(next);
        if (!reading.cursor.isCurrent(BasicToken.T_DOUBLE_DOT)) {
            throw reading.refusal(from, "a range is written 'minimum .. maximum'");
        }
        reading.cursor.next();
        return new Block.Range(minimum, expression(next));
    }

    /**
     * {@code (a, b, c)}: each coordinate an expression. A single expression — or one in parentheses around the whole
     * of it, {@code (x + 1)} — is one coordinate.
     *
     * @param next the words the line's grammar may write after the tuple
     */
    List<SourceText> tuple(Set<String> next) {
        TokenCursor view = bounded(next);
        int close = closingOf(view);
        boolean whole = close >= 0
                && (close + 1 >= view.remaining() || view.lookAt(close + 1).type() == BasicToken.T_STRING);
        if (!whole || !hasCommaInside(view, close)) {
            return List.of(expression(next));
        }
        reading.cursor.next();
        List<SourceText> coordinates = new ArrayList<>();
        coordinates.add(expression(next));
        while (reading.cursor.isCurrent(BasicToken.T_COMMA)) {
            reading.cursor.next();
            coordinates.add(expression(next));
        }
        reading.expect(BasicToken.T_CLOSE_PAREN, "')' to close the coordinates");
        return coordinates;
    }

    /** The offset of the parenthesis closing the one the view stands on; -1 when it stands on none, or it never closes. */
    private static int closingOf(TokenCursor view) {
        if (!view.isCurrent(BasicToken.T_OPEN_PAREN)) {
            return -1;
        }
        int depth = 0;
        for (int offset = 0; offset < view.remaining(); offset++) {
            if (view.lookAt(offset).type() == BasicToken.T_OPEN_PAREN) {
                depth++;
            } else if (view.lookAt(offset).type() == BasicToken.T_CLOSE_PAREN && --depth == 0) {
                return offset;
            }
        }
        return -1;
    }

    private static boolean hasCommaInside(TokenCursor view, int close) {
        int depth = 0;
        for (int offset = 0; offset < close; offset++) {
            Token.Type type = view.lookAt(offset).type();
            if (type == BasicToken.T_OPEN_PAREN || type == BasicToken.T_OPEN_BRACKET) {
                depth++;
            } else if (type == BasicToken.T_CLOSE_PAREN || type == BasicToken.T_CLOSE_BRACKET) {
                depth--;
            } else if (depth == 1 && type == BasicToken.T_COMMA) {
                return true;
            }
        }
        return false;
    }

    /** The cursor, ending at the line's end and at its grammar's next words outside parentheses, unless called. */
    private TokenCursor bounded(Set<String> next) {
        return new BoundedTokenCursor(reading.cursor, (token, after, depth) -> Reading.endsLine(token)
                || (depth == 0 && token.type() != BasicToken.T_STRING && next.contains(token.value())
                    && after.type() != BasicToken.T_OPEN_PAREN));
    }
}
