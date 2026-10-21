package org.jmouse.sheet.parser;

import org.jmouse.core.Priority;
import org.jmouse.sheet.SheetSyntaxException;
import org.jmouse.sheet.node.FieldNode;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

/** {@code name value [source …]} — a line that opens no block. */
@Priority(SheetParserPriority.FIELD)
public class FieldParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        FieldNode field = new FieldNode();
        field.setSpan(SheetReading.span(cursor));
        field.setName(SheetNames.name(cursor, "a field's name"));
        if (SheetValueReading.atValueEnd(cursor)) {
            throw new SheetSyntaxException(cursor, "'" + field.getName() + "' has no value");
        }
        field.setValue(SheetValueReading.value(cursor, context));
        if (SheetReading.atSource(cursor)) {
            field.setSource(SheetReading.citation(cursor));
        }
        if (!SheetReading.atLineEnd(cursor)) {
            throw new SheetSyntaxException(cursor, "'" + cursor.lookAt(0).value() + "' follows the value of '"
                    + field.getName() + "': a value is one value, or several separated by commas, then at most its source");
        }
        parent.add(field);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return SheetReading.atWord(cursor) && !SheetReading.opensBlock(cursor);
    }
}
