package org.jmouse.cad.table;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.sheet.SheetReader;
import org.jmouse.sheet.parser.EntryParser;
import org.jmouse.sheet.parser.FieldParser;

/**
 * A table's lines: the sheet language's own — a field, {@code name value [source …]}, or an entry that opens a block,
 * {@code id ["title"] [source …] { … }} —, read by its parsers in its own context ({@link SheetReader#linesContext()}), on the
 * document's cursor. The host language's context is never given them: its other lines — a component's behaviour — are not
 * the sheet's to read. One line read is one node of the table's body.
 */
public final class TableLinesParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        ParserContext sheet = SheetReader.linesContext();
        Node line = BasicNode.forToken(cursor.current());
        Parser entries = sheet.getParser(EntryParser.class);
        if (entries.supports(cursor)) {
            entries.parse(cursor, line, sheet);
        } else {
            sheet.getParser(FieldParser.class).parse(cursor, line, sheet);
        }
        parent.add(line.getFirst());
    }
}
