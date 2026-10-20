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
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.units.Quantity;

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
        Optional<Quantity> quantity = quantities.read(bounded(next), context).map(QuantityLiteralNode::getQuantity)
                .filter(ignored -> cursor.position() == end.getPosition());
        cursor.restore(end);
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
        try {
            context.getParser(ExpressionParser.class).parse(bounded(next), context);
        } catch (ExperienceParseException refusal) {
            throw refusal;
        } catch (RuntimeException unreadable) {
            throw reading.refusal(first, "'" + reading.lineFrom(first) + "' is not a value: " + unreadable.getMessage());
        }
        return reading.text(first.offset(), reading.cursor.endOfRead());
    }

    /** The cursor, ending at the line's end and at its grammar's next words outside parentheses, unless called. */
    private TokenCursor bounded(Set<String> next) {
        return new BoundedTokenCursor(reading.cursor, (token, after, depth) -> Reading.endsLine(token)
                || (depth == 0 && token.type() != BasicToken.T_STRING && next.contains(token.value())
                    && after.type() != BasicToken.T_OPEN_PAREN));
    }
}
