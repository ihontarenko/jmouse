package org.jmouse.cad.document;

import org.jmouse.el.grammar.Lines;

import org.jmouse.el.grammar.GrammarRefusal;
import org.jmouse.el.grammar.LineParser;
import org.jmouse.el.grammar.WrittenLineNode;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;

import java.util.List;

/**
 * A field of {@code meta}: {@code key value} — the value kept as the author wrote it —, or {@code key { line … }}, a
 * group whose lines are kept whole ({@code links { "label" "https://…" }}).
 */
public class MetaLineParser extends LineParser<MetaLineNode> {

    @Override
    protected MetaLineNode line(TokenCursor cursor, ParserContext context) {
        Token first = cursor.current();
        String key = SourceReading.touchingWord(cursor, Lines.WORD_SEPARATORS);
        // A term writes a field 'summary: "…"'.
        if (cursor.isCurrent(BasicToken.T_COLON)) {
            cursor.next();
        }
        if (cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            Node group = context.getParser(WrittenLinesParser.class).parse(cursor, context);
            List<String> lines = group.getChildren().stream().map(WrittenLineNode.class::cast).map(WrittenLineNode::written).toList();
            return new MetaLineNode(key, "", lines);
        }
        if (Lines.lineEnd().matches(cursor)) {
            throw GrammarRefusal.at(cursor, first, "a meta field is 'name value'");
        }
        return new MetaLineNode(key, WrittenLineParser.writtenToLineEnd(cursor), List.of());
    }
}
