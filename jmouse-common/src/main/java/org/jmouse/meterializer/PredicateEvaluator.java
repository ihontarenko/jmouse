package org.jmouse.meterializer;

import org.jmouse.core.Verify;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PredicateEvaluator {

    private final ValueResolver resolver;

    public PredicateEvaluator(ValueResolver resolver) {
        this.resolver = Verify.nonNull(resolver, "resolver");
    }

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

    private boolean isPresent(Object value) {
        return switch (value) {
            case null -> false;
            case String string -> !string.trim().isEmpty();
            case Collection<?> collection -> !collection.isEmpty();
            case Map<?, ?> map -> !map.isEmpty();
            default -> true;
        };
    }

    private boolean asBoolean(Object value) {
        return switch (value) {
            case null -> false;
            case Boolean booleanValue -> booleanValue;
            case Number number -> number.intValue() != 0;
            default -> Boolean.parseBoolean(String.valueOf(value));
        };
    }

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
