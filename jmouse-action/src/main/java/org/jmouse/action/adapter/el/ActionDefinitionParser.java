package org.jmouse.action.adapter.el;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.sub.KeyValueParser;

/**
 * EL parser that recognizes action definition expressions starting with '{@code @}'. ⚙️
 *
 * <p>
 * Supported syntax:
 * </p>
 *
 * <pre>{@code
 * @Action[name]{ key:value }
 * }</pre>
 *
 * <p>
 * Where:
 * </p>
 * <ul>
 *     <li>{@code @} marks the start of an action definition.</li>
 *     <li>{@code Action} is the action type identifier.</li>
 *     <li>{@code [name]} identifies the action instance.</li>
 *     <li>{@code {...}} is an optional configuration block parsed by {@link KeyValueParser}.</li>
 * </ul>
 *
 * <h3>Examples</h3>
 *
 * <pre>{@code
 * @Action[autoload]{'source':'user'}
 * @Action[persist]{'target':'database','mode':'insert'}
 * @Action[notify]{'channel':'email'}
 * }</pre>
 *
 * <h3>AST output</h3>
 *
 * <p>
 * This parser creates an {@link ActionDefinitionNode} and attaches it to the provided
 * {@code parent} AST node. Any key-value pairs inside the configuration block are
 * parsed and added as children of that node.
 * </p>
 *
 * <p>
 * The parser is typically registered during EL bootstrap for the action module.
 * </p>
 */
public class ActionDefinitionParser implements Parser {

    /**
     * Parses '{@code @Identifier[name]{...}}' into an {@link ActionDefinitionNode}.
     *
     * <p>
     * Grammar (simplified):
     * </p>
     *
     * <pre>{@code
     * ActionDefinition := '@' IDENTIFIER '[' IDENTIFIER ']' '{' [ KeyValueList ] '}'
     * }</pre>
     *
     * @param cursor  token cursor positioned at '{@code @}'
     * @param parent  AST parent node to attach parsed node to
     * @param context parser context used to obtain sub-parsers
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.ensure(BasicToken.T_AT);
        cursor.ensure(BasicToken.T_IDENTIFIER);

        cursor.ensure(BasicToken.T_OPEN_BRACKET);
        Node definition = new ActionDefinitionNode(
                cursor.ensure(BasicToken.T_IDENTIFIER).value()
        );
        cursor.ensure(BasicToken.T_CLOSE_BRACKET);

        cursor.ensure(BasicToken.T_OPEN_CURLY);
        if (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            context.getParser(KeyValueParser.class).parse(cursor, definition, context);
        }
        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        parent.add(definition);
    }

    /**
     * Checks whether the cursor is at the start of an action definition.
     *
     * @param cursor token cursor
     * @return {@code true} if the next tokens match '{@code @Identifier[}'
     */
    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.matchesSequence(BasicToken.T_AT, BasicToken.T_IDENTIFIER, BasicToken.T_OPEN_BRACKET);
    }

}