package org.jmouse.dom.blueprint.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.Blueprint.ElementBlueprint;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Factory for context-aware matchers.
 */
public final class ContextMatch {

    private ContextMatch() {}

    public static ContextAwareMatcher tagName(String tagName) {
        Verify.nonNull(tagName, "tagName");
        return (blueprint, context) -> blueprint instanceof ElementBlueprint element
                && element.tagName().equalsIgnoreCase(tagName);
    }

    public static ContextAwareMatcher element(Predicate<ElementBlueprint> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return (blueprint, context) -> blueprint instanceof ElementBlueprint element && predicate.test(element);
    }

    public static ContextAwareMatcher depthAtLeast(int depth) {
        return (blueprint, context) -> context.getDepth() >= depth;
    }

    public static ContextAwareMatcher insideAncestor(String ancestorTagName) {
        Verify.nonNull(ancestorTagName, "ancestorTagName");
        return (blueprint, context) -> context.hasAncestor(ancestorTagName);
    }

    public static ContextAwareMatcher parentTagName(String parentTagName) {
        Verify.nonNull(parentTagName, "parentTagName");
        return (blueprint, context) -> {
            ElementBlueprint parent = context.isParent();
            return parent != null && parent.tagName().equalsIgnoreCase(parentTagName);
        };
    }

    public static ContextAwareMatcher and(ContextAwareMatcher left, ContextAwareMatcher right) {
        Verify.nonNull(left, "left");
        Verify.nonNull(right, "right");
        return (blueprint, context) -> left.matches(blueprint, context) && right.matches(blueprint, context);
    }

    public static ContextAwareMatcher or(ContextAwareMatcher left, ContextAwareMatcher right) {
        Verify.nonNull(left, "left");
        Verify.nonNull(right, "right");
        return (blueprint, context) -> left.matches(blueprint, context) || right.matches(blueprint, context);
    }

    public static ContextAwareMatcher not(ContextAwareMatcher matcher) {
        Verify.nonNull(matcher, "matcher");
        return (blueprint, context) -> !matcher.matches(blueprint, context);
    }
}
