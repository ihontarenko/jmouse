package org.jmouse.access.el.condition;

import org.jmouse.access.policy.ConditionMentions;
import org.jmouse.core.MimeParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
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

    private ConditionReading() {
    }

    /**
     * Reads one condition.
     *
     * <p>⚠️ Takes the tokens rather than lexing them, so that the three readings a compilation makes
     * of one condition cost one pass between them — see {@link ConditionTokens}.
     *
     * @param lexed its tokens, exactly as {@link ConditionTokens#all(String)} returned them
     * @return the names it talks about, and whether the reading was exhaustive
     */
    static ConditionMentions of(List<Token> lexed) {
        List<Token>              tokens  = ConditionTokens.significant(lexed);
        Set<String>              actions = new TreeSet<>();
        Set<String>              values  = new TreeSet<>();
        Map<String, Set<String>> paths   = new TreeMap<>();
        boolean                  certain = true;

        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);

            if (token.type() != BasicToken.T_IDENTIFIER) {
                continue;
            }

            if (isAProperty(tokens, index) || isATestName(tokens, index)) {
                continue;
            }

            String name = token.value();

            // Read whatever this name has a dot after it, whether or not the name is one anybody
            // publishes: a path is checkable on its own, against a type, and is worth knowing even
            // where the reading of the rest turns out uncertain.
            memberAfter(tokens, index).ifPresent(
                    member -> paths.computeIfAbsent(name, head -> new TreeSet<>()).add(member));

            if (ACTION.equals(name)) {
                String compared = comparedLiteral(tokens, index);

                if (compared == null) {
                    certain = false;
                } else {
                    actions.add(compared);
                }

                continue;
            }

            // ⚠️ A name with a dot after it is BOTH a value and a path, and excluding it from one
            // because it appears in the other is a silent regression: the evaluator binds the names
            // this reports as values, so `purpose.code` would have named a variable nothing bound and
            // the rule would have read null forever. Paths and values are two questions about one
            // word — is this published, and does that member exist — and only the second is answered
            // by looking at a type.
            if (!BOUND.contains(name)) {
                values.add(name);
            }
        }

        return new ConditionMentions(actions, values, certain, paths);
    }

    /**
     * The member written directly after this name, where there is one — the {@code name} in
     * {@code caller.name}.
     *
     * <p>⚠️ <strong>Only the first hop.</strong> {@code caller.owner.name} yields {@code owner} and
     * stops, because what {@code owner} is has left the vocabulary this can check. Reading further
     * would mean guessing at types, and a checker that guesses refuses rules that work.
     */
    private static Optional<String> memberAfter(List<Token> tokens, int index) {
        if (!isAt(tokens, index + 1, BasicToken.T_DOT)
            || !isAt(tokens, index + 2, BasicToken.T_IDENTIFIER)) {

            return Optional.empty();
        }

        return Optional.of(tokens.get(index + 2).value());
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
}
