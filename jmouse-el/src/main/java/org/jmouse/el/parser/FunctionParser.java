package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.ParenthesesParser;

/**
 * 🧩 Parses function calls and their arguments.
 *
 * <p>Supports simple and namespaced identifiers and an optional parenthesized
 * argument list. When arguments are present, delegates to {@link ParenthesesParser}
 * with {@link ArgumentsParser} as the next parser.</p>
 *
 * <h3>Grammar (simplified)</h3>
 * <pre>
 * FunctionCall  ::= Identifier ( ":" Identifier )? Arguments?
 * Arguments     ::= "(" ( Expression ( "," Expression )* )? ")"
 * Identifier    ::= T_IDENTIFIER
 * </pre>
 *
 * <h3>Examples</h3>
 * <ul>
 *   <li>{@code emptyFunction()}</li>
 *   <li>{@code sum(1, 2, x)}</li>
 *   <li>{@code ns:max(1, min(2, 3))}</li>
 * </ul>
 *
 * @see ParenthesesParser
 * @see ArgumentsParser
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FunctionParser implements Parser {

    /**
     * 🚀 Parses a function invocation and attaches a {@link FunctionNode} to the {@code parent}.
     *
     * <p>Behavior:</p>
     * <ol>
     *   <li>Consumes a mandatory {@link BasicToken#T_IDENTIFIER} as the function name.</li>
     *   <li>Optionally consumes {@code ":"} + {@link BasicToken#T_IDENTIFIER} (namespaced name).</li>
     *   <li>If the next token is {@link BasicToken#T_OPEN_PAREN}, parses a parenthesized
     *       argument list via {@link ParenthesesParser}, with {@link ArgumentsParser} set
     *       as the next parser in {@link ParserOptions}.</li>
     *   <li>Creates and adds a {@link FunctionNode} to {@code parent}.</li>
     * </ol>
     *
     * <p><b>Note:</b> Token expectations are enforced via {@link TokenCursor#ensure(BasicToken)},
     * which signals a parse error if they are not met.</p>
     *
     * @param cursor  token stream cursor (advanced as tokens are consumed)
     * @param parent  AST node to which the parsed {@link FunctionNode} is added
     * @param context parser context carrying options and sub-parsers
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        // Extract function name and ensure it's a valid identifier
        StringBuilder builder = new StringBuilder(cursor.peek().value());

        // Ensure that the current token is a function name
        cursor.ensure(BasicToken.T_IDENTIFIER);

        if (cursor.currentIf(BasicToken.T_COLON)) {
            builder.append(':').append(cursor.peek().value());
            // Ensure that the current token is a function name
            cursor.ensure(BasicToken.T_IDENTIFIER);
        }

        FunctionNode function = new FunctionNode(builder.toString());

        if (cursor.isCurrent(BasicToken.T_OPEN_PAREN)) {
            context.setOptions(ParserOptions.withNextParser(ArgumentsParser.class));
            function.setArguments((Expression) context.getParser(ParenthesesParser.class).parse(cursor, context));
            context.clearOptions();
        }

        parent.add(function);
    }
}
