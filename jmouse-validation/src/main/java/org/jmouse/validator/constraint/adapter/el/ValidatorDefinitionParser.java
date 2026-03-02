package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.sub.KeyValueParser;

/**
 * EL parser that recognizes validation definition expressions starting with '{@code @}'. 🧩
 *
 * <p>
 * Supported syntax:
 * </p>
 *
 * <pre>{@code
 * @ConstraintName(...)
 * }</pre>
 *
 * <p>
 * Where:
 * </p>
 * <ul>
 *     <li>{@code @} starts the validation definition.</li>
 *     <li>{@code ConstraintName} is an identifier used later for constraint type resolution.</li>
 *     <li>{@code (...)} is an optional key-value argument list parsed by {@link KeyValueParser}.</li>
 * </ul>
 *
 * <h3>Examples</h3>
 *
 * <pre>{@code
 * @OneOf('allowed':['ADMIN','USER'])
 * @MinMax('mode':'RANGE','min':18,'max':65,'message':'out of range')
 * @Required()
 * }</pre>
 *
 * <h3>AST output</h3>
 * <p>
 * This parser produces a {@link ValidationDefinitionNode} and attaches it to the provided {@code parent}.
 * Any key-value pairs inside parentheses are parsed and added as children of that node.
 * </p>
 *
 * <p>
 * This parser is typically registered once during bootstrap (see {@code ConstraintELModule}).
 * </p>
 */
public class ValidatorDefinitionParser implements Parser {

    /**
     * Parses '{@code @Identifier(...)}' into a {@link ValidationDefinitionNode}.
     *
     * <p>
     * Grammar (simplified):
     * </p>
     * <pre>{@code
     * ValidatorDefinition := '@' IDENTIFIER '(' [ KeyValueList ] ')'
     * }</pre>
     *
     * @param cursor  token cursor positioned at '{@code @}'
     * @param parent  AST parent node to attach parsed node to
     * @param context parser context used to obtain sub-parsers
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.ensure(BasicToken.T_AT);

        Node definitionNode = new ValidationDefinitionNode(
                cursor.ensure(BasicToken.T_IDENTIFIER).value()
        );

        cursor.ensure(BasicToken.T_OPEN_PAREN);
        if (!cursor.isCurrent(BasicToken.T_CLOSE_PAREN)) {
            context.getParser(KeyValueParser.class).parse(cursor, definitionNode, context);
        }
        cursor.ensure(BasicToken.T_CLOSE_PAREN);

        parent.add(definitionNode);
    }

    /**
     * Checks whether the cursor is at a validation definition start.
     *
     * @param cursor token cursor
     * @return {@code true} if the next tokens are '{@code @}' + identifier
     */
    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.matchesSequence(BasicToken.T_AT, BasicToken.T_IDENTIFIER);
    }

}