package org.jmouse.el.renderable.parser.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ParameterNode;
import org.jmouse.el.node.expression.ParameterSetNode;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.sub.ParametersParser;
import org.jmouse.el.renderable.node.MacroNode;
import org.jmouse.el.renderable.parser.TemplateParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jmouse.el.lexer.BasicToken.*;
import static org.jmouse.el.renderable.lexer.TemplateToken.*;

/**
 * Parses a macro definition in a view.
 * <p>
 * This parser processes macro tags by extracting the macro name, its parameters,
 * and its body. The expected syntax is:
 * <pre>
 *   {% macro macroName(param1, param2, ...) %}
 *       ... macro body ...
 *   {% endmacro %}
 * </pre>
 * </p>
 */
public class MacroParser implements TagParser {

    /**
     * Parses a macro definition from the token stream.
     *
     * @param cursor  the token cursor for reading tokens
     * @param context the parser context for obtaining sub-parsers
     * @return a Node representing the macro definition
     */
    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        // Consume the "macro" token.
        cursor.ensure(T_MACRO);

        // Retrieve the macro name.
        TemplateParser              templateParser = (TemplateParser) context.getParser(TemplateParser.class);
        Token                       name           = cursor.ensure(T_IDENTIFIER);
        MacroNode                   macro          = new MacroNode();
        List<String>            names    = new ArrayList<>();
        Map<String, Expression> defaults = new HashMap<>();

        macro.setName(name.value());

        // Parse macro parameters.
        cursor.ensure(T_OPEN_PAREN);

        if (!cursor.isCurrent(T_CLOSE_PAREN)) {
            if (context.getParser(ParametersParser.class)
                    .parse(cursor, context) instanceof ParameterSetNode parameters) {
                for (ParameterNode parameter : parameters.getSet()) {
                    Expression defaultValue = parameter.getDefaultValue();
                    names.add(parameter.getName());
                    if (defaultValue != null) {
                        defaults.put(parameter.getName(), defaultValue);
                    }
                }
            }
        }

        cursor.ensure(T_CLOSE_PAREN);
        cursor.ensure(T_CLOSE_EXPRESSION);

        // Parse macro body until the end macro tag is reached.
        Matcher<TokenCursor> stopper = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_MACRO);
        Node                 body    = templateParser.parse(cursor, context, stopper);

        // Ensure that the "endmacro" tag is present.
        cursor.ensure(T_END_MACRO);

        // Set the parsed body and parameter names on the MacroNode.
        macro.setBody(body);
        macro.setDefaultValues(defaults);
        macro.setArguments(names);

        return macro;
    }

    /**
     * Returns the name of this tag parser.
     *
     * @return the string "macro"
     */
    @Override
    public String getName() {
        return "macro";
    }
}
