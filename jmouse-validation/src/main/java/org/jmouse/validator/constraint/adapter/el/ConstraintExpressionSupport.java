package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.ValidationDefinition;

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
     * </p>
     *
     * @param expression EL constraint expression
     * @return mapped constraint instance
     *
     * @throws IllegalArgumentException if expression does not produce a {@link ValidationDefinition}
     */
    public Constraint parse(String expression) {
        Object evaluated = expressionLanguage.evaluate(expression);

        if (!(evaluated instanceof ValidationDefinition definition)) {
            throw new IllegalArgumentException(
                    "Expression did not produce ValidationDefinition: " + expression
            );
        }

        return adapter.toConstraint(definition);
    }
}