package org.jmouse.template.parser.arithmetic;

import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.ExpressionNode;
import org.jmouse.template.node.Node;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

/**
 * AdditiveExpressionParser parses additive expressions.
 *
 * <p>
 * Grammar:
 * </p>
 *
 * <pre>{@code
 *   AdditiveExpression ::= MultiplicativeExpression { ("+" | "-") MultiplicativeExpression }
 * }</pre>
 *
 * <p>
 * This parser handles the addition and subtraction operators, delegating the parsing of multiplicative expressions
 * to the MultiplicativeExpressionParser.
 * </p>
 */
public class AdditiveExpressionParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {

    }

    public ExpressionNode parseExpression(TokenCursor cursor, ParserContext context) {
        return null;
    }
}
