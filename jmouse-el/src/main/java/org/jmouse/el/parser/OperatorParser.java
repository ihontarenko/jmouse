package org.jmouse.el.parser;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.FilterOperator;
import org.jmouse.el.extension.operator.InOperator;
import org.jmouse.el.extension.operator.NullCoalesceOperator;
import org.jmouse.el.extension.operator.TestOperator;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.*;
import org.jmouse.el.parser.expression.Associativity;
import org.jmouse.el.parser.expression.ExpressionReader;
import org.jmouse.el.parser.expression.InfixBinding;
import org.jmouse.el.parser.expression.PrecedenceGrammar;
import org.jmouse.el.parser.expression.PrecedenceParser;

import static org.jmouse.el.lexer.BasicToken.*;

/**
 * Parses arithmetic and logical expressions with operator precedence.
 * <p>
 * Supports nested expressions, implicit multiplication (e.g. 2(3 + 4) → 2 * (3 + 4)), and test operators.
 * </p>
 */
public class OperatorParser implements Parser {

    /**
     * Parses an expression and attaches it to the parent node.
     *
     * @param cursor  the token stream cursor
     * @param parent  the parent node to add the parsed expression
     * @param context the parser context
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(parseExpression(cursor, context, 0));
    }

    /**
     * Parses an expression considering operator precedence.
     *
     * @param cursor     the token cursor
     * @param context    the parser context
     * @param precedence the current precedence level
     * @return the parsed expression node
     */
    private Expression parseExpression(TokenCursor cursor, ParserContext context, int precedence) {
        return PrecedenceParser.parse(new Input(cursor, context), precedence, new Grammar());
    }

    private record Input(TokenCursor cursor, ParserContext context) {}

    /** jME's operators and node construction, over the shared ordering mechanism. */
    private final class Grammar implements PrecedenceGrammar<Input, Expression, Operator> {

        @Override
        public Expression primary(Input input, ExpressionReader<Expression> expressions) {
            return parsePrimaryExpression(input.cursor(), input.context());
        }

        @Override
        public InfixBinding<Operator> infix(Input input, Expression left) {
            if (!input.cursor().hasNext()) {
                return null;
            }

            Operator operator = input.context().getOperator(input.cursor().peek().type());

            return operator == null ? null
                    : new InfixBinding<>(operator, operator.getPrecedence(), Associativity.LEFT);
        }

        @Override
        public Expression reduce(Input input, Expression left, InfixBinding<Operator> binding,
                                 ExpressionReader<Expression> expressions) {
            TokenCursor cursor = input.cursor();
            ParserContext context = input.context();
            cursor.next();
            return switch (binding.operator()) {
                case InOperator.IN -> {
                    InOperationNode node = new InOperationNode();
                    node.setLeft(left);
                    node.setRight(expressions.parse(binding.rightMinimum()));
                    yield node;
                }
                case TestOperator.IS -> {
                    TestNode node = (TestNode) context.getParser(TestParser.class).parse(cursor, context);
                    node.setLeft(left);
                    yield node;
                }
                case NullCoalesceOperator.NULL_COALESCE -> {
                    NullSafeFallbackNode node = new NullSafeFallbackNode();
                    node.setNullable(left);
                    node.setOtherwise(expressions.parse(binding.rightMinimum()));
                    yield node;
                }
                case FilterOperator.FILTER -> {
                    Expression filtered = left;
                    while (cursor.hasNext() && cursor.isCurrent(T_IDENTIFIER)) {
                        if (context.getParser(FilterParser.class).parse(cursor, context) instanceof FilterNode filter) {
                            filter.setLeft(filtered);
                            filtered = filter;
                        }
                    }
                    yield filtered;
                }
                default -> new BinaryOperation(left, binding.operator(), expressions.parse(binding.rightMinimum()));
            };
        }

        @Override
        public long position(Input input) {
            return input.cursor().position();
        }
    }

    /**
     * Parses a primary expression.
     *
     * @param cursor  the token cursor
     * @param context the parser context
     * @return the parsed primary expression node
     */
    private Expression parsePrimaryExpression(TokenCursor cursor, ParserContext context) {
        PrimaryExpressionParser parser = (PrimaryExpressionParser) context.getParser(PrimaryExpressionParser.class);
        Expression              left   = (Expression) parser.parse(cursor, context);

        // Implicit multiplication: e.g., 2(3 + 4) becomes 2 * (3 + 4)
        if (cursor.isCurrent(T_OPEN_PAREN)) {
            Operator multiply = context.getOperator(T_MULTIPLY);
            left = new BinaryOperation(left, multiply, parsePrimaryExpression(cursor, context));
        }

        return left;
    }
}
