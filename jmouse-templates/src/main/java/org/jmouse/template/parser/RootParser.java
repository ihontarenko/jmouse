package org.jmouse.template.parser;

import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;

import static org.jmouse.template.lexer.TemplateToken.*;

public class RootParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (cursor.isNext(T_OPEN_CALL, T_OPEN_PRINT, T_OPEN_COMMENT, T_OPEN_JAVA_CODE)) {

            if (cursor.matchesSequence(T_OPEN_CALL, T_FOR)) {
                context.setOptions(ParserOptions.withStopCondition(token -> token.type() == T_END_FOR));
                context.getParser(LoopParser.class).parse(cursor, parent, context);
            }

        }
    }

}
