package org.jmouse.sheet.parser;

import org.jmouse.core.Priority;
import org.jmouse.sheet.SheetSyntaxException;
import org.jmouse.sheet.node.EntryNode;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

/**
 * {@code id ["title"] [source …] { … }} — a line that opens a block. The block's body is the engine's
 * {@link StatementsParser}, so the comments and blank lines inside it are kept with the lines they stand above.
 */
@Priority(SheetParserPriority.ENTRY)
public class EntryParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        EntryNode entry = new EntryNode();
        entry.setSpan(SheetReading.span(cursor));
        entry.setId(SheetNames.name(cursor, "an entry's id"));
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            entry.setTitle(SheetReading.text(cursor.next()));
        }
        if (SheetReading.atSource(cursor)) {
            entry.setSource(SheetReading.citation(cursor));
        }
        if (!cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            throw new SheetSyntaxException(cursor, "after an entry's id come, at most, its title in quotes and its "
                    + "source, then '{'");
        }
        Node body = context.getParser(StatementsParser.class).parse(cursor, context);
        for (Node statement : body.getChildren()) {
            entry.addExpression((Expression) statement);
        }
        parent.add(entry);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return SheetReading.atWord(cursor) && SheetReading.opensBlock(cursor);
    }
}
