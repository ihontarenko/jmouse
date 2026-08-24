package org.jmouse.access.el.condition;

import org.jmouse.access.policy.PolicyException;
import org.jmouse.core.MimeParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Which functions a condition calls, with what — checked at load, against what is actually registered.
 *
 * <h2>⚠️ Why this has to happen at load and cannot wait for evaluation</h2>
 *
 * <p>{@code FunctionNode} does throw for a name nothing registers — but at <em>evaluation</em>, and
 * {@code ExpressionConditionCompiler.ExpressionCondition#holds} catches every {@code RuntimeException}
 * and reads it as {@code false}. So a mistyped call — {@code consumd('ai-token')} — compiles cleanly,
 * boots cleanly, and then refuses everybody on every request, leaving one warning line behind it.
 *
 * <p>{@link ConditionVocabulary} refuses a quoted index for exactly this reason and says why: <em>"an
 * authorization rule that boots clean and refuses everybody at runtime is the worst way to be
 * wrong."</em> A call nobody registered is the same failure with a different spelling.
 *
 * <h2>⚠️ And the arguments matter as much as the name</h2>
 *
 * <p>A wrong <em>name</em> at least fails loudly once this class has run. A wrong <em>argument</em>
 * fails silently forever: {@code consumed('ai-token', 'week')} where nothing records a {@code week}
 * bucket reads zero, so the rule never holds and the quota simply does not exist. Nothing throws,
 * nothing logs, and the first sign is an invoice.
 *
 * <p>So each call's literal arguments are handed to the function that will receive them, through
 * {@link AccessFunction#verifyArguments(List)}, and a function that can tell they are wrong says so
 * here rather than answering zero forever.
 *
 * <h2>⚠️ A test is not a function, and they lex identically</h2>
 *
 * <p>{@code is hasAny('DRAFT', 'LIVE')} and {@code consumed('ai-token', '3h')} are both an identifier
 * followed by a parenthesised argument list. Told apart by what comes <em>before</em>: a test name
 * follows {@code is}, optionally with a negation between them. Getting this wrong would refuse every
 * policy already using a test with arguments, which is most of them.
 *
 * <h2>What it declines to read</h2>
 *
 * <p>Anything it cannot identify exactly, it leaves alone — the same bargain {@code ConditionReading}
 * makes. A namespaced call ({@code ns:name(…)}) shares its colon with the ternary, and a call reached
 * through a property ({@code resource.thing(…)}) is a different parser's business. An argument that is
 * not a single string literal is reported as {@code null} rather than guessed at, so a function
 * checking its arguments can tell "this is wrong" from "this cannot be read from here".
 */
final class ConditionCalls {

    private ConditionCalls() {
    }

    /**
     * One call as it was written: the name, and each argument that is a plain string literal.
     *
     * @param name      the function called
     * @param arguments one entry per argument, in order; {@code null} where the argument is anything
     *                  other than a single string literal — a variable, an expression, a number
     */
    record Call(String name, List<String> arguments) {
    }

    /**
     * Refuses a condition calling something the catalogue does not carry, or calling it wrongly.
     *
     * @param source    the condition as written, for the message
     * @param tokens    its tokens, structural ones included
     * @param functions what a call may resolve to
     * @throws PolicyException naming the call and listing what would have worked
     */
    static void verify(String source, List<Token> tokens, FunctionCatalog functions, TestCatalog tests) {
        for (Call call : in(tokens)) {
            AccessFunction called = functions.find(call.name());

            if (called == null) {
                throw new PolicyException(
                        ("condition '%s' calls '%s', which nothing registers. " + describe(functions))
                                .formatted(source, call.name()));
            }

            verifyArgumentsOf(source, call, called);
        }

        verifyApplications(source, tokens, tests);
    }

    /**
     * The same check for the other half of the vocabulary: {@code x is somethingNobodyRegistered}.
     *
     * <p>⚠️ It matters more here than it does for a function. An unknown <em>function</em> throws at
     * evaluation and is read as {@code false}; an unknown <em>test</em> does the same — and a
     * {@code false} inside a {@code deny} means the deny does not hold. So a mistyped test in a denial
     * boots clean and quietly stops refusing anybody.
     */
    private static void verifyApplications(String source, List<Token> tokens, TestCatalog tests) {
        for (Call application : applied(tokens)) {
            if (ConditionDialect.builtInTestNames().contains(application.name())) {
                // A built-in the dialect chose. It declares no arguments to check.
                continue;
            }

            AccessTest applied = tests.find(application.name());

            if (applied == null) {
                throw new PolicyException(
                        ("condition '%s' applies the test '%s', which nothing registers. " + describe(tests))
                                .formatted(source, application.name()));
            }

            verifyArgumentsOf(source, application, applied);
        }
    }

    /**
     * ⚠️ The function's own complaint, quoted rather than replaced.
     *
     * <p>It knows what {@code 'week'} is wrong <em>about</em>; this class only knows where it was
     * written. Both halves are needed for somebody to fix it, so the message keeps the function's
     * sentence and adds the condition it appeared in.
     */
    private static void verifyArgumentsOf(String source, Call call, AccessFunction called) {
        try {
            called.verifyArguments(call.arguments());
        } catch (RuntimeException wrong) {
            throw new PolicyException(
                    "condition '%s' calls %s incorrectly: %s".formatted(
                            source, call.name(), wrong.getMessage()), wrong);
        }
    }

    private static void verifyArgumentsOf(String source, Call application, AccessTest applied) {
        try {
            applied.verifyArguments(application.arguments());
        } catch (RuntimeException wrong) {
            throw new PolicyException(
                    "condition '%s' applies %s incorrectly: %s".formatted(
                            source, application.name(), wrong.getMessage()), wrong);
        }
    }

    /** Every function call the condition makes, in the order a reader meets them. */
    static List<Call> in(List<Token> tokens) {
        List<Token> significant = ConditionTokens.significant(tokens);
        List<Call>  calls       = new ArrayList<>();

        for (int position = 0; position < significant.size() - 1; position++) {
            Token token = significant.get(position);

            boolean isCall = token.type() == BasicToken.T_IDENTIFIER
                             && significant.get(position + 1).type() == BasicToken.T_OPEN_PAREN;

            if (isCall && !isTestName(significant, position) && !isReached(significant, position)) {
                calls.add(new Call(token.value(), argumentsFrom(significant, position + 1)));
            }
        }

        return calls;
    }

    /**
     * The arguments of a call whose opening parenthesis is at {@code openingParen}.
     *
     * <p>Split on commas at depth one, so a nested call or a parenthesised expression stays one
     * argument rather than becoming several. An argument that is exactly one string literal is read;
     * anything else is {@code null}.
     */
    /**
     * Every test a condition applies — {@code caller is agent}, {@code now is workingHours('mon-fri 09:00-18:00')}.
     *
     * <p>The counterpart of {@link #in(List)}, and it reads the same tokens from the other side: that
     * method skips an identifier standing after {@code is} because it is a test, this one keeps only
     * those. A test may be applied bare, so the parentheses are optional here and required there.
     */
    static List<Call> applied(List<Token> tokens) {
        List<Token> significant   = ConditionTokens.significant(tokens);
        List<Call>  applications  = new ArrayList<>();

        for (int position = 0; position < significant.size(); position++) {
            Token token = significant.get(position);

            if (token.type() != BasicToken.T_IDENTIFIER || !isTestName(significant, position)) {
                continue;
            }

            boolean withArguments = position + 1 < significant.size()
                                    && significant.get(position + 1).type() == BasicToken.T_OPEN_PAREN;

            applications.add(new Call(
                    token.value(),
                    withArguments ? argumentsFrom(significant, position + 1) : List.of()));
        }

        return applications;
    }

    private static List<String> argumentsFrom(List<Token> tokens, int openingParen) {
        List<String> arguments = new ArrayList<>();
        List<Token>  current   = new ArrayList<>();
        int          depth     = 0;

        for (int position = openingParen; position < tokens.size(); position++) {
            Token token = tokens.get(position);

            if (token.type() == BasicToken.T_OPEN_PAREN) {
                depth++;

                if (depth == 1) {
                    continue;
                }
            }

            if (token.type() == BasicToken.T_CLOSE_PAREN) {
                depth--;

                if (depth == 0) {
                    addArgument(arguments, current);
                    return arguments;
                }
            }

            if (depth == 1 && token.type() == BasicToken.T_COMMA) {
                addArgument(arguments, current);
                current = new ArrayList<>();
                continue;
            }

            current.add(token);
        }

        // Unbalanced parentheses: the parser is about to say so far better than this could.
        return arguments;
    }

    /** ⚠️ Empty means "no argument here", which is different from "an argument nobody can read". */
    private static void addArgument(List<String> arguments, List<Token> written) {
        if (written.isEmpty()) {
            return;
        }

        boolean isLiteral = written.size() == 1 && written.get(0).type() == BasicToken.T_STRING;

        arguments.add(isLiteral ? MimeParser.unquote(written.get(0).value()) : null);
    }

    /**
     * Whether the identifier at {@code position} is a test's name rather than a function's.
     *
     * <p>{@code is hasAny(…)} puts {@code is} immediately before it; {@code is not hasAny(…)} puts a
     * negation in between. Both are tests, and neither belongs to the function catalogue.
     */
    private static boolean isTestName(List<Token> tokens, int position) {
        if (position == 0) {
            return false;
        }

        Token previous = tokens.get(position - 1);

        if (previous.type() == BasicToken.T_IS) {
            return true;
        }

        return previous.type() == BasicToken.T_NE
               && position > 1
               && tokens.get(position - 2).type() == BasicToken.T_IS;
    }

    /**
     * Whether the identifier is reached through something rather than called by name — a property path
     * or a namespace. Not this checker's business, and deliberately not guessed at.
     */
    private static boolean isReached(List<Token> tokens, int position) {
        if (position == 0) {
            return false;
        }

        BasicToken previous = tokens.get(position - 1).type() instanceof BasicToken type ? type : null;

        return previous == BasicToken.T_DOT || previous == BasicToken.T_COLON;
    }

    private static String describe(FunctionCatalog functions) {
        if (functions.isEmpty()) {
            return "This installation registers no condition functions at all, so a rule cannot call "
                   + "one — a product contributes them as AccessFunction beans.";
        }

        return "Known functions: " + String.join(", ", functions.all()) + ".";
    }

    private static String describe(TestCatalog tests) {
        String builtIn = "Built-in tests: " + String.join(", ", ConditionDialect.builtInTestNames()) + ".";

        if (tests.isEmpty()) {
            return builtIn + " This installation contributes no test of its own — a product adds them as "
                   + "AccessTest beans.";
        }

        return builtIn + " Contributed tests: " + String.join(", ", tests.all()) + ".";
    }
}
