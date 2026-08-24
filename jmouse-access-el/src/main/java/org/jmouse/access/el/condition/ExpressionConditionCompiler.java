package org.jmouse.access.el.condition;

import org.jmouse.access.CallerView;
import org.jmouse.access.PlaceView;
import org.jmouse.access.spi.ConditionFunctionFailure;
import org.jmouse.access.spi.DeferredValue;
import org.jmouse.access.spi.GrantCondition;
import org.jmouse.access.policy.ConditionCompiler;
import org.jmouse.access.policy.ConditionMentions;
import org.jmouse.access.spi.ConditionContext;
import org.jmouse.access.policy.PolicyException;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.node.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Compiles a policy condition through jMouse EL, in the restricted dialect and nothing wider.
 *
 * <p>⚠️ <strong>It builds its own language.</strong> Reaching for the shared singleton would hand a
 * policy file the full dialect — bean access, {@code class(…)}, {@code set(…)}, {@code in} — which
 * is the one thing {@link ConditionDialect} exists to prevent. A condition is a rule about
 * authorization; it gets the vocabulary of one.</p>
 *
 * <p>Compilation happens <strong>once, at load</strong>. Three things follow, all of them wanted: a
 * request never parses an expression, a bad expression fails the boot with the line that carries it,
 * and the text somebody wrote survives for the control room to show back.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ExpressionConditionCompiler implements ConditionCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionConditionCompiler.class);

    private final ExpressionLanguage expressionLanguage;
    private final FunctionCatalog    functions;
    private final TestCatalog        tests;

    /** An installation contributing nothing: the dialect exactly as it was before any of this existed. */
    public ExpressionConditionCompiler() {
        this(FunctionCatalog.empty());
    }

    /**
     * The ordinary wiring: the dialect, plus whatever this product registered as an
     * {@link AccessFunction}.
     *
     * <p>⚠️ The catalogue is kept as well as being built into the language, because
     * {@link ConditionVocabulary} has to refuse an <em>unregistered</em> name at load. Without it, a
     * mistyped call compiles, boots, and then reads as {@code false} on every request — see
     * {@link #compile(String)}.
     */
    public ExpressionConditionCompiler(FunctionCatalog functions) {
        this(functions, TestCatalog.empty());
    }

    /**
     * The full wiring: the dialect, plus whatever this product registered as an {@link AccessFunction}
     * and as an {@link AccessTest}.
     *
     * <p>Both catalogues are kept as well as being built into the language, for the same reason: an
     * unregistered <em>name</em> has to be refused at load. A mistyped call and a mistyped test both
     * compile, boot, and then read as {@code false} on every request — and a {@code false} inside a
     * {@code deny} permits.
     */
    public ExpressionConditionCompiler(FunctionCatalog functions, TestCatalog tests) {
        this.functions          = functions == null ? FunctionCatalog.empty() : functions;
        this.tests              = tests == null ? TestCatalog.empty() : tests;
        this.expressionLanguage = new ExpressionLanguage(restrictedExtensions(this.functions, this.tests));
    }

    public ExpressionConditionCompiler(ExpressionLanguage expressionLanguage) {
        this(expressionLanguage, FunctionCatalog.empty());
    }

    public ExpressionConditionCompiler(ExpressionLanguage expressionLanguage, FunctionCatalog functions) {
        this(expressionLanguage, functions, TestCatalog.empty());
    }

    public ExpressionConditionCompiler(
            ExpressionLanguage expressionLanguage, FunctionCatalog functions, TestCatalog tests) {

        this.expressionLanguage = expressionLanguage;
        this.functions          = functions == null ? FunctionCatalog.empty() : functions;
        this.tests              = tests == null ? TestCatalog.empty() : tests;
    }

    /**
     * ⚠️ <strong>Lexed once for all three questions asked of it.</strong> The vocabulary check, the
     * name reading and the parser each need this condition's tokens, and only the parser's are cached
     * — so a pass apiece meant two thirds of the work was redone every time, for an answer that could
     * not have differed between them. See {@link ConditionTokens}.
     */
    @Override
    public GrantCondition compile(String source) {
        if (source == null || source.isBlank()) {
            throw new PolicyException("a condition cannot be empty");
        }

        List<Token> tokens = ConditionTokens.all(source);

        ConditionVocabulary.verify(source, tokens);

        // ⚠️ Before compiling, not during evaluation. FunctionNode does throw for an unregistered name,
        // but `holds` reads any exception as false — so an unchecked typo boots clean and then refuses
        // everybody, forever, with a log line. See ConditionCalls.
        ConditionCalls.verify(source, tokens, functions, tests);

        try {
            // ⚠️ The names are read here and kept, not read again per decision. They are what makes a
            // deferred value cost nothing: a rule binds the names it mentions and no others, so a
            // supplier behind a name this rule never writes is never asked.
            return new ExpressionCondition(
                    source, expressionLanguage, expressionLanguage.compile(source), mentionsOf(source, tokens));
        } catch (RuntimeException exception) {
            throw new PolicyException(
                    "condition '%s' will not compile: %s".formatted(source, exception.getMessage()), exception);
        }
    }

    /**
     * Which action and value names this condition talks about, read straight off the tokens.
     *
     * <p>⚠️ <strong>Off the tokens, not off the compiled tree, and that is not laziness.</strong>
     * What a checker needs is the names a person <em>wrote</em>; a tree has already turned
     * {@code action == 'x'} into an operator node whose operands are of no particular type, and
     * walking it would be a second traversal of a grammar this class exists to own once. The lexer is
     * already the thing {@link ConditionVocabulary} trusts to say what a condition is made of.
     *
     * <p>⚠️ <strong>Anything it cannot read exactly, it declines to read at all.</strong> An
     * {@code action} compared against something other than a string literal — a ternary, another
     * variable — makes the answer {@linkplain ConditionMentions#unknown() uncertain}, which switches
     * the pair check off for that rule instead of guessing. A checker that guessed would refuse rules
     * that work, and being refused for a rule that is correct is how a validator comes to be turned
     * off entirely.
     */
    @Override
    public ConditionMentions mentions(String source) {
        if (source == null || source.isBlank()) {
            return ConditionMentions.unknown();
        }

        try {
            return mentionsOf(source, ConditionTokens.all(source));
        } catch (RuntimeException unlexable) {
            LOGGER.debug("The condition `{}` could not be lexed for names: {}", source, unlexable.toString());

            return ConditionMentions.unknown();
        }
    }

    /**
     * The same reading, over tokens somebody has already paid for.
     *
     * <p>⚠️ It swallows a failure into {@linkplain ConditionMentions#unknown() uncertain} rather than
     * letting it out, and the caller in {@link #compile(String)} depends on that: a condition that
     * compiles but cannot be <em>read</em> is a rule that works with its pair check switched off, not
     * a rule that fails to load.
     */
    private ConditionMentions mentionsOf(String source, List<Token> tokens) {
        try {
            return ConditionReading.of(tokens);
        } catch (RuntimeException unreadable) {
            LOGGER.debug("The condition `{}` could not be read for names: {}", source, unreadable.toString());

            return ConditionMentions.unknown();
        }
    }

    private static ExtensionContainer restrictedExtensions(FunctionCatalog functions, TestCatalog tests) {
        StandardExtensionContainer container = new StandardExtensionContainer();
        container.importExtension(new ConditionDialect(functions, tests));
        return container;
    }

    /**
     * One compiled condition, and the things it is allowed to see.
     *
     * <p>The caller, the place and the resource are bound as variables per evaluation, and so are the
     * action and every value the call published. Nothing else is reachable: a predicate that could
     * read a repository or a clock would be a predicate whose answer depends on something the file
     * does not mention, and a rule nobody can read from its own text is worse than no rule.</p>
     *
     * <h2>⚠️ A published value is bound by its own name</h2>
     *
     * <p>{@code purpose}, not {@code values.purpose}. A rule is read by somebody administering an
     * installation, and one extra level of indirection is one more thing they have to be told about a
     * notation that should read like a sentence.
     *
     * <p>What that costs is that a value called {@code place} would cover the scope, so the three
     * bound names are written <em>after</em> the bag and win. A route publishing one of them is a
     * route publishing a value no rule can reach, and the startup check is where that gets said.</p>
     *
     * <h2>⚠️ Why it is {@code caller} and not {@code subject}</h2>
     *
     * <p>Because {@code subject} is a keyword of the language these conditions are written inside, and
     * a variable named after a keyword is a variable nobody can use: it lexes as {@code T_SUBJECT}
     * before anything looks at what it means, and the file then fails to <em>parse</em> over a word
     * that is not a keyword at that position at all.
     *
     * <p>{@code caller} is also the truer word. What is bound here is who is <em>asking</em>;
     * {@code subject} in this grammar is who a block of grants is <em>about</em>, and the two are the
     * same only by coincidence — never for an agent, and never for an impersonated session.</p>
     */
    private record ExpressionCondition(
            String             source,
            ExpressionLanguage expressionLanguage,
            Expression         compiled,
            ConditionMentions  mentions
    ) implements GrantCondition {

        /**
         * ⚠️ <strong>A condition that blows up has not answered — and "not answered" is not "no".</strong>
         *
         * <p>The evaluator can still fail on data a file could not anticipate: a member the row does not
         * have, a null halfway down a path. Letting that out would turn an authorization rule into a 500
         * on a route that would otherwise have worked, so it is caught — but it is caught and
         * <strong>re-thrown as a {@link ConditionFunctionFailure}</strong>, not flattened.
         *
         * <p>⚠️ It used to answer {@code false}, and the argument for that was that this axis may only
         * narrow, so a false could cost a permission and never hand one out. <strong>That argument is
         * wrong, and {@code JMF-5} had already refuted it for functions:</strong> a {@code false}
         * refuses a conditional <em>allow</em> and <em>permits</em> a conditional <em>deny</em>. So
         * {@code resource.deletdAt is olderThan('30d')} — one letter short of a real member — made the
         * whole condition false and the retention rule quietly stopped refusing anybody. One warn line,
         * no failing test.
         *
         * <p>No boolean is safe in both positions, which is the entire reason
         * {@link ConditionFunctionFailure} exists. It carries the failure to {@code ConditionAxis} — the
         * only place that knows whether this condition is attached to an allow or to a deny — which
         * applies the deny and drops the allow. <strong>Both refuse.</strong>
         *
         * <p>⚠️ An installation carrying a broken rule that has been quietly permitting will start
         * refusing. That is the point, and it is the only honest direction; the log line names the rule
         * so it can be fixed in one reading.
         */
        @Override
        public boolean holds(ConditionContext context) {
            try {
                EvaluationContext evaluation = expressionLanguage.newContext();

                bindPublishedValues(evaluation, context);

                // The decision itself, for the functions and tests this rule calls — under a key no
                // rule can name. Everything a policy is allowed to see is published below, as a view.
                ConditionBinding.bind(evaluation, context);

                // ⚠️ Views rather than the engine's own records. `Subject` carries `description` and
                // has never carried `name`, so binding it directly made `caller.name` a rule that
                // loads clean and never holds. A view turns the members into a declared vocabulary,
                // which is what lets a typo be refused at load instead of discovered in production.
                evaluation.setValue("caller", CallerView.of(context.subject()));
                evaluation.setValue("place", PlaceView.of(context.place()));
                evaluation.setValue("resource", context.resource());
                evaluation.setValue("action", context.action());

                Boolean holds = evaluation.getConversion()
                        .convert(compiled.evaluate(evaluation), Boolean.class);

                return Boolean.TRUE.equals(holds);
            } catch (ConditionFunctionFailure unanswerable) {
                throw unanswerable;
            } catch (RuntimeException failed) {
                // ⚠️ A rule that BLEW UP has not answered `false` — it has not answered at all, and the
                // two are opposite readings inside a deny. Reading it as `false` used to mean a
                // misspelled member such as `resource.deletdAt` made the whole condition false, so the
                // denial quietly stopped refusing anybody, with one warn line and no failing test.
                //
                // Sending it on as a ConditionFunctionFailure hands the decision to ConditionAxis, the
                // only place that knows whether this is an allow or a deny: it applies the deny and
                // drops the allow. Both refuse. Same bargain JMF-5 made for a function that throws —
                // this is the seam that was still flattening it for everything else.
                LOGGER.warn("The condition `{}` could not be evaluated and is being refused rather than "
                            + "read as false: {}", source, failed.toString());

                throw new ConditionFunctionFailure(describe(), false, failed);
            }
        }

        /**
         * What to call this condition when it could not be evaluated at all.
         *
         * <p>{@link ConditionFunctionFailure} was written for a named function, and this failure has no
         * name to give — it is the expression itself that broke. The source is what a person can act on,
         * trimmed so a refusal stays a sentence.
         */
        private String describe() {
            String written = source.replaceAll("\\s+", " ").trim();

            return written.length() <= 60 ? written : written.substring(0, 57) + "…";
        }

        /**
         * Binds what the call published — <strong>only the names this rule actually writes</strong>.
         *
         * <p>⚠️ This is what makes {@link org.jmouse.access.spi.DeferredValue} worth having. Binding
         * the whole bag would resolve every deferred value on every decision, which is precisely the
         * query-per-call this rule language is careful to avoid; binding the mentioned names resolves
         * what the rule reads and nothing else.
         *
         * <p>⚠️ <strong>An unreadable rule binds everything.</strong> Where the source could not be
         * reduced to a set of names — a value reached through something the reader cannot follow —
         * there is no safe subset, so the whole bag is bound and any laziness in it is paid for. That
         * is the correct trade: a rule that works and costs a query beats a rule that quietly stops
         * seeing a value.
         */
        private void bindPublishedValues(EvaluationContext evaluation, ConditionContext context) {
            Map<String, Object> published = context.values();

            if (!mentions.isCheckable()) {
                DeferredValue.resolveAll(published).forEach(evaluation::setValue);
                return;
            }

            mentions.values().stream()
                    .filter(published::containsKey)
                    .forEach(name -> evaluation.setValue(
                            name, DeferredValue.resolve(published.get(name))));
        }

        @Override
        public String source() {
            return source;
        }
    }
}
