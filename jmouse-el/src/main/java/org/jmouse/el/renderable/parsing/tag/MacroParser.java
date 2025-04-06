package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.core.CursorMatcher;
import org.jmouse.el.core.lexer.Token;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.core.node.expression.ArgumentsNode;
import org.jmouse.el.core.node.expression.PropertyNode;
import org.jmouse.el.core.parser.ParserContext;
import org.jmouse.el.core.parser.TagParser;
import org.jmouse.el.core.parser.sub.ArgumentsParser;
import org.jmouse.el.renderable.RenderableNode;
import org.jmouse.el.renderable.node.MacroNode;
import org.jmouse.el.renderable.parsing.TemplateParser;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.el.core.lexer.BasicToken.*;
import static org.jmouse.el.renderable.lexer.TemplateToken.*;

/**
 * Parses a macro definition in a template.
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
     * @return a RenderableNode representing the macro definition
     */
    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        // Consume the "macro" token.
        cursor.ensure(T_MACRO);

        // Retrieve the macro name.
        TemplateParser templateParser = (TemplateParser) context.getParser(TemplateParser.class);
        Token          macroNameToken = cursor.ensure(T_IDENTIFIER);
        MacroNode      macro          = new MacroNode();

        macro.setName(macroNameToken.value());

        // Parse macro parameters.
        cursor.ensure(T_OPEN_PAREN);
        ArgumentsNode arguments  = (ArgumentsNode) context.getParser(ArgumentsParser.class).parse(cursor, context);
        List<String>  parameters = new ArrayList<>();

        for (Node child : arguments.children()) {
            if (child instanceof PropertyNode property) {
                parameters.add(property.getPath());
            }
        }

        cursor.ensure(T_CLOSE_PAREN);
        cursor.ensure(T_CLOSE_EXPRESSION);

        // Parse macro body until the end macro tag is reached.
        Matcher<TokenCursor> stopper = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_MACRO);
        RenderableNode       body    = (RenderableNode) templateParser.parse(cursor, context, stopper);

        // Ensure that the "endmacro" tag is present.
        cursor.ensure(T_END_MACRO);

        // Set the parsed body and parameter names on the MacroNode.
        macro.setBody(body);
        macro.setArguments(parameters);

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
