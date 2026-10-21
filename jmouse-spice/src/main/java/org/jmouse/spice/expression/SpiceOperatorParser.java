package org.jmouse.spice.expression;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.MathematicOperator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.unary.NegativeUnaryOperation;
import org.jmouse.el.parser.OperatorParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.PrimaryExpressionParser;
import org.jmouse.formula.el.FormulaOperator;
import org.jmouse.formula.el.NotNode;

/**
 * SPICE's operator grammar over jME's tokens, operators and nodes, substituted for jME's {@link OperatorParser} by
 * {@link SpiceExtensionContainer}, so parentheses and a call's arguments go through it too:
 *
 * <ul>
 *   <li>a leading minus negates and binds below a power: {@code -x**2} is {@code -(x**2)}, as jMF reads it;</li>
 *   <li>{@code **} and {@code ^} are one power and bind to the right: {@code 2**3**2} is {@code 2**(3**2)};</li>
 *   <li>a leading {@code !} negates a condition and binds as a leading minus does, as in C;</li>
 *   <li>nothing multiplies without an operator: {@code 2 R1} is not SPICE, and the parse stops in front of
 *       {@code R1}, which the language refuses as a part it did not read.</li>
 * </ul>
 */
final class SpiceOperatorParser extends OperatorParser {

    /** Where a power binds, and what a leading sign takes: everything that binds at least as tightly. */
    private static final int POWER_PRECEDENCE = 800;

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(expression(cursor, context, 0));
    }

    private Expression expression(TokenCursor cursor, ParserContext context, int minimumPrecedence) {
        Expression left = unary(cursor, context);
        while (cursor.hasNext()) {
            Operator operator = context.getOperator(cursor.peek().type());
            if (operator == null || precedenceOf(operator) < minimumPrecedence) {
                break;
            }
            cursor.next();
            Expression right = isPower(operator) ? powerOperand(cursor, context)
                    : expression(cursor, context, precedenceOf(operator) + 1);
            left = new BinaryOperation(left, operator, right);
        }
        return left;
    }

    /** The right side of a power, which may carry its own sign — {@code 2**-x} — and nests rightwards. */
    private Expression powerOperand(TokenCursor cursor, ParserContext context) {
        if (cursor.consumeIf(BasicToken.T_MINUS)) {
            return new NegativeUnaryOperation(powerOperand(cursor, context));
        }
        if (cursor.consumeIf(BasicToken.T_PLUS)) {
            return powerOperand(cursor, context);
        }
        Expression base = primary(cursor, context);
        Operator next = cursor.hasNext() ? context.getOperator(cursor.peek().type()) : null;
        if (next != null && isPower(next)) {
            cursor.next();
            return new BinaryOperation(base, next, powerOperand(cursor, context));
        }
        return base;
    }

    private Expression unary(TokenCursor cursor, ParserContext context) {
        if (cursor.consumeIf(BasicToken.T_NEGATE)) {
            return new NotNode(expression(cursor, context, POWER_PRECEDENCE));
        }
        if (cursor.consumeIf(BasicToken.T_MINUS)) {
            return new NegativeUnaryOperation(expression(cursor, context, POWER_PRECEDENCE));
        }
        if (cursor.consumeIf(BasicToken.T_PLUS)) {
            return unary(cursor, context);
        }
        return primary(cursor, context);
    }

    private Expression primary(TokenCursor cursor, ParserContext context) {
        return (Expression) context.getParser(PrimaryExpressionParser.class).parse(cursor, context);
    }

    /** Both powers bind at one strength, whatever jME ranks its {@code **} at. */
    private static int precedenceOf(Operator operator) {
        return isPower(operator) ? POWER_PRECEDENCE : operator.getPrecedence();
    }

    private static boolean isPower(Operator operator) {
        return operator == FormulaOperator.POWER || operator == MathematicOperator.EXPONENTIAL;
    }
}
