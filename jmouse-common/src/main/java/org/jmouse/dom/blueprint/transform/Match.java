package org.jmouse.dom.blueprint.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.Blueprint;
import org.jmouse.dom.blueprint.BlueprintValue;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Factory for common blueprint matchers.
 */
public final class Match {

    private Match() {}

    public static ContextAwareMatcher tagName(String tagName) {
        Verify.nonNull(tagName, "tagName");
        return (blueprint, c) -> blueprint instanceof Blueprint.ElementBlueprint element
                && element.tagName().equalsIgnoreCase(tagName);
    }

    public static ContextAwareMatcher element(Predicate<Blueprint.ElementBlueprint> predicate) {
        Verify.nonNull(predicate, "predicate");
        return (blueprint, c) -> blueprint instanceof Blueprint.ElementBlueprint element && predicate.test(element);
    }

    public static ContextAwareMatcher hasAttribute(String attributeName) {
        Verify.nonNull(attributeName, "attributeName");
        return element(e -> e.attributes().containsKey(attributeName));
    }

    public static ContextAwareMatcher attributeEquals(String attributeName, String expectedConstant) {
        Verify.nonNull(attributeName, "attributeName");
        Verify.nonNull(expectedConstant, "expectedConstant");
        return element(e -> constantAttributeEquals(e.attributes(), attributeName, expectedConstant));
    }

    public static ContextAwareMatcher and(BlueprintMatcher left, BlueprintMatcher right) {
        Verify.nonNull(left, "left");
        Verify.nonNull(right, "right");
        return (blueprint, c) -> left.matches(blueprint) && right.matches(blueprint);
    }

    public static ContextAwareMatcher or(BlueprintMatcher left, BlueprintMatcher right) {
        Verify.nonNull(left, "left");
        Verify.nonNull(right, "right");
        return (blueprint, c) -> left.matches(blueprint) || right.matches(blueprint);
    }

    private static boolean constantAttributeEquals(
            Map<String, BlueprintValue> attributes,
            String attributeName,
            String expectedConstant
    ) {
        BlueprintValue value = attributes.get(attributeName);
        if (value instanceof BlueprintValue.ConstantValue(Object constant)) {
            return expectedConstant.equals(String.valueOf(constant));
        }
        return false;
    }
}
