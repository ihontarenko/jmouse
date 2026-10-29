package org.jmouse.el.grammar;


import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.ParserContext;

/**
 * Reads a block — {@code word { line … }} — into its node: the engine's body parser, opened by the block's word. Its
 * lines are read by the {@link LinesParser} it names, which a language registers among its parsers.
 *
 * <p>⚠️ A block is never chosen by the engine's dispatch: the body that holds it asks for the parser of a word, so a
 * word of the language stays free as a name in another language's body read through the same context.</p>
 *
 * @param <N> the block's node
 */
public abstract class BlockParser<N extends BlockNode> extends AbstractBodyParser<N, Token.Type> {

    /** The word the block opens with. */
    public abstract String word();

    /** A fresh node for the block, its word already read. */
    protected abstract N node(TokenCursor cursor, ParserContext context);

    @Override
    protected final N createNode(TokenCursor cursor, ParserContext context) {
        return node(cursor, context);
    }

    /** The token the lexer reads the block's word as: a word, unless another language of the context keeps it as its own. */
    @Override
    protected Token.Type token() {
        return BasicToken.T_IDENTIFIER;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return cursor.checkValueAt(0, word()) && cursor.isNext(BasicToken.T_OPEN_CURLY);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        return (S) SpanNode.of(cursor.current().lineNumber(), cursor.current().offset());
    }
}
