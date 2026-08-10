package org.jmouse.access.el.condition;

import org.jmouse.access.policy.PolicyException;
import org.jmouse.core.MimeParser;
import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.*;

import java.util.EnumSet;
import java.util.Set;

/**
 * The tokens a condition may be written with — checked before anything is compiled.
 *
 * <p>⚠️ <strong>This exists because leaving an operator unregistered does not remove it, it makes it
 * silent.</strong> {@link ConditionDialect} registers no {@code in} operator, and the natural
 * expectation is that {@code resource.size in [1, 2, 3]} then fails to compile. It does not. The
 * expression parser stops at the token it cannot use, evaluates the part it understood, and the
 * trailing half of the rule is discarded without a word — so the rule answers, and answers about a
 * question nobody asked.</p>
 *
 * <p>An authorization rule that quietly means something other than what is written is the failure
 * this whole module is built to avoid, so the vocabulary is checked <em>lexically</em> first: every
 * token in the source must be one of the forms below, or the condition does not load. That makes the
 * refusal loud, early, and specific about the word it objected to.</p>
 *
 * <p>What is missing from the list is missing on purpose — arithmetic and {@code **}, the filter
 * pipe, {@code ++} and {@code --}, {@code ..}, {@code ~}, the lambda arrow, {@code @} bean access,
 * {@code $} placeholders, and {@code in}.</p>
 *
 * <p>One shape is refused although every token in it is allowed: a <em>quoted</em> index. See
 * {@link #verifyIndex(String, Token, Token)} — it is the one spelling that loads and then fails on
 * every request, and this is the only place that can still say so usefully.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ConditionVocabulary {

    /**
     * Every token type a condition may contain: names and property access, literals, comparison,
     * boolean composition, null-coalescing, the ternary, {@code is} tests, and grouping.
     */
    private static final Set<BasicToken> ALLOWED = EnumSet.of(
            // names and property access
            BasicToken.T_IDENTIFIER, BasicToken.T_DOT,
            // literals
            BasicToken.T_STRING, BasicToken.T_NUMERIC, BasicToken.T_INT, BasicToken.T_LONG,
            BasicToken.T_FLOAT, BasicToken.T_DOUBLE, BasicToken.T_SHORT, BasicToken.T_BYTE,
            BasicToken.T_CHARACTER, BasicToken.T_TRUE, BasicToken.T_FALSE, BasicToken.T_NULL,
            // comparison
            BasicToken.T_EQ, BasicToken.T_NE, BasicToken.T_GT, BasicToken.T_GE,
            BasicToken.T_LT, BasicToken.T_LE,
            // boolean composition, null-coalescing, the ternary
            BasicToken.T_AND, BasicToken.T_OR, BasicToken.T_NEGATE,
            BasicToken.T_NULL_COALESCE, BasicToken.T_QUESTION, BasicToken.T_COLON,
            // tests
            BasicToken.T_IS,
            // grouping and lists
            BasicToken.T_OPEN_PAREN, BasicToken.T_CLOSE_PAREN,
            BasicToken.T_OPEN_BRACKET, BasicToken.T_CLOSE_BRACKET, BasicToken.T_COMMA,
            // structural
            BasicToken.T_NEW_LINE, BasicToken.T_SOL, BasicToken.T_EOL
    );

    private static final Lexer LEXER = new DefaultLexer(
            new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer()));

    private ConditionVocabulary() {
    }

    /**
     * Refuses a condition containing anything outside the dialect.
     *
     * @param source the condition, as it was written in the file
     * @throws PolicyException naming the first word that is not allowed here
     */
    public static void verify(String source) {
        TokenCursor cursor   = LEXER.tokenize(new StringSource("CONDITION(" + source + ")", source));
        Token       previous = null;

        while (cursor.hasNext()) {
            Token token = cursor.next();

            if (token.type() instanceof BasicToken type) {
                if (!ALLOWED.contains(type)) {
                    throw new PolicyException(("condition '%s' uses '%s', which an authorization rule may not: "
                            + "a condition compares what it was given to what the file says, and nothing else")
                            .formatted(source, token.value()));
                }

                verifyIndex(source, previous, token);
            }

            previous = token;
        }
    }

    /**
     * Refuses a quoted index — {@code resource['status']} rather than {@code resource[status]}.
     *
     * <p>⚠️ <strong>The quoted form is the one shape in this dialect that survives load and fails on
     * every request.</strong> It compiles, and then the property path takes the quotes for part of
     * the key and throws where the answer was wanted. An authorization rule that boots clean and
     * refuses everybody at runtime is the worst way to be wrong, so it is refused here, at load,
     * where the message can say what to write instead.</p>
     *
     * @param source   the condition, for the message
     * @param previous the token before this one, or {@code null} at the start
     * @param token    the token being checked
     * @throws PolicyException when a string literal is being used as an index
     */
    private static void verifyIndex(String source, Token previous, Token token) {
        boolean quotedIndex = token.type() == BasicToken.T_STRING
                && previous != null && previous.type() == BasicToken.T_OPEN_BRACKET;

        if (!quotedIndex) {
            return;
        }

        throw new PolicyException(("condition '%s' quotes an index: write '[%s]' rather than '[%s]'. "
                + "The quoted form loads and then fails on every request, which is a worse answer than "
                + "this one").formatted(source, MimeParser.unquote(token.value()), token.value()));
    }
}
