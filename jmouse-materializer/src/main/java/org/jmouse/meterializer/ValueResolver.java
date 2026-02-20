package org.jmouse.meterializer;

/**
 * Resolves a {@link ValueExpression} into a concrete runtime value. 🧩
 *
 * <p>
 * A {@code ValueResolver} is responsible for interpreting declarative
 * {@link ValueExpression} instances within the context of an active
 * {@link RenderingExecution}.
 * </p>
 *
 * <p>
 * Typical responsibilities include:
 * </p>
 * <ul>
 *     <li>resolving path expressions against the data model</li>
 *     <li>retrieving request attributes</li>
 *     <li>evaluating nested format expressions</li>
 *     <li>returning constant values as-is</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * ValueResolver resolver = (expression, execution) -> {
 *     // custom resolution logic
 *     return ...;
 * };
 *
 * Object result = resolver.resolve(
 *     ValueExpression.path("user.name"),
 *     execution
 * );
 * }</pre>
 *
 * <p>
 * Implementations should be stateless and thread-safe unless explicitly
 * documented otherwise.
 * </p>
 */
@FunctionalInterface
public interface ValueResolver {

    /**
     * Resolves a value expression in the given execution context.
     *
     * @param value      expression to resolve
     * @param execution  active rendering execution context
     * @return resolved value (may be {@code null})
     */
    Object resolve(ValueExpression value, RenderingExecution execution);

}
