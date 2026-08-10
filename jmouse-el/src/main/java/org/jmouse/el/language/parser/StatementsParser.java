package org.jmouse.el.language.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.*;

public class StatementsParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.ensure(BasicToken.T_OPEN_CURLY);

        AutodetectFirstParser parser    = (AutodetectFirstParser) context.getParser(AutodetectFirstParser.class);
        BasicNode             container = BasicNode.forToken(cursor.current());

        skipSeparators(cursor);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            if (cursor.isCurrent(BasicToken.T_HASH)) {
                skipCommentLine(cursor);
                continue;
            }

            int        position   = cursor.position();
            Expression expression = (Expression) parser.parse(cursor, context);

            if (expression == null) {
                Token current = cursor.current();
                throw new ParseException("Unexpected token %s at line: %d".formatted(
                        cursor.peek(), current.lineNumber()));
            }

            container.add(expression);

            if (cursor.position() == position) {
                throw new ParseException(
                        "Parser for JMP statement '%s' returned without consuming it at token position %d"
                                .formatted(cursor.current().value(), position)
                );
            }

            skipSeparators(cursor);
        }

        parent.add(container);

        cursor.ensure(BasicToken.T_CLOSE_CURLY);
    }

    private void skipSeparators(TokenCursor cursor) {
        while (cursor.consumeIf(BasicToken.T_NEW_LINE, BasicToken.T_SEMICOLON));
    }

    private void skipCommentLine(TokenCursor cursor) {
        while (!cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_EOL)) {
            cursor.next();
        }
        skipSeparators(cursor);
    }

}
