package org.jmouse.cad.document;

import org.jmouse.el.grammar.AuthoredText;

import org.jmouse.el.grammar.Lines;

import org.jmouse.el.grammar.LineParser;
import org.jmouse.el.grammar.WrittenLineNode;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/** A line kept whole, as written, to its end. */
public class WrittenLineParser extends LineParser<WrittenLineNode> {

    @Override
    protected WrittenLineNode line(TokenCursor cursor, ParserContext context) {
        return new WrittenLineNode(writtenToLineEnd(cursor));
    }

    /** What stands from the cursor to the end of its line, as the author wrote it; the cursor is left at the line's end. */
    static String writtenToLineEnd(TokenCursor cursor) {
        Token first = cursor.current();
        Token last = first;
        while (!Lines.lineEnd().matches(cursor)) {
            last = cursor.next();
        }
        return AuthoredText.of(cursor).written(first, last);
    }
}
