package org.jmouse.access.el.condition;

import org.jmouse.access.spi.ConditionContext;
import org.jmouse.el.evaluation.EvaluationContext;

/**
 * The decision a condition is running inside, handed to the functions and tests it calls.
 *
 * <h2>Why it exists</h2>
 *
 * <p>{@code ConditionAxis} builds a {@link ConditionContext} per evaluation — the subject, the place the
 * rule is attached to, the resource, the action and whatever the enforcement layer published — and gives
 * it to the condition. The condition then publishes <em>views</em> of it: {@code caller},
 * {@code place}, {@code resource}, {@code action}.
 *
 * <p>An {@link AccessFunction} is handed {@code (Arguments, EvaluationContext)} and therefore sees only
 * those views. That is enough to recover an identifier and nothing more: {@code PlaceView} is a
 * presentation of a place, not the {@code ScopeReference} the engine answers questions about. So a
 * function cannot ask what this subject is entitled to <em>here</em>, which is what an allowance, a
 * count and a permission check all need.
 *
 * <h2>⚠️ The key is punctuation on purpose</h2>
 *
 * <p>{@link ConditionVocabulary} is a whitelist of <strong>token kinds</strong>, not of names — so any
 * name that lexes as an identifier is reachable from any rule. {@code @access} does not lex as one, and
 * a rule that tried would be refused by the vocabulary before it ever compiled.
 *
 * <p>⚠️ Were it reachable, a rule could write {@code theName.subject().principalId()} and walk out of the
 * restricted dialect into ordinary Java bean navigation — the one thing {@link ConditionDialect} exists
 * to prevent, and it would not fail a single test.
 *
 * <p>It is written straight into the scoped chain rather than through
 * {@link EvaluationContext#setValue(String, Object)}, which reads a name as a property path.
 */
public final class ConditionBinding {

    /** ⚠️ Not an identifier, and that is the whole defence. See the class javadoc. */
    private static final String KEY = "@access";

    private ConditionBinding() {
    }

    /**
     * Publishes the decision for the functions and tests this evaluation is about to call.
     */
    public static void bind(EvaluationContext evaluation, ConditionContext context) {
        if (evaluation == null || context == null) {
            return;
        }

        evaluation.getScopedChain().setValue(KEY, context);
    }

    /**
     * The decision being evaluated, or {@code null} where nothing bound one — a harness, a compiler
     * check, a call from outside the axis.
     *
     * <p>Use this where a function has something sensible to say without a decision. Where it does not,
     * use {@link #require(EvaluationContext)}: a {@code null} that flows onward becomes a wrong answer,
     * and a wrong answer from an authorization function is the failure mode this whole seam is careful
     * about.
     */
    public static ConditionContext find(EvaluationContext evaluation) {
        if (evaluation == null) {
            return null;
        }

        return evaluation.getScopedChain().getValue(KEY) instanceof ConditionContext bound ? bound : null;
    }

    /**
     * The decision being evaluated, or a refusal saying what happened.
     *
     * <p>⚠️ Throwing is the safe half. {@code FunctionCatalog} turns any throw into a
     * {@code ConditionFunctionFailure}, which {@code ConditionAxis} reads by applying a deny and dropping
     * an allow — both refuse. A {@code null} returned instead would be compared against a threshold and
     * quietly answer something.
     */
    public static ConditionContext require(EvaluationContext evaluation) {
        ConditionContext bound = find(evaluation);

        if (bound == null) {
            throw new IllegalStateException(
                    "this function needs the decision it is running inside, and nothing bound one. It is "
                    + "published by the condition compiler for every evaluation the access engine drives, "
                    + "so a missing one means the function was called from somewhere else — a bare "
                    + "expression harness, or a test that compiled a condition without evaluating it "
                    + "through the axis.");
        }

        return bound;
    }
}
