package org.jmouse.sheet.parser;

import org.jmouse.sheet.SheetSyntaxException;
import org.jmouse.sheet.lexer.SheetToken;
import org.jmouse.sheet.node.SheetDocumentNode;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code sheet "Title" as slug { … }} — the whole document. The comments above its first line are kept with it, and
 * nothing may follow its closing brace.
 */
public class SheetDocumentParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.consumeIf(BasicToken.T_SOL);
        List<Trivia> above = comments(cursor);
        SheetDocumentNode document = new SheetDocumentNode();
        if (!cursor.isCurrent(SheetToken.T_SHEET)) {
            throw new SheetSyntaxException(cursor, "a sheet document begins with: sheet \"Title\" as slug {");
        }
        document.setSpan(SheetReading.span(cursor));
        cursor.ensure(SheetToken.T_SHEET);
        if (!cursor.isCurrent(BasicToken.T_STRING)) {
            throw new SheetSyntaxException(cursor, "the map's title is a text in quotes");
        }
        document.setTitle(SheetReading.text(cursor.next()));
        if (!cursor.isCurrent(SheetToken.T_AS)) {
            throw new SheetSyntaxException(cursor, "after the title comes: as slug");
        }
        cursor.ensure(SheetToken.T_AS);
        document.setSlug(SheetNames.name(cursor, "the map's slug"));
        if (!cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            throw new SheetSyntaxException(cursor, "the header ends with the slug, then '{'");
        }
        Node body = context.getParser(StatementsParser.class).parse(cursor, context);
        for (Node statement : body.getChildren()) {
            document.addExpression((Expression) statement);
        }
        document.addLeadingTrivia(above);
        comments(cursor);
        if (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_EOL)) {
            throw new SheetSyntaxException(cursor, "a document is one map: nothing follows its closing '}'");
        }
        parent.add(document);
    }

    /** The comment lines from where the cursor stands, line breaks stepped over. */
    private static List<Trivia> comments(TokenCursor cursor) {
        List<Trivia> comments = new ArrayList<>();
        while (cursor.hasNext()) {
            if (cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_SEMICOLON)) {
                cursor.next();
            } else if (cursor.isCurrent(BasicToken.T_HASH)) {
                Token first = cursor.lookAt(0);
                Token last = first;
                while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_EOL)) {
                    last = cursor.next();
                }
                comments.add(Trivia.comment(SourceReading.text(cursor, first, last)));
            } else {
                break;
            }
        }
        return comments;
    }
}
