package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

/** A {@code .jmx} text: one document or more, each added to the parent as a {@link DocumentNode}. */
final class JmxFileParser extends AbstractParser {

    private final Reading reading;

    JmxFileParser(Reading reading) {
        this.reading = reading;
    }

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        reading.skipBlank();
        DocumentParser documents = (DocumentParser) context.getParser(DocumentParser.class);
        while (!reading.atEnd()) {
            documents.parse(cursor, parent, context);
            reading.skipBlank();
        }
        if (parent.getChildren().isEmpty()) {
            throw reading.refusal("a jMX file holds at least one document: 'kind \"Title\" { … }'");
        }
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return false;
    }
}
