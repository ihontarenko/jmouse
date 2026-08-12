package org.jmouse.access.el.condition;

import org.jmouse.access.policy.ConditionMentions;
import org.jmouse.core.MimeParser;
import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionRecognizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reads the names out of a condition, so a rule can be checked before anybody runs it.
 *
 * <p>Answers the two questions {@link ConditionMentions} exists for: <em>which actions does this rule
 * bound itself to</em>, and <em>which published values does it read</em>. Together they catch the one
 * mistake name-checking cannot — a rule naming a real action and a real value that no single call
 * ever carries at once, which never fires and says nothing.
 *
 * <h2>What is read, and what makes it give up</h2>
 *
 * <p>An action is taken from a comparison against a string literal, written either way round:
 * {@code action == 'entry.list'} and {@code 'entry.list' == action}. ⚠️ Anything else {@code action}
 * appears in — a ternary, a comparison against another name, a bare truth test — makes the whole
 * reading <strong>uncertain</strong>, which switches the pair check off for that rule. Refusing a rule
 * because a checker could not follow it is how a checker comes to be switched off entirely.
 *
 * <p>A value is any other bare name the condition reads. Three things are deliberately not values:
 * the names the evaluator binds itself ({@code caller}, {@code place}, {@code resource}), anything
 * after a dot, which is a property of one of those rather than a name of its own, and anything after
 * {@code is}, which is the name of a test.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class ConditionReading {

    /** What the evaluator binds itself — see {@code ExpressionConditionCompiler.ExpressionCondition}. */
    private static final Set<String> BOUND = Set.of("caller", "place", "resource");

    /** The member a rule bounds itself with, rather than a value anybody published. */
    private static final String ACTION = "action";

    private static final Lexer LEXER = new DefaultLexer(
            new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer()));

    private ConditionReading() {
    }

    /**
     * Reads one condition.
     *
     * @param source the condition, as it was written in the file
     * @return the names it talks about, and whether the reading was exhaustive
     */
    static ConditionMentions of(String source) {
        List<Token> tokens  = tokensOf(source);
        Set<String> actions = new TreeSet<>();
        Set<String> values  = new TreeSet<>();
        boolean     certain = true;

        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);

            if (token.type() != BasicToken.T_IDENTIFIER) {
                continue;
            }

            if (isAProperty(tokens, index) || isATestName(tokens, index)) {
                continue;
            }

            String name = token.value();

            if (ACTION.equals(name)) {
                String compared = comparedLiteral(tokens, index);

                if (compared == null) {
                    certain = false;
                } else {
                    actions.add(compared);
                }

                continue;
            }

            if (!BOUND.contains(name)) {
                values.add(name);
            }
        }

        return new ConditionMentions(actions, values, certain);
    }

    /**
     * The string literal this name is compared against, either way round, or null where it is not
     * compared against one at all.
     */
    private static String comparedLiteral(List<Token> tokens, int index) {
        if (isEquality(tokens, index + 1) && isText(tokens, index + 2)) {
            return MimeParser.unquote(tokens.get(index + 2).value());
        }

        if (isEquality(tokens, index - 1) && isText(tokens, index - 2)) {
            return MimeParser.unquote(tokens.get(index - 2).value());
        }

        return null;
    }

    /**
     * ⚠️ {@code !=} counts as well as {@code ==}, and it has to.
     *
     * <p>The rule this whole mechanism was built for is {@code action == 'x' and purpose != 'HOLDER'},
     * but nothing stops somebody writing {@code action != 'entry.list'} — and a reading that ignored
     * it would call the rule unscoped, which switches the check off exactly where a rule is most
     * likely to be wrong.
     */
    private static boolean isEquality(List<Token> tokens, int index) {
        return isAt(tokens, index, BasicToken.T_EQ) || isAt(tokens, index, BasicToken.T_NE);
    }

    private static boolean isText(List<Token> tokens, int index) {
        return isAt(tokens, index, BasicToken.T_STRING);
    }

    /** Whether this name is a property of something else — the {@code status} in {@code resource.status}. */
    private static boolean isAProperty(List<Token> tokens, int index) {
        return isAt(tokens, index - 1, BasicToken.T_DOT);
    }

    /** Whether this name is a test's — the {@code empty} in {@code resource is empty}. */
    private static boolean isATestName(List<Token> tokens, int index) {
        return isAt(tokens, index - 1, BasicToken.T_IS);
    }

    private static boolean isAt(List<Token> tokens, int index, BasicToken type) {
        return index >= 0 && index < tokens.size() && tokens.get(index).type() == type;
    }

    /** Every token of the condition, structural ones dropped so lookahead can count on adjacency. */
    private static List<Token> tokensOf(String source) {
        TokenCursor cursor = LEXER.tokenize(new StringSource("CONDITION(" + source + ")", source));
        List<Token> tokens = new ArrayList<>();

        while (cursor.hasNext()) {
            Token token = cursor.next();

            if (token.type() == BasicToken.T_NEW_LINE
                || token.type() == BasicToken.T_SOL
                || token.type() == BasicToken.T_EOL) {

                continue;
            }

            tokens.add(token);
        }

        return tokens;
    }
}
