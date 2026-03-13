package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.ValidationDefinition;

import java.util.Map;

/**
 * Facade for parsing EL-based constraint definitions into typed {@link Constraint} instances. 🧠
 *
 * <p>
 * This class integrates:
 * </p>
 * <ul>
 *     <li>{@link ExpressionLanguage} — evaluates the raw EL expression,</li>
 *     <li>{@link ConstraintExpressionAdapter} — converts {@link ValidationDefinition}
 *         into a concrete {@link Constraint}.</li>
 * </ul>
 *
 * <h3>Supported expression style</h3>
 *
 * <pre>{@code
 * @OneOf('values':['uk','en'],'message':'lang must be uk/en (expr)')
 * }</pre>
 *
 * <p>
 * The expression must evaluate to a {@link ValidationDefinition}.
 * Otherwise, an {@link IllegalArgumentException} is thrown.
 * </p>
 *
 * <h3>Typical usage</h3>
 *
 * <pre>{@code
 * ConstraintExpressionSupport support =
 *     ConstraintELModule.create(el, registry, mapper);
 *
 * Constraint constraint =
 *     support.parse("@OneOf('values':['uk','en'],'message':'lang must be uk/en (expr)')");
 * }</pre>
 */
public final class ConstraintExpressionSupport {

    private final ExpressionLanguage          expressionLanguage;
    private final ConstraintExpressionAdapter adapter;

    /**
     * Creates a new support facade.
     *
     * @param expressionLanguage EL engine
     * @param adapter            adapter converting definitions to constraints
     */
    public ConstraintExpressionSupport(
            ExpressionLanguage expressionLanguage,
            ConstraintExpressionAdapter adapter
    ) {
        this.expressionLanguage = expressionLanguage;
        this.adapter = adapter;
    }

    /**
     * Parses and converts an EL expression into a {@link Constraint}.
     *
     * <p>
     * The expression must evaluate to a {@link ValidationDefinition}.
     * A fresh {@link EvaluationContext} is created automatically.
     * </p>
     *
     * @param expression EL constraint expression
     *
     * @return mapped constraint instance
     *
     * @throws IllegalArgumentException if expression does not produce a {@link ValidationDefinition}
     */
    public Constraint parse(String expression) {
        return parse(expression, expressionLanguage.newContext());
    }

    /**
     * Parses and converts an EL expression into a {@link Constraint}
     * using the provided evaluation values.
     *
     * <p>
     * A fresh {@link EvaluationContext} is created and populated
     * from the given value map before evaluation.
     * </p>
     *
     * @param expression EL constraint expression
     * @param values     values exposed to the evaluation context
     *
     * @return mapped constraint instance
     *
     * @throws IllegalArgumentException if expression does not produce a {@link ValidationDefinition}
     */
    public Constraint parse(String expression, Map<String, Object> values) {
        EvaluationContext evaluationContext = expressionLanguage.newContext();
        values.forEach(evaluationContext::setValue);
        return parse(expression, evaluationContext);
    }

    /**
     * Parses and converts an EL expression into a {@link Constraint}
     * using the given evaluation context.
     *
     * <p>
     * The evaluated result must be a {@link ValidationDefinition},
     * which is then adapted to a concrete {@link Constraint}.
     * </p>
     *
     * @param expression        EL constraint expression
     * @param evaluationContext evaluation context
     *
     * @return mapped constraint instance
     *
     * @throws IllegalArgumentException if expression does not produce a {@link ValidationDefinition}
     */
    public Constraint parse(String expression, EvaluationContext evaluationContext) {
        Object evaluated = expressionLanguage.evaluate(expression, evaluationContext);

        if (!(evaluated instanceof ValidationDefinition definition)) {
            throw new IllegalArgumentException(
                    "Expression did not produce ValidationDefinition: " + expression
            );
        }

        return adapter.toConstraint(definition);
    }

}