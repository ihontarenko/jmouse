package org.jmouse.action.adapter.el;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.sub.KeyValueParser;

import static org.jmouse.el.lexer.BasicToken.*;
import static org.jmouse.el.lexer.BasicToken.T_CLOSE_CURLY;
import static org.jmouse.el.lexer.BasicToken.T_OPEN_CURLY;

/**
 * Parses action definitions like {@code @[namespace:name]{...}}. ⚙️
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * @[default:autoload]{'source':'user'}
 * @[db:persist]{'target':'users'}
 * @[notify:email]{'template':'welcome'}
 * }</pre>
 *
 * <p>
 * The bracket part contains:
 * </p>
 * <ul>
 *     <li>namespace</li>
 *     <li>action name</li>
 * </ul>
 *
 * <p>
 * The curly block is optional and parsed via {@link KeyValueParser}.
 * </p>
 */
public class ActionDefinitionParser implements Parser {

    /**
     * Parses action definition into {@link ActionDefinitionNode}.
     *
     * <pre>{@code
     * ActionDefinition :=
     *     '@' '[' Namespace ':' Name ']' '{' [ KeyValueList ] '}'
     *
     * Namespace := IDENTIFIER
     * Name      := IDENTIFIER
     * }</pre>
     *
     * @param cursor  token cursor positioned at {@code @}
     * @param parent  parent AST node
     * @param context parser context
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        ActionDefinitionNode definition = new ActionDefinitionNode();

        cursor.ensure(T_AT);
        cursor.ensure(T_OPEN_BRACKET);
        definition.setNamespace(
                cursor.ensure(T_IDENTIFIER).value()
        );
        cursor.ensure(T_COLON);
        definition.setName(
                cursor.ensure(T_IDENTIFIER).value()
        );
        cursor.ensure(BasicToken.T_CLOSE_BRACKET);

        cursor.ensure(T_OPEN_CURLY);
        if (!cursor.isCurrent(T_CLOSE_CURLY)) {
            context.getParser(KeyValueParser.class).parse(cursor, definition, context);
        }
        cursor.ensure(T_CLOSE_CURLY);

        parent.add(definition);
    }

    /**
     * Returns {@code true} if the cursor points to an action definition start.
     */
    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.matchesSequence(
                T_AT,
                T_OPEN_BRACKET,
                T_IDENTIFIER,
                T_COLON,
                T_IDENTIFIER,
                T_CLOSE_BRACKET
        );
    }

}