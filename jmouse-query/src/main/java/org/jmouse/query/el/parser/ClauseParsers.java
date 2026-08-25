package org.jmouse.query.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.query.el.node.ClauseKind;
import org.jmouse.query.el.node.ClauseNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Every clause the language can read, by keyword.
 *
 * <h2>⚠️ Why a registry rather than a chain of {@code if}s on tokens</h2>
 *
 * <p>The clause set was open to everything <em>downstream</em> of the parse — a clause carries its own
 * {@link ClauseKind}, nothing tabulates them, and un-parsing can no longer lose one. The front half was
 * still closed: reading a clause meant matching one of five lexer tokens, so a product's own clause
 * needed a token added to this library. Editing the library in order to extend it is the one thing
 * "pluggable parts" is meant to remove.</p>
 *
 * <p>Here a clause is found by the word it is written with. The five built-in keywords keep their tokens
 * because those tokens are load-bearing elsewhere — {@code from}, {@code key} and {@code on} all appear
 * in a mapping — but nothing new needs one.</p>
 *
 * <h2>⚠️ A registered word is looked up, never guessed</h2>
 *
 * <p>An unknown word at clause position is refused with the registered ones listed. It is never read as
 * an expression and never skipped: a line nobody understood, silently ignored, is a query that returns
 * the wrong rows and says nothing.</p>
 *
 * <h2>⚠️ Registration is installation-wide, and that is deliberate</h2>
 *
 * <p>A clause is a property of the <em>language</em>, not of one parse. Two documents in one process
 * reading the same word as two different clauses would be two languages sharing a file extension — which
 * is exactly the confusion the namespace rule (a dot means it belongs to somebody else) exists to
 * prevent. So this is registered once, at startup, the way a dialect is.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ClauseParsers {

    /** How one clause reads what follows its keyword. */
    @FunctionalInterface
    public interface Reader {

        /**
         * @param cursor  positioned just after the keyword and its optional colon
         * @param context the parsers available
         * @return the clause
         */
        ClauseNode read(TokenCursor cursor, ParserContext context);
    }

    /**
     * ⚠️ Concurrent because it is read on every parse and written at startup — and a plain map read while
     * another thread is registering is not a race that shows up as an error. It shows up as a clause that
     * exists on one request and does not on the next.
     */
    private static final Map<String, Registration> REGISTERED = new ConcurrentHashMap<>();

    private ClauseParsers() {
    }

    /**
     * Adds a clause to the language.
     *
     * <p>⚠️ A word belonging to somebody other than the language carries a dot — {@code elastic.score}.
     * Unqualified words are the language's, forever, so a product's clause and a future built-in one can
     * never collide silently.</p>
     *
     * @param kind   what the clause is — its keyword, capability, order and repeatability
     * @param reader how it reads
     */
    public static void register(ClauseKind kind, Reader reader) {
        REGISTERED.put(kind.keyword(), new Registration(kind, reader));
    }

    /** Whether this word is a clause anybody has registered. */
    public static boolean knows(String keyword) {
        return REGISTERED.containsKey(keyword);
    }

    public static Optional<Reader> reader(String keyword) {
        return Optional.ofNullable(REGISTERED.get(keyword)).map(Registration::reader);
    }

    public static Optional<ClauseKind> kind(String keyword) {
        return Optional.ofNullable(REGISTERED.get(keyword)).map(Registration::kind);
    }

    /** Every keyword that would have worked — what a refusal lists. */
    public static Set<String> keywords() {
        return Set.copyOf(REGISTERED.keySet());
    }

    private record Registration(ClauseKind kind, Reader reader) {
    }
}
