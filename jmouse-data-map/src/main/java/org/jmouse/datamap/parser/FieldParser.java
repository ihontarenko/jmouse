package org.jmouse.datamap.parser;

import org.jmouse.core.Priority;
import org.jmouse.datamap.DataMapSyntaxException;
import org.jmouse.datamap.node.FieldNode;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

/** {@code name value [source …]} — a line that opens no block. */
@Priority(DataMapParserPriority.FIELD)
public class FieldParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        FieldNode field = new FieldNode();
        field.setSpan(DataMapReading.span(cursor));
        field.setName(DataMapNames.name(cursor, "a field's name"));
        if (DataMapValueReading.atValueEnd(cursor)) {
            throw new DataMapSyntaxException(cursor, "'" + field.getName() + "' has no value");
        }
        field.setValue(DataMapValueReading.value(cursor));
        if (DataMapReading.atSource(cursor)) {
            field.setSource(DataMapReading.citation(cursor));
        }
        if (!DataMapReading.atLineEnd(cursor)) {
            throw new DataMapSyntaxException(cursor, "'" + cursor.lookAt(0).value() + "' follows the value of '"
                    + field.getName() + "': a value is one value, or several separated by commas, then at most its source");
        }
        parent.add(field);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return DataMapReading.atWord(cursor) && !DataMapReading.opensBlock(cursor);
    }
}
