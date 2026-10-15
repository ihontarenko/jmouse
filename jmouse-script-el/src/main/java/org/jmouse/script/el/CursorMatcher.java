package org.jmouse.script.el;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.CursorLookahead;
import org.jmouse.script.el.lexer.ScriptToken;

import static org.jmouse.el.lexer.BasicToken.*;
import static org.jmouse.script.el.lexer.ScriptToken.*;
import static org.jmouse.script.el.lexer.StatementToken.T_DO;

/**
 * The shape of jMS's own constructions — {@code script}, {@code behaviour} and {@code on} — as a lookahead over
 * tokens. The statements written inside them are the core's, in {@link org.jmouse.script.el.StatementMatcher}.
 *
 * <p>Dispatch in the expression language is "first parser whose {@code supports} says yes, in priority
 * order", so a matcher that is merely <em>nearly</em> right is a statement quietly handed to the wrong
 * parser. Two rules keep that from happening here:</p>
 *
 * <ul>
 *   <li><strong>Every matcher is anchored.</strong> It reads from the cursor's own position outwards
 *       and never scans the rest of the file for a token it hopes to find.</li>
 *   <li><strong>Overlapping shapes are made disjoint, not ordered.</strong> Every construction but one
 *       opens with a keyword of its own; the exception is an assignment, and it is pinned by the
 *       {@code =} that has to follow its property path.</li>
 * </ul>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class CursorMatcher {

    private CursorMatcher() {
    }

    /** {@code script "slice-01" { … }} */
    public static Matcher<TokenCursor> script() {
        return new ScriptMatcher();
    }

    /** {@code behaviour "gatherer" do … end} */
    public static Matcher<TokenCursor> behaviour() {
        return new BehaviourMatcher();
    }

    /** {@code on unload [180] [when …] do … end} */
    public static Matcher<TokenCursor> handler() {
        return new HandlerMatcher();
    }

    private record ScriptMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_SCRIPT)
                    && checkAt(cursor, 1, T_STRING)
                    && checkAt(cursor, 2, T_OPEN_CURLY);
        }
    }

    private record BehaviourMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_BEHAVIOUR)
                    && checkAt(cursor, 1, T_STRING)
                    && checkAt(cursor, 2, T_DO);
        }
    }

    /**
     * A handler is {@code on} and an event name, and nothing more is looked at.
     *
     * <p>⚠️ <strong>The optional argument and the optional {@code when} are deliberately not
     * measured.</strong> A matcher only has to answer "is this a handler"; what may follow the event is
     * {@code HandlerParser}'s to enforce, and it does so with a sentence naming what it expected. A
     * matcher that measured the whole header would make a malformed one <em>stop matching</em> — it
     * would fall through to the expression parser and be reported as an unexpected {@code on} several
     * tokens earlier, which says nothing about the mistake that was actually made.</p>
     */
    private record HandlerMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_ON) && checkAt(cursor, 1, ScriptToken.nameTokens());
        }
    }

    /**
     * Reads the token at a lookahead offset, tolerating the end of the stream.
     *
     * @param cursor   the cursor to read from
     * @param offset   the lookahead offset
     * @param expected the types the token may have
     * @return {@code true} when a token is there and has one of those types
     */
    private static boolean checkAt(TokenCursor cursor, int offset, Token.Type... expected) {
        return CursorLookahead.at(cursor, offset, expected);
    }
}
