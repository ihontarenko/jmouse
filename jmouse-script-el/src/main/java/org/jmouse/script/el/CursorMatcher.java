package org.jmouse.script.el;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.CursorLookahead;
import org.jmouse.script.el.lexer.ScriptToken;

import static org.jmouse.el.lexer.BasicToken.*;
import static org.jmouse.script.el.lexer.ScriptToken.*;

/**
 * The shape of every {@code .jms} construction, as a lookahead over tokens.
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

    /** Returned by the scanners here for "this is not that shape". */
    private static final int NO_MATCH = CursorLookahead.NO_MATCH;

    private CursorMatcher() {
    }

    /** {@code include 'common.jms'} */
    public static Matcher<TokenCursor> include() {
        return new IncludeMatcher();
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

    /** {@code function overdue(entry) … end} */
    public static Matcher<TokenCursor> function() {
        return new FunctionMatcher();
    }

    /** {@code if … then … end} */
    public static Matcher<TokenCursor> branch() {
        return new BranchMatcher();
    }

    /** {@code for entry in … do … end} */
    public static Matcher<TokenCursor> loop() {
        return new LoopMatcher();
    }

    /** {@code local slot = …} */
    public static Matcher<TokenCursor> local() {
        return new LocalMatcher();
    }

    /** {@code return} — with or without a value. */
    public static Matcher<TokenCursor> returned() {
        return new ReturnMatcher();
    }

    /** {@code entry.state = 'moving'} */
    public static Matcher<TokenCursor> assignment() {
        return new AssignmentMatcher();
    }

    private record IncludeMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_INCLUDE) && checkAt(cursor, 1, T_STRING);
        }
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

    private record FunctionMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, LanguageToken.T_FUNCTION)
                    && checkAt(cursor, 1, ScriptToken.nameTokens())
                    && checkAt(cursor, 2, T_OPEN_PAREN);
        }
    }

    private record BranchMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, LanguageToken.T_IF);
        }
    }

    /**
     * A loop is pinned by its {@code in}, which is what tells {@code for entry in …} from any other
     * shape a body might hold — there being no C-style {@code for} in this language to confuse it with.
     */
    private record LoopMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_FOR)
                    && checkAt(cursor, 1, ScriptToken.nameTokens())
                    && checkAt(cursor, 2, T_IN);
        }
    }

    private record LocalMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_LOCAL) && checkAt(cursor, 1, ScriptToken.nameTokens());
        }
    }

    private record ReturnMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_RETURN);
        }
    }

    /**
     * An assignment is a property path and an {@code =}.
     *
     * <p>⚠️ <strong>It matches {@code ==} as well, and that is on purpose.</strong> The two spellings
     * are one token type — see {@link #assignsAt} — so a matcher that admitted only the first would let
     * {@code entry.state == 'moving'} fall through to the expression parser, which reads it as a
     * comparison, evaluates it, throws the answer away and reports nothing. A typed {@code ==} where an
     * assignment was meant is the single most likely mistake in this language, and a script that
     * silently stops moving on minute forty is the failure this line exists to prevent. Matching both
     * and refusing the wrong one in {@code AssignmentParser} turns it into a sentence at load.</p>
     */
    private record AssignmentMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            int path = propertyPathLength(cursor, 0);

            return path != NO_MATCH && checkAt(cursor, path, T_EQ);
        }
    }

    /**
     * Measures a property path — {@code entry}, {@code entry.state}, {@code building.owner.id}.
     *
     * @param cursor the cursor to read from
     * @param start  the lookahead offset the path is expected to begin at
     * @return how many tokens it occupies, or {@link #NO_MATCH}
     */
    private static int propertyPathLength(TokenCursor cursor, int start) {
        return CursorLookahead.joinedNameLength(cursor, start, T_DOT, ScriptToken.nameTokens());
    }

    /**
     * Whether the token at an offset is an assignment rather than an equality test.
     *
     * <p>⚠️ <strong>Read by value, and it has to be.</strong> {@link BasicToken#T_EQ} answers to
     * {@code ==}, {@code =}, {@code eq} and {@code equals} alike, so the two spellings arrive as one
     * token <em>type</em> and are told apart only by the characters that were written. Every dialect on
     * this lexer that binds a name — {@code .jmm}'s {@code let} among them — gets away without asking,
     * because a keyword in front of the {@code =} settles it. An assignment has no keyword, so this is
     * the one place the question has to be put to the text itself, and it is asked here once rather
     * than at each site that would otherwise have to remember to.</p>
     *
     * @param cursor the cursor to read from
     * @param offset the lookahead offset
     * @return {@code true} when a single {@code =} was written there
     */
    public static boolean assignsAt(TokenCursor cursor, int offset) {
        Token token = cursor.lookAt(offset);

        return token != null && token.type() == T_EQ && "=".equals(token.value());
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
