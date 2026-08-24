package org.jmouse.query.el.parser;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.query.el.node.FunctionNode;
import org.jmouse.query.el.node.ParameterDeclarationNode;

/**
 * Parses {@code function name(parameters) { … }}.
 *
 * <pre>
 *   function recent(userIds as int[], days as int : 7) {
 *     where entry[owner] in userIds and entry[created] &gt; now() - days
 *   }
 * </pre>
 *
 * <h2>⚠️ The name is {@code QueryFunctionParser}, not {@code FunctionParser}</h2>
 *
 * <p>The expression language has a {@code FunctionParser} of its own — it reads a <em>call</em>,
 * {@code count(x)} — and this reads a <em>declaration</em>. Two different classes answering to one name
 * in the same import list is a reader checking which is which every time, and eventually a registration
 * of the wrong one.</p>
 *
 * <h2>⚠️ Parameters are read here rather than by the core parser</h2>
 *
 * <p>Core's {@code ParametersParser} reads {@code name : default} and stops there — it has no notion of
 * a type, because jMT's {@code macro} never needed one. jMQ does: a type is what lets a compiler bind
 * {@code int[]} as several parameters instead of substituting text. So the {@code :} half keeps core's
 * exact meaning, and {@code as type} is read beside it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryFunctionParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.ensure(LanguageToken.T_FUNCTION);

        FunctionNode function = new FunctionNode();

        function.setName(cursor.ensure(BasicToken.T_IDENTIFIER).value());

        cursor.ensure(BasicToken.T_OPEN_PAREN);

        if (!cursor.isCurrent(BasicToken.T_CLOSE_PAREN)) {
            do {
                function.addParameter(parameter(cursor, context));
            } while (cursor.consumeIf(BasicToken.T_COMMA));
        }

        cursor.ensure(BasicToken.T_CLOSE_PAREN);
        cursor.ensure(BasicToken.T_OPEN_CURLY);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            Documents.skipBlankSpace(cursor);

            if (cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
                break;
            }

            Documents.refuseReserved(cursor);

            Token opening = cursor.peek();

            // ⚠️ Clauses and nothing else — no statements, no assignment, no loop. A body that could
            // loop would need a sandbox with timeouts and memory limits around every evaluation; refusing
            // the shape here is what keeps the language total and the sandbox out of the design.
            function.addClause(ClauseParser.parse(cursor, context), opening);
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        parent.add(function);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(LanguageToken.T_FUNCTION);
    }

    /**
     * Reads {@code name [as type[\[\]]] [: default]}.
     */
    private ParameterDeclarationNode parameter(TokenCursor cursor, ParserContext context) {
        ParameterDeclarationNode parameter = new ParameterDeclarationNode();

        parameter.setName(cursor.ensure(BasicToken.T_IDENTIFIER).value());

        if (cursor.consumeIf(org.jmouse.query.el.lexer.QueryToken.T_AS)) {
            parameter.setType(cursor.ensure(BasicToken.T_IDENTIFIER).value());

            if (cursor.consumeIf(BasicToken.T_OPEN_BRACKET)) {
                cursor.ensure(BasicToken.T_CLOSE_BRACKET);
                parameter.setCollection(true);
            }
        }

        // ⚠️ Core's meaning, unchanged: a colon introduces a DEFAULT VALUE. jMT's macro reads it the same
        // way, and one punctuation mark meaning two things across two sibling dialects is invisible until
        // somebody copies a parameter list from one into the other.
        if (cursor.consumeIf(BasicToken.T_COLON)) {
            parameter.setDefaultValue(
                    (Expression) context.getParser(ExpressionParser.class).parse(cursor, context));
        }

        return parameter;
    }
}
