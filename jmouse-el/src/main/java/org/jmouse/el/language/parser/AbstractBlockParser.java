package org.jmouse.el.language.parser;

import org.jmouse.core.context.ContextScope;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;

public abstract class AbstractBlockParser<N extends ExpressionsNode, T extends Token.Type> extends AbstractParser {

    @Override
    public final void parse(TokenCursor cursor, Node parent, ParserContext context) {
        SpanNode span = span(cursor);

        cursor.ensure(token());

        N node = createNode(cursor, context);

        if (node.getSpan() == null) {
            node.setSpan(span);
        }

        try (ContextScope.ScopeToken ignored = context.getContextScope().open(scopeType())) {
            parseBlock(cursor, node, context);
            closeBlock(cursor);
            parent.add(node);
        }
    }

    /**
     * Consumes whatever terminates the block, where the body reader did not consume it itself.
     *
     * <p>A brace-delimited body is closed by the reader that opened it, so the default does nothing.
     * A word-delimited one is not: a body ending in {@code end} has to stop <em>before</em> that word,
     * because the same reader also serves a branch that stops at {@code else} instead — so the block
     * is the only thing that knows which word ends it, and this is where it says so.</p>
     *
     * @param cursor the cursor, positioned on the block's terminator
     */
    protected void closeBlock(TokenCursor cursor) {
    }

    /**
     * The parser that reads this block's statements.
     *
     * <p>⚠️ A type rather than an instance, because parsers are resolved from the context by class. A
     * dialect whose bodies are not written between braces registers its own reader and names it here;
     * everything else inherits {@link StatementsParser}.</p>
     *
     * @return the statement reader's type
     */
    protected Class<? extends Parser> statementsParser() {
        return StatementsParser.class;
    }

    protected abstract void parseBlock(TokenCursor cursor, N node, ParserContext context);

    protected abstract N createNode(TokenCursor cursor, ParserContext context);

    protected abstract T token();

    protected abstract boolean matches(TokenCursor cursor);

    protected abstract <S extends SpanNode> S span(TokenCursor cursor);

    protected Class<? extends Parser> scopeType() {
        return getClass();
    }

    @Override
    public final boolean supports(TokenCursor cursor) {
        return matches(cursor);
    }
}