package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.LambdaNode;
import org.jmouse.el.node.expression.ParameterSetNode;
import org.jmouse.el.parser.sub.ParametersParser;

import static org.jmouse.el.lexer.BasicToken.*;

/**
 * Parses a lambda expression of the form {@code (params) -> body} or
 * {@code (params) -> { body }} and constructs a {@link LambdaNode}.
 * <p>
 * Supports optional parameter lists and optional block-style bodies.
 * </p>
 *
 * Example:
 * <pre>{@code
 *   (x, y) -> x + y
 *   () -> { compute() }
 * }</pre>
 *
 * Invoked when the parser encounters an opening parenthesis
 * followed by the arrow token {@code ->}.
 *
 * @author Ivan Hontarenko
 * @author ihontarenko@gmail.com
 */
public class LambdaParser implements Parser {

    /**
     * Parses a lambda expression and adds it to the given parent node.
     *
     * @param cursor  the token stream cursor positioned at '('
     * @param parent  the AST node to which the lambda node is attached
     * @param context the parser context for retrieving sub-parsers
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        LambdaNode lambda = new LambdaNode();

        // Expect opening parenthesis for parameters
        cursor.currentIf(T_OPEN_PAREN);

        // Parse optional parameter list
        if (cursor.isCurrent(T_IDENTIFIER)) {
            Expression params = (Expression)
                    context.getParser(ParametersParser.class)
                            .parse(cursor, context);
            lambda.setParameters((ParameterSetNode) params);
        }

        // Expect closing parenthesis and arrow token
        cursor.currentIf(T_CLOSE_PAREN);
        cursor.ensure(T_ARROW);

        // Optional block braces around body
        cursor.currentIf(T_OPEN_CURLY);
        if (!cursor.isCurrent(T_CLOSE_CURLY)) {
            Expression body = (Expression)
                    context.getParser(ExpressionParser.class)
                            .parse(cursor, context);
            lambda.setBody(body);
        }
        cursor.currentIf(T_CLOSE_CURLY);

        parent.add(lambda);
    }
}
