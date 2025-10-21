package org.jmouse.security.authorization.method;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.EvaluationException;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParseException;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.ExpressionAccessResult;

/**
 * 🧮 Evaluates security expressions within a given {@link EvaluationContext}.
 * <p>
 * This utility bridges the jMouse Expression Language (EL) engine with
 * the authorization layer, allowing boolean or structured {@link AccessResult}
 * evaluations directly from {@link Expression} nodes.
 * </p>
 *
 * <h3>Supported result types ⚙️</h3>
 * <ul>
 *   <li>{@link Boolean} → wrapped as {@link ExpressionAccessResult}</li>
 *   <li>{@link AccessResult} → returned as-is</li>
 *   <li>{@link String} → parsed as boolean ("true"/"false")</li>
 * </ul>
 *
 * <h3>Error handling 🚨</h3>
 * <ul>
 *   <li>Throws {@link IllegalArgumentException} if the result type is unsupported</li>
 *   <li>Wraps {@link EvaluationException} and {@link ParseException} with
 *       contextual error messages for easier debugging</li>
 * </ul>
 */
public final class SecurityExpressionEvaluator {

    private SecurityExpressionEvaluator() {
    }

    /**
     * 🔍 Evaluates a compiled {@link Expression} using the provided evaluation context.
     *
     * @param expression the parsed EL expression (e.g. <code>hasRole('ADMIN')</code>)
     * @param context    the evaluation context containing variables and root objects
     * @return an {@link AccessResult} describing the access decision outcome,
     *         or {@code null} if the expression returned {@code null}
     * @throws IllegalArgumentException if evaluation or parsing fails,
     *                                  or if result type is invalid
     */
    public static AccessResult evaluate(Expression expression, EvaluationContext context) {
        try {
            if (expression == null) {
                return null;
            }

            Object evaluated = expression.evaluate(context);

            return switch (evaluated) {
                case Boolean booleanValue -> new ExpressionAccessResult(booleanValue, expression);
                case AccessResult accessResult -> accessResult;
                case String stringValue -> new ExpressionAccessResult(Boolean.parseBoolean(stringValue), expression);
                case null -> null;
                default -> throw new IllegalArgumentException(
                        "Evaluation result should be either 'Boolean' or 'AccessResult'");
            };
        } catch (EvaluationException evaluationException) {
            throw new IllegalArgumentException(
                    "Could not evaluate expression: " + expression, evaluationException);
        } catch (ParseException parseException) {
            throw new IllegalArgumentException(
                    "Could not parse expression: " + expression, parseException);
        }
    }

}
