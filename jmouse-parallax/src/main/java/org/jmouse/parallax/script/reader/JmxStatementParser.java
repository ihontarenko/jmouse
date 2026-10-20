package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Kind;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Reads one statement of a document's braces, begun by its word: {@code parameters { … }}, {@code extends base}.
 * {@link JmxExtension} lists one for every word jMX knows.
 *
 * <p>⚠️ <strong>Never chosen by the engine's dispatch</strong> — {@link #supports} answers {@code false}. No word of
 * jMX is a lexer token: a block is a block's word at the start of a statement of a document, so {@code points},
 * {@code state} and {@code view} stay free as names inside {@code behaviour} and {@code run}, where jMS reads them
 * through the same context. {@link DocumentParser} asks these parsers by {@link #reads}, and only them.</p>
 */
final class JmxStatementParser extends AbstractParser {

    private final String keyword;
    private final Predicate<Kind> kinds;
    private final boolean block;
    private final Function<Token, DocumentStatement> reader;

    /**
     * @param kinds  the documents whose statement it reads: a graph's {@code expect} is not a circuit's
     * @param block  whether the statement is a block, which a document's kind decides whether it may hold, once unless
     *               repeatable; {@code extends}, {@code units}, {@code use} and {@code type} are not
     * @param reader reads the statement, its word already consumed
     */
    private JmxStatementParser(String keyword, Predicate<Kind> kinds, boolean block,
                               Function<Token, DocumentStatement> reader) {
        this.keyword = keyword;
        this.kinds = kinds;
        this.block = block;
        this.reader = reader;
    }

    /** A block any document may be allowed to hold. */
    static JmxStatementParser block(String keyword, Function<Token, Block> reader) {
        return block(keyword, kind -> true, reader);
    }

    /** A block read this way only in documents of some kinds. */
    static JmxStatementParser block(String keyword, Predicate<Kind> kinds, Function<Token, Block> reader) {
        return new JmxStatementParser(keyword, kinds, true,
                token -> new DocumentStatement.BlockStatement(reader.apply(token)));
    }

    /** A statement that is not a block: a document's base, its units, what it uses, a type. */
    static JmxStatementParser declaration(String keyword, Function<Token, DocumentStatement> reader) {
        return new JmxStatementParser(keyword, kind -> true, false, reader);
    }

    /** {@code meta { … }}: a block, kept apart from the others in the document. */
    static JmxStatementParser meta(Function<Token, DocumentStatement> reader) {
        return new JmxStatementParser("meta", kind -> true, true, reader);
    }

    String keyword() {
        return keyword;
    }

    boolean reads(String word, Kind kind) {
        return word.equals(keyword) && kinds.test(kind);
    }

    boolean isBlock() {
        return block;
    }

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Token word = cursor.next();
        parent.add(new DocumentStatementNode(reader.apply(word)));
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return false;
    }
}
