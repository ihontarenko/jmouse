package org.jmouse.dom.template.transform;

import org.jmouse.dom.template.NodeTemplate.Element;

import java.util.function.Predicate;

import static org.jmouse.core.Verify.nonNull;

/**
 * Factory for context-aware matchers.
 */
public final class ContextMatch {

    private ContextMatch() {}

    public static ContextAwareMatcher tagName(String tagName) {
        nonNull(tagName, "tagName");
        return (blueprint, context) -> blueprint instanceof Element element
                && element.tagName().equalsIgnoreCase(tagName);
    }

    public static ContextAwareMatcher element(Predicate<Element> predicate) {
        nonNull(predicate, "predicate");
        return (blueprint, context) -> blueprint instanceof Element element && predicate.test(element);
    }

    public static ContextAwareMatcher depthGte(int depth) {
        return (blueprint, context) -> context.getDepth() >= depth;
    }

    public static ContextAwareMatcher insideAncestor(String ancestorTag) {
        nonNull(ancestorTag, "ancestorTag");
        return (blueprint, context) -> context.hasAncestor(ancestorTag);
    }

    public static ContextAwareMatcher parentTag(String parentTagName) {
        nonNull(parentTagName, "parentTagName");
        return (blueprint, context) -> {
            Element parent = context.isParent();
            return parent != null && parent.tagName().equalsIgnoreCase(parentTagName);
        };
    }

}
