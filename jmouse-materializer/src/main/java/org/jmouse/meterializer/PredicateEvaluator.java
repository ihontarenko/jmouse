package org.jmouse.meterializer;

import org.jmouse.core.Verify;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluates {@link TemplatePredicate} instances against a {@link RenderingExecution}. 🧠
 *
 * <p>
 * The evaluator delegates value extraction to a {@link ValueResolver},
 * then applies predicate-specific logic (boolean coercion, equality,
 * presence checks, containment, logical composition).
 * </p>
 *
 * <h3>Supported predicate types</h3>
 * <ul>
 *     <li>{@code BooleanValue} — coerces resolved value to boolean</li>
 *     <li>{@code Present} — checks value presence (null/empty-safe)</li>
 *     <li>{@code Equality} — compares resolved left/right values</li>
 *     <li>{@code Not} — negation</li>
 *     <li>{@code All} — logical AND over nested predicates</li>
 *     <li>{@code Contains} — membership / substring check</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * // present(user.name) && contains(user.tags, "vip")
 * TemplatePredicate predicate = new TemplatePredicate.All(List.of(
 *     new TemplatePredicate.Present(ValueExpression.path("user.name")),
 *     new TemplatePredicate.Contains(
 *         ValueExpression.path("user.tags"),
 *         ValueExpression.constant("vip")
 *     )
 * ));
 *
 * boolean result = evaluator.evaluate(predicate, execution);
 * }</pre>
 *
 * <p>
 * The evaluator is stateless and thread-safe assuming the provided
 * {@link ValueResolver} is thread-safe.
 * </p>
 */
public final class PredicateEvaluator {

    private final ValueResolver resolver;

    /**
     * Creates evaluator with required {@link ValueResolver}.
     *
     * @param resolver value resolver used to extract predicate operands
     */
    public PredicateEvaluator(ValueResolver resolver) {
        this.resolver = Verify.nonNull(resolver, "resolver");
    }

    /**
     * Evaluates a predicate against the given execution context.
     *
     * @param predicate predicate to evaluate
     * @param execution rendering execution context
     * @return {@code true} if predicate matches
     *
     * @throws IllegalStateException if predicate type is unsupported
     */
    public boolean evaluate(TemplatePredicate predicate, RenderingExecution execution) {
        return switch (predicate) {
            case TemplatePredicate.BooleanValue booleanValuePredicate ->
                    asBoolean(resolver.resolve(booleanValuePredicate.value(), execution));
            case TemplatePredicate.Present presentPredicate ->
                    isPresent(resolver.resolve(presentPredicate.value(), execution));
            case TemplatePredicate.Equality equalityPredicate ->
                    Objects.equals(
                            resolver.resolve(equalityPredicate.left(), execution),
                            resolver.resolve(equalityPredicate.right(), execution)
                    );
            case TemplatePredicate.Not notPredicate ->
                    !evaluate(notPredicate.inner(), execution);
            case TemplatePredicate.All allPredicate -> {
                for (TemplatePredicate inner : allPredicate.predicates()) {
                    if (!evaluate(inner, execution)) {
                        yield false;
                    }
                }
                yield true;
            }
            case TemplatePredicate.Contains containsPredicate -> contains(
                    resolver.resolve(containsPredicate.collection(), execution),
                    resolver.resolve(containsPredicate.value(), execution)
            );
            default -> throw new IllegalStateException("Unexpected predicate: " + predicate);
        };
    }

    /**
     * Determines whether a value is considered "present".
     *
     * <p>Rules:</p>
     * <ul>
     *     <li>{@code null} → false</li>
     *     <li>empty {@code String} (after trim) → false</li>
     *     <li>empty {@code Collection} → false</li>
     *     <li>empty {@code Map} → false</li>
     *     <li>otherwise → true</li>
     * </ul>
     */
    private boolean isPresent(Object value) {
        return switch (value) {
            case null -> false;
            case String string -> !string.trim().isEmpty();
            case Collection<?> collection -> !collection.isEmpty();
            case Map<?, ?> map -> !map.isEmpty();
            default -> true;
        };
    }

    /**
     * Coerces a value to boolean.
     *
     * <p>Rules:</p>
     * <ul>
     *     <li>{@code null} → false</li>
     *     <li>{@code Boolean} → direct value</li>
     *     <li>{@code Number} → non-zero → true</li>
     *     <li>otherwise → {@link Boolean#parseBoolean(String)}</li>
     * </ul>
     */
    private boolean asBoolean(Object value) {
        return switch (value) {
            case null -> false;
            case Boolean booleanValue -> booleanValue;
            case Number number -> number.intValue() != 0;
            default -> Boolean.parseBoolean(String.valueOf(value));
        };
    }

    /**
     * Evaluates containment semantics.
     *
     * <p>Supported cases:</p>
     * <ul>
     *     <li>{@code Collection} → contains element</li>
     *     <li>{@code Map} → contains key</li>
     *     <li>{@code String} → substring match</li>
     *     <li>array → treated as {@code List}</li>
     *     <li>fallback → {@link Objects#equals(Object, Object)}</li>
     * </ul>
     */
    private boolean contains(Object collectionValue, Object searchedValue) {
        if (collectionValue == null || searchedValue == null) {
            return false;
        }

        if (collectionValue.getClass().isArray()) {
            return contains(List.of(((Object[]) collectionValue)), searchedValue);
        }

        return switch (collectionValue) {
            case Collection<?> collection -> collection.contains(searchedValue);
            case Map<?, ?> map -> map.containsKey(searchedValue);
            case String text -> text.contains(String.valueOf(searchedValue));
            default -> Objects.equals(collectionValue, searchedValue);
        };

    }
}
