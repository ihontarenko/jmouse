package org.jmouse.parallax.script.reader;

import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;

/**
 * A {@code .jmx} text: one document or more. The engine's statement reader walks the text — its statements are
 * documents, with no braces around them: it opens at the start of the text and closes at its end.
 */
final class JmxFileParser extends StatementsParser {

    private final Reading reading;

    JmxFileParser(Reading reading) {
        this.reading = reading;
    }

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        super.parse(cursor, parent, context);
        if (parent.getChildren().getLast().getChildren().isEmpty()) {
            throw reading.refusal("a jMX file holds at least one document: 'kind \"Title\" { … }'");
        }
    }

    @Override
    protected void open(TokenCursor cursor) {
        cursor.consumeIf(BasicToken.T_SOL);
    }

    @Override
    protected boolean isClosed(TokenCursor cursor) {
        return reading.atEnd();
    }

    @Override
    protected void close(TokenCursor cursor) {
    }

    @Override
    protected Expression statement(TokenCursor cursor, ParserContext context) {
        Node holder = BasicNode.forToken(cursor.current());
        context.getParser(DocumentParser.class).parse(cursor, holder, context);
        return (Expression) holder.getFirst();
    }
}
