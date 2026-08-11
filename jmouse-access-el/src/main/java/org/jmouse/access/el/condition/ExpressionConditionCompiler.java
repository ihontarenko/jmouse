package org.jmouse.access.el.condition;

import org.jmouse.access.spi.GrantCondition;
import org.jmouse.access.policy.ConditionCompiler;
import org.jmouse.access.spi.ConditionContext;
import org.jmouse.access.policy.PolicyException;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.node.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public ExpressionConditionCompiler() {
        this(new ExpressionLanguage(restrictedExtensions()));
    }

    public ExpressionConditionCompiler(ExpressionLanguage expressionLanguage) {
        this.expressionLanguage = expressionLanguage;
    }

    @Override
    public GrantCondition compile(String source) {
        if (source == null || source.isBlank()) {
            throw new PolicyException("a condition cannot be empty");
        }

        ConditionVocabulary.verify(source);

        try {
            return new ExpressionCondition(source, expressionLanguage, expressionLanguage.compile(source));
        } catch (RuntimeException exception) {
            throw new PolicyException(
                    "condition '%s' will not compile: %s".formatted(source, exception.getMessage()), exception);
        }
    }

    private static ExtensionContainer restrictedExtensions() {
        StandardExtensionContainer container = new StandardExtensionContainer();
        container.importExtension(new ConditionDialect());
        return container;
    }

    /**
     * One compiled condition, and the three things it is allowed to see.
     *
     * <p>The caller, the place and the resource are bound as variables per evaluation. Nothing else
     * is reachable: a predicate that could read a repository or a clock would be a predicate whose
     * answer depends on something the file does not mention, and a rule nobody can read from its own
     * text is worse than no rule.</p>
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
            String source, ExpressionLanguage expressionLanguage, Expression compiled
    ) implements GrantCondition {

        /**
         * ⚠️ <strong>A condition that blows up answers "no", and says so in the log.</strong>
         *
         * <p>The evaluator can still fail on data a file could not anticipate — a property the row
         * does not have, a null halfway down a path — and the two ways of handling that are not
         * equally bad. Letting it out turns an authorization rule into a 500 on a route that would
         * otherwise have worked; answering {@code false} costs one decision and leaves a line naming
         * the rule that caused it.
         *
         * <p>Refusing is only the safe half of that trade because of <em>where</em> this runs: an
         * axis that may narrow and never widen. A false here can cost somebody a permission and can
         * never hand one out.
         */
        @Override
        public boolean holds(ConditionContext context) {
            try {
                EvaluationContext evaluation = expressionLanguage.newContext();

                evaluation.setValue("caller", context.subject());
                evaluation.setValue("place", context.place());
                evaluation.setValue("resource", context.resource());

                Boolean holds = evaluation.getConversion()
                        .convert(compiled.evaluate(evaluation), Boolean.class);

                return Boolean.TRUE.equals(holds);
            } catch (RuntimeException failed) {
                LOGGER.warn("The condition `{}` could not be evaluated and is being read as false: {}",
                            source, failed.toString());

                return false;
            }
        }

        @Override
        public String source() {
            return source;
        }
    }
}
