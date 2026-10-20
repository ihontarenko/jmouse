package org.jmouse.datamap.parser;

import org.jmouse.datamap.DataMapSyntaxException;
import org.jmouse.datamap.lexer.DataMapToken;
import org.jmouse.datamap.node.DataMapDocumentNode;
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
 * {@code map "Title" as slug { … }} — the whole document. The comments above its first line are kept with it, and
 * nothing may follow its closing brace.
 */
public class DataMapDocumentParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.consumeIf(BasicToken.T_SOL);
        List<Trivia> above = comments(cursor);
        DataMapDocumentNode document = new DataMapDocumentNode();
        if (!cursor.isCurrent(DataMapToken.T_MAP)) {
            throw new DataMapSyntaxException(cursor, "a jMD document begins with: map \"Title\" as slug {");
        }
        document.setSpan(DataMapReading.span(cursor));
        cursor.ensure(DataMapToken.T_MAP);
        if (!cursor.isCurrent(BasicToken.T_STRING)) {
            throw new DataMapSyntaxException(cursor, "the map's title is a text in quotes");
        }
        document.setTitle(DataMapReading.text(cursor.next()));
        if (!cursor.isCurrent(DataMapToken.T_AS)) {
            throw new DataMapSyntaxException(cursor, "after the title comes: as slug");
        }
        cursor.ensure(DataMapToken.T_AS);
        document.setSlug(DataMapNames.name(cursor, "the map's slug"));
        if (!cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            throw new DataMapSyntaxException(cursor, "the header ends with the slug, then '{'");
        }
        Node body = context.getParser(StatementsParser.class).parse(cursor, context);
        for (Node statement : body.getChildren()) {
            document.addExpression((Expression) statement);
        }
        document.addLeadingTrivia(above);
        comments(cursor);
        if (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_EOL)) {
            throw new DataMapSyntaxException(cursor, "a document is one map: nothing follows its closing '}'");
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
