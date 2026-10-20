package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.QuantityParser;
import org.jmouse.formula.el.FormulaExtensionContainer;
import org.jmouse.formula.el.FormulaOperatorParser.Juxtaposition;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.units.Quantity;

import java.util.Optional;

/**
 * The values of a jMX line, read by jMF's grammar on the engine: {@code 2 * pi * f}, {@code 8 / 3},
 * {@code 100 * abs(a - b) / rating}, and a number with its unit as one operand — {@code 100 nF}, {@code 200 ppm/K}.
 *
 * <p>Composed for a language whose lines go on after a value: juxtaposition ends the expression
 * ({@link Juxtaposition#ENDS_EXPRESSION}), so the line's own words — {@code in}, {@code step}, {@code log} — are
 * simply not operators and the value stops before them; a {@link QuantityParser} is registered, so a unit is read with
 * its number. The expression is kept as the author wrote it, cut out of the source, for jMF to compile with the
 * document's names.</p>
 */
final class JmxValues {

    private final Reading reading;
    private final ParserContext context;
    private final QuantityParser quantities = new QuantityParser();

    JmxValues(Reading reading) {
        this.reading = reading;
        FormulaExtensionContainer formulas = new FormulaExtensionContainer(Juxtaposition.ENDS_EXPRESSION);
        formulas.addParser(quantities);
        this.context = new DefaultParserContext(formulas);
    }

    /** A value as written, and the quantity it is when it is a number with at most its unit. */
    Values.Written written() {
        TokenCursor cursor = reading.cursor;
        TokenCursor.Savepoint start = cursor.savepoint();
        SourceText expression = expression();
        TokenCursor.Savepoint end = cursor.savepoint();
        cursor.restore(start);
        Optional<Quantity> quantity = quantities.read(cursor, context).map(QuantityLiteralNode::getQuantity)
                .filter(ignored -> cursor.position() == end.getPosition());
        cursor.restore(end);
        return new Values.Written(expression, quantity);
    }

    /** One expression from where the cursor stands, as written; the cursor is left where it ends. */
    SourceText expression() {
        TokenCursor cursor = reading.cursor;
        Token first = cursor.current();
        if (reading.atLineEnd() || cursor.isCurrent(BasicToken.T_STRING)) {
            throw reading.refusal(first, "a value is missing");
        }
        try {
            context.getParser(ExpressionParser.class).parse(cursor, context);
        } catch (ExperienceParseException refusal) {
            throw refusal;
        } catch (RuntimeException unreadable) {
            throw reading.refusal(first, "'" + reading.lineFrom(first) + "' is not a value: " + unreadable.getMessage());
        }
        return reading.text(first.offset(), reading.endOfRead());
    }
}
