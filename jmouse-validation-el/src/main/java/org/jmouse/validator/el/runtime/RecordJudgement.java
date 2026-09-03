package org.jmouse.validator.el.runtime;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.mapper.Mapper;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One record being judged: what it holds, what has been said about it, and how to ask an expression. ⚖️
 *
 * <p>Everything mutable about a validation run lives here and nowhere else, which is what lets a
 * {@link CompiledValidation} be shared, reused and read from several threads: the compiled tree holds
 * expressions and signatures, and one of these is created per record.</p>
 *
 * <h2>⚠️ Every field is in scope, not only the one being checked</h2>
 *
 * <p>A guard reads siblings by name and an invariant compares two of them, so the whole record is bound
 * before anything runs. {@code value} and {@code field} are re-bound per line on top of that — which
 * is what makes {@code field.label ~ ' is needed'} work in a message, and what a check's arguments read
 * when they refer to the value being examined.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RecordJudgement {

    private final Map<String, Object>    values;
    private final EvaluationContext      context;
    private final ConstraintTypeRegistry registry;
    private final Mapper                 mapper;

    private final List<ValidationError> errors  = new ArrayList<>();
    private final Set<String>           skipped = new LinkedHashSet<>();
    private final Set<String>           asked   = new LinkedHashSet<>();

    RecordJudgement(
            Map<String, Object> values,
            ExpressionLanguage expressionLanguage,
            ConstraintTypeRegistry registry,
            Mapper mapper
    ) {
        this.values = values;
        this.registry = registry;
        this.mapper = mapper;
        this.context = expressionLanguage.newContext();

        values.forEach(context::setValue);
    }

    /**
     * What the record holds under a name, noting that the field has now been examined.
     *
     * <p>⚠️ The noting happens here because this is called exactly once per line that runs, which makes
     * it the one place that knows a field was actually asked about. Recording it at the top of
     * {@link CompiledLine#evaluate} instead would be the same fact written somewhere it can be
     * forgotten.</p>
     *
     * @param field the field's name
     * @return its value, or {@code null} when the record does not carry it
     */
    public Object field(String field) {
        asked.add(field);

        return values.get(field);
    }

    /**
     * Evaluates a condition.
     *
     * <p>⚠️ Anything other than {@code true} is false, {@code null} included. A guard that cannot be
     * answered is not a guard that holds: treating an unanswerable condition as true would run a branch
     * on the strength of an expression nobody could evaluate.</p>
     *
     * @param condition the expression to ask
     * @return whether it holds
     */
    public boolean holds(Expression condition) {
        return Boolean.TRUE.equals(condition.evaluate(context));
    }

    /**
     * Evaluates an expression for its value.
     *
     * @param expression the expression to ask
     * @return whatever it produced
     */
    public Object value(Expression expression) {
        return expression.evaluate(context);
    }

    /**
     * Evaluates a message.
     *
     * @param message the expression to ask
     * @return what it says, as text
     */
    public String say(Expression message) {
        return String.valueOf(value(message));
    }

    /**
     * Builds a check's constraint, with the value under examination in scope.
     *
     * @param check the check
     * @param field the field it is about
     * @param value what the record holds there
     * @return the constraint
     */
    public Constraint constraintOf(CompiledCheck check, String field, Object value) {
        context.setValue("field", field);
        context.setValue("value", value);

        return check.constraint(registry, mapper, this);
    }

    /**
     * Records a complaint, unless the same one has already been made about the same field.
     *
     * <h2>⚠️ A line message covers several checks, so it can be said twice</h2>
     *
     * <p>{@code part_number : notBlank, size(3, 32), pattern('…') : 'A part number looks like AB-1234'}
     * gives one sentence to three checks. Two of them failing produced that sentence twice — and a
     * reader seeing one sentence repeated cannot tell it was two different faults; they read it as the
     * software stuttering.</p>
     *
     * <p>⚠️ <strong>Deduplicated on the words, not on the check.</strong> Two checks that fail with
     * <em>different</em> wording both have something to say and both are kept; two that say exactly the
     * same thing are saying one thing. What the reader sees is the message, so that is what decides.</p>
     *
     * @param field   what it is about, or {@code null} for the record itself
     * @param message what to say
     */
    public void refuse(String field, String message) {
        ValidationError error = new ValidationError(field, message);

        if (errors.contains(error)) {
            return;
        }

        errors.add(error);
    }

    /**
     * Judges a list of items in order.
     *
     * @param items what to evaluate
     */
    public void evaluate(List<CompiledItem> items) {
        for (CompiledItem item : items) {
            item.evaluate(this);
        }
    }

    /**
     * Records every field a branch would have asked about, having not run.
     *
     * @param items the branch that was not taken
     */
    public void skip(List<CompiledItem> items) {
        for (CompiledItem item : items) {
            switch (item) {
                case CompiledLine line -> skipped.add(line.field());
                case CompiledGuard guard -> {
                    skip(guard.body());
                    skip(guard.otherwise());
                }
                case CompiledInvariant ignored -> {
                    // An invariant names no field, so there is nothing to report as unasked.
                }
            }
        }
    }

    /** @return whether anything has been refused so far */
    boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * What was found.
     *
     * @param gated whether the gate refused
     * @return the outcome
     */
    ValidationOutcome outcome(boolean gated) {
        // ⚠️ A field asked about somewhere is not "not asked", even though another branch skipped it —
        // `always` and a guarded branch may both name it. The subtraction happens here rather than at
        // skip time, because the branch that asks may run after the branch that does not.
        List<String> unasked = skipped.stream().filter(field -> !asked.contains(field)).toList();

        return new ValidationOutcome(errors, unasked, gated);
    }
}
