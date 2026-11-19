package org.jmouse.formula.el;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.MathematicOperator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.unary.NegativeUnaryOperation;
import org.jmouse.el.parser.OperatorParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.PrimaryExpressionParser;
import org.jmouse.el.parser.expression.Associativity;
import org.jmouse.el.parser.expression.ExpressionReader;
import org.jmouse.el.parser.expression.InfixBinding;
import org.jmouse.el.parser.expression.PrecedenceGrammar;
import org.jmouse.el.parser.expression.PrecedenceParser;

import java.util.Set;

/**
 * The formula grammar over jME's tokens, operators and nodes. A formula container substitutes it for jME's
 * {@link OperatorParser}, so every place jME parses a sub-expression — parentheses, function arguments — goes
 * through these rules too:
 *
 * <ul>
 *   <li>a leading minus negates: {@code -x^2} is {@code -(x^2)}, {@code 2 * -x} is allowed;</li>
 *   <li>power ({@code ^} or {@code **}) is right-associative: {@code x^y^2} is {@code x^(y^2)};</li>
 *   <li>juxtaposition multiplies: {@code 2x}, {@code 3(x + 1)}, {@code (x + 1)(x - 1)};</li>
 *   <li>a trailing {@code !} is a factorial, a leading one negates a condition: {@code n!}, {@code !(x > 0)};</li>
 *   <li>which operators exist is FormulaCoreExtension's to say; this parser only orders them.</li>
 * </ul>
 */
public class FormulaOperatorParser extends OperatorParser {

    private static final int POWER_PRECEDENCE = 800;
    /** The postfix mark of a factorial; the word `not` reads as the same token and is a negation. */
    private static final String FACTORIAL_MARK = "!";
    private static final int RELATION_PRECEDENCE = 300;
    private static final Set<Token.Type> STARTS_PRIMARY = Set.of(BasicToken.T_IDENTIFIER, BasicToken.T_OPEN_PAREN,
            BasicToken.T_NUMERIC, BasicToken.T_INT, BasicToken.T_LONG, BasicToken.T_FLOAT, BasicToken.T_DOUBLE);

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(expression(cursor, context, 0));
    }

    private Expression expression(TokenCursor cursor, ParserContext context, int minimumPrecedence) {
        return PrecedenceParser.parse(new Input(cursor, context), minimumPrecedence, new FormulaGrammar());
    }

    /** The right side of a power may carry its own sign: 2^-x. */
    private Expression powerOperand(TokenCursor cursor, ParserContext context) {
        return PrecedenceParser.parse(new Input(cursor, context), POWER_PRECEDENCE, new PowerGrammar());
    }

    private record Input(TokenCursor cursor, ParserContext context) {}

    private enum Spelling { EXPLICIT, IMPLICIT }

    private record FormulaOperation(Operator operator, Spelling spelling) {}

    private class FormulaGrammar implements PrecedenceGrammar<Input, Expression, FormulaOperation> {
        @Override
        public Expression primary(Input input, ExpressionReader<Expression> expressions) {
            return unary(input.cursor(), input.context());
        }

        @Override
        public InfixBinding<FormulaOperation> infix(Input input, Expression left) {
            if (!input.cursor().hasNext()) {
                return null;
            }
            Token token = input.cursor().peek();
            Operator operator = input.context().getOperator(token.type());
            Spelling spelling = Spelling.EXPLICIT;
            if (operator == null && STARTS_PRIMARY.contains(token.type())) {
                operator = MathematicOperator.MULTIPLY;
                spelling = Spelling.IMPLICIT;
            }
            if (operator == null) {
                return null;
            }
            return binding(operator, spelling);
        }

        protected InfixBinding<FormulaOperation> binding(Operator operator, Spelling spelling) {
            return new InfixBinding<>(new FormulaOperation(operator, spelling), operator.getPrecedence(),
                    isPower(operator) ? Associativity.RIGHT : Associativity.LEFT);
        }

        @Override
        public Expression reduce(Input input, Expression left, InfixBinding<FormulaOperation> binding,
                                 ExpressionReader<Expression> expressions) {
            if (binding.operator().spelling() == Spelling.EXPLICIT) {
                input.cursor().next();
            }
            return new BinaryOperation(left, binding.operator().operator(), readRight(input, binding, expressions));
        }

        protected Expression readRight(Input input, InfixBinding<FormulaOperation> binding,
                                       ExpressionReader<Expression> expressions) {
            return isPower(binding.operator().operator()) ? powerOperand(input.cursor(), input.context())
                    : expressions.parse(binding.rightMinimum());
        }

        @Override
        public long position(Input input) {
            return input.cursor().position();
        }
    }

    /** The signed, power-only right operand retains the formula language's original grammar. */
    private final class PowerGrammar extends FormulaGrammar {
        @Override
        public Expression primary(Input input, ExpressionReader<Expression> expressions) {
            if (input.cursor().consumeIf(BasicToken.T_MINUS)) {
                return new NegativeUnaryOperation(expressions.parse(POWER_PRECEDENCE));
            }
            if (input.cursor().consumeIf(BasicToken.T_PLUS)) {
                return expressions.parse(POWER_PRECEDENCE);
            }
            return FormulaOperatorParser.this.primary(input.cursor(), input.context());
        }

        @Override
        public InfixBinding<FormulaOperation> infix(Input input, Expression left) {
            Operator operator = input.cursor().hasNext()
                    ? input.context().getOperator(input.cursor().peek().type()) : null;
            return operator != null && isPower(operator) ? binding(operator, Spelling.EXPLICIT) : null;
        }

        @Override
        protected Expression readRight(Input input, InfixBinding<FormulaOperation> binding,
                                       ExpressionReader<Expression> expressions) {
            return expressions.parse(binding.rightMinimum());
        }
    }

    private Expression unary(TokenCursor cursor, ParserContext context) {
        if (cursor.isCurrent(BasicToken.T_NEGATE)) {
            // `!x > 0 and y` is `(!(x > 0)) and y`: a negation takes a whole comparison, not just its left side.
            cursor.next();
            return new NotNode(expression(cursor, context, RELATION_PRECEDENCE));
        }
        if (cursor.isCurrent(BasicToken.T_MINUS)) {
            cursor.next();
            return new NegativeUnaryOperation(expression(cursor, context, POWER_PRECEDENCE));
        }
        if (cursor.isCurrent(BasicToken.T_PLUS)) {
            cursor.next();
            return unary(cursor, context);
        }
        return primary(cursor, context);
    }

    /** A primary expression and the factorials that follow it: `n!`, `(n + 1)!`, `3!!` as (3!)!. */
    private Expression primary(TokenCursor cursor, ParserContext context) {
        Expression primary = (Expression) context.getParser(PrimaryExpressionParser.class).parse(cursor, context);
        // Only the mark is a factorial: 'not' shares its token, and 'n not' is no factorial.
        while (cursor.isCurrent(BasicToken.T_NEGATE) && FACTORIAL_MARK.equals(cursor.peek().value())) {
            cursor.next();
            primary = new FactorialNode(primary);
        }
        return primary;
    }

    private static boolean isPower(Operator operator) {
        return operator == FormulaOperator.POWER || operator == MathematicOperator.EXPONENTIAL;
    }
}
