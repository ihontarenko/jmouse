package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.BoundedTokenCursor.Boundary;
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
 * {@link BoundedTokenCursor} that ends where the grammar says — at the words it writes next ({@link #words}), or at a
 * shape of its own, {@code /div} after a scale or {@code name =} opening the next setting — and at the end of the line;
 * jMF's parser reads it as it reads any formula. The expression is kept as the author wrote it, cut out of the source,
 * for jMF to compile with the document's names.</p>
 */
final class JmxValues {

    /** Nothing but the end of the line ends the value. */
    static final Boundary LINE = (token, after, depth) -> false;

    private final Reading reading;
    private final ParserContext context;
    private final QuantityParser quantities = new QuantityParser();

    JmxValues(Reading reading) {
        this.reading = reading;
        FormulaExtensionContainer formulas = new FormulaExtensionContainer();
        formulas.addParser(quantities);
        this.context = new DefaultParserContext(formulas);
    }

    /** A value ends at one of these words, outside parentheses, and not where it is called: {@code log(x)} is a value. */
    static Boundary words(Set<String> next) {
        return (token, after, depth) -> depth == 0 && token.type() != BasicToken.T_STRING && next.contains(token.value())
                && after.type() != BasicToken.T_OPEN_PAREN;
    }

    Values.Written written(Set<String> next) {
        return written(words(next));
    }

    /**
     * A value as written, and the quantity it is when it is a number with at most its unit — then the number alone is
     * its expression, and the unit is the quantity's.
     *
     * @param until where the line's grammar takes over after the value
     */
    Values.Written written(Boundary until) {
        TokenCursor cursor = reading.cursor;
        TokenCursor.Savepoint start = cursor.savepoint();
        SourceText expression = expression(until);
        TokenCursor.Savepoint end = cursor.savepoint();
        cursor.restore(start);
        Token first = cursor.current();
        // The number ends where its unit begins: one token, or two when its sign stands apart (- 5 V).
        Token unitStart = cursor.lookAt(cursor.isCurrent(BasicToken.T_MINUS, BasicToken.T_PLUS) ? 2 : 1);
        Optional<Quantity> quantity = quantities.read(bounded(until), context).map(QuantityLiteralNode::getQuantity)
                .filter(ignored -> cursor.position() == end.getPosition());
        cursor.restore(end);
        if (quantity.isPresent() && !quantity.get().unit().isEmpty()) {
            expression = reading.text(first.offset(), unitStart.offset());
        }
        return new Values.Written(expression, quantity);
    }

    /**
     * A value that must be a number with at most its unit — a coordinate, a duration, a threshold —, in SI.
     *
     * @param until where the line's grammar takes over after the value
     */
    double quantity(Boundary until) {
        Token from = reading.cursor.current();
        Values.Written written = written(until);
        return written.quantity().orElseThrow(() -> reading.refusal(from,
                "'" + written.expression().text() + "' must be a number with an optional unit")).value();
    }

    /**
     * A plain number where the cursor stands — {@code 240}, {@code -0.5}, {@code - 5} —, read by the engine's
     * {@link QuantityParser}; empty, the cursor where it was, when no number without a unit stands there.
     */
    Optional<Double> number() {
        TokenCursor cursor = reading.cursor;
        TokenCursor.Savepoint start = cursor.savepoint();
        Optional<Quantity> read = quantities.read(bounded(LINE), context).map(QuantityLiteralNode::getQuantity);
        if (read.isEmpty() || !read.get().unit().isEmpty()) {
            cursor.restore(start);
            return Optional.empty();
        }
        return Optional.of(read.get().value());
    }

    SourceText expression(Set<String> next) {
        return expression(words(next));
    }

    /**
     * One expression from where the cursor stands, as written; the cursor is left where it ends.
     *
     * @param until where the line's grammar takes over after the expression
     */
    SourceText expression(Boundary until) {
        Token first = reading.cursor.current();
        if (reading.atLineEnd() || reading.cursor.isCurrent(BasicToken.T_STRING)) {
            throw reading.refusal(first, "a value is missing");
        }
        if (reading.cursor.isCurrent(BasicToken.T_OPEN_BRACKET)) {
            listed(until);
            return reading.text(first.offset(), reading.cursor.endOfRead());
        }
        try {
            context.getParser(ExpressionParser.class).parse(bounded(until), context);
        } catch (ExperienceParseException refusal) {
            throw refusal;
        } catch (RuntimeException unreadable) {
            throw reading.refusal(first, "'" + reading.lineFrom(first) + "' is not a value: " + unreadable.getMessage());
        }
        return reading.text(first.offset(), reading.cursor.endOfRead());
    }

    List<SourceText> listed(Set<String> next) {
        return listed(words(next));
    }

    /**
     * {@code [a, b, c]} — values listed in brackets, each an expression or a list itself, as a matrix's rows are:
     * {@code [[1, 2], [2, 4]]}. jMF has no lists; they are jMX's, and what reads the value reads them.
     *
     * @param until where the line's grammar takes over after the list
     * @return each listed value as written
     */
    List<SourceText> listed(Boundary until) {
        reading.cursor.next();
        List<SourceText> listed = new ArrayList<>();
        listed.add(expression(until));
        while (reading.cursor.isCurrent(BasicToken.T_COMMA)) {
            reading.cursor.next();
            listed.add(expression(until));
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
        TokenCursor view = bounded(words(next));
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

    /** The cursor, ending at the line's end and where the grammar says. */
    private TokenCursor bounded(Boundary until) {
        return new BoundedTokenCursor(reading.cursor, (token, after, depth) -> Reading.endsLine(token)
                || until.endsAt(token, after, depth));
    }
}
