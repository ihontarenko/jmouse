package org.jmouse.el.grammar;

import org.jmouse.el.lexer.BasicToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;

/**
 * A block said as data: its word, the grammar of what stands after it before its braces ({@code expect "title" from
 * "source"}, {@code symbol uses and-gate-symbol}, {@code analysis transient for 80 ms}), whether it has a body, and the
 * statements its body may hold ({@link LineSet}) — lines by their grammars, formulas, blocks nested in it. A module says
 * its blocks this way; none of them is a parser class of its own.
 *
 * <p>It is a {@link LineReader} too, so a block may stand in another's body: a {@code bench} inside an {@code expect}.</p>
 */
public class ClauseBlockParser extends BlockParser<ClauseBlockNode> implements LineReader {

    /** Whether a block is opened with braces. */
    public enum Body {
        /** never: its head says all of it — {@code footprint soic-8-footprint} */
        NONE,
        /** when it has more to say — {@code symbol uses diode-symbol}, or with its bindings in braces */
        OPTIONAL,
        /** always */
        REQUIRED
    }

    private final String           word;
    private final ClauseLineParser header;
    private final Body             body;
    private final LineSet          lines;

    /**
     * @param header the grammar of its head, word-less, or null for a block whose word is followed by its braces
     * @param lines  what its body holds, or null for a block with none
     */
    public ClauseBlockParser(String word, LineGrammar header, Body body, LineSet lines) {
        this.word = word;
        this.header = header == null ? null : new ClauseLineParser(header);
        this.body = body;
        this.lines = lines;
    }

    /** Whether its word is followed by a head before its braces. */
    public boolean headed() {
        return header != null;
    }

    /** What its body may hold, or null for a block with none. */
    public LineSet lines() {
        return lines;
    }

    /** The grammar of its head, word-less; empty for a block whose word is followed by its braces. */
    public Optional<LineGrammar> header() {
        return Optional.ofNullable(header).map(ClauseLineParser::grammar);
    }

    /** Whether it is opened with braces: never, when it has more to say, or always. */
    public Body body() {
        return body;
    }

    /** {@code word { statement … }}: a block that is its body. */
    public static ClauseBlockParser of(String word, LineSet lines) {
        return new ClauseBlockParser(word, null, Body.REQUIRED, lines);
    }

    /** {@code word head}: a block that is its head, a line of its own in a body. */
    public static ClauseBlockParser headed(String word, LineGrammar header) {
        return new ClauseBlockParser(word, header, Body.NONE, null);
    }

    @Override
    public String word() {
        return word;
    }

    @Override
    public String says() {
        return word;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        int prefixes = header == null ? 0 : header.grammar().prefixesAt(cursor);
        return opensTerm(cursor) || cursor.checkValueAt(prefixes, word)
                && (header != null || cursor.lookAt(prefixes + 1).type() == BasicToken.T_OPEN_CURLY);
    }

    /** {@code expect("title", from: "source") { … }}, {@code analysis(transient, for: 80 ms)}: the block as a term. */
    @Override
    public boolean opensTerm(TokenCursor cursor) {
        return header != null && TermReading.opens(cursor, header.grammar().termWord());
    }

    @Override
    public Expression readTerm(TokenCursor cursor, ParserContext context) {
        return (Expression) parse(cursor, context);
    }

    @Override
    public boolean opens(TokenCursor cursor) {
        return supports(cursor);
    }

    @Override
    public Expression read(TokenCursor cursor, ParserContext context) {
        return (Expression) parse(cursor, context);
    }

    @Override
    protected ClauseBlockNode node(TokenCursor cursor, ParserContext context) {
        Token opening = cursor.lookAt(-1);
        if (header == null) {
            return new ClauseBlockNode(word, null);
        }
        // The engine's block parser has read the first word: a prefix — 'live' of 'live glyph …' — or the block's own.
        List<String> prefixes = new ArrayList<>();
        while (!opening.value().equals(word) && !opening.value().equals(header.grammar().termWord())
                && header.grammar().prefixes().contains(opening.value())) {
            prefixes.add(opening.value());
            opening = cursor.next();
        }
        boolean term = (cursor.isCurrent(BasicToken.T_OPEN_PAREN) || cursor.isCurrent(BasicToken.T_COLON))
                && SourceReading.touches(cursor, opening, cursor.current());
        return new ClauseBlockNode(word, term ? header.headerTerm(cursor, context, opening)
                : header.header(cursor, context, opening, prefixes));
    }

    @Override
    protected void parseBlock(TokenCursor cursor, ClauseBlockNode node, ParserContext context) {
        boolean braces = cursor.isCurrent(BasicToken.T_OPEN_CURLY);
        if (braces && body == Body.NONE) {
            throw GrammarRefusal.here(cursor, "'" + word + "' says all of it on its line; it has no '{ … }'");
        }
        if (!braces) {
            if (body == Body.REQUIRED) {
                throw GrammarRefusal.here(cursor, "'" + word + "' is followed by '{ … }'");
            }
            return;
        }
        node.open();
        super.parseBlock(cursor, node, context);
    }

    @Override
    protected Node parseStatements(TokenCursor cursor, ParserContext context) {
        return new LineSetStatements(lines).parse(cursor, context);
    }
}
