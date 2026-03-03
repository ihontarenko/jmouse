package org.jmouse.meterializer.transform;

import org.jmouse.meterializer.NodeTemplate;
import org.jmouse.meterializer.ValueExpression;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static org.jmouse.core.Verify.nonNull;

public final class Match {

    private Match() {}

    public static ContextAwareMatcher tagName(String tagName) {
        nonNull(tagName, "qName");
        return (t, context) ->
                t instanceof NodeTemplate.Element element
                        && element.tagName().equalsIgnoreCase(tagName);
    }

    public static ContextAwareMatcher element(Predicate<NodeTemplate.Element> predicate) {
        nonNull(predicate, "predicate");
        return (t, context) ->
                t instanceof NodeTemplate.Element element
                        && predicate.test(element);
    }

    public static ContextAwareMatcher hasAttribute(String attributeName) {
        nonNull(attributeName, "attributeName");
        return element(e -> e.attributes().containsKey(attributeName));
    }

    public static ContextAwareMatcher attributeValue(String attributeName, String constant) {
        nonNull(attributeName, "attributeName");
        nonNull(constant, "constant");
        return element(e -> constantAttributeValues(e.attributes(), attributeName, constant));
    }

    // ---------- Context-aware helpers ----------
    public static ContextAwareMatcher insideAncestor(String ancestorTagName) {
        nonNull(ancestorTagName, "ancestorTagName");
        return (t, context) -> context != null && context.hasAncestor(ancestorTagName);
    }

    public static ContextAwareMatcher insideAncestor(Predicate<NodeTemplate.Element> predicate) {
        nonNull(predicate, "predicate");
        return (t, context) -> {
            if (context == null) {
                return false;
            }
            return context.hasAncestor(predicate);
        };
    }

    public static ContextAwareMatcher parentTag(String parentTagName) {
        return (t, context) -> {
            if (context == null) {
                return false;
            }
            NodeTemplate.Element parent = context.ancestor();
            return parent != null && parent.tagName().equalsIgnoreCase(nonNull(parentTagName, "parentTagName"));
        };
    }

    public static ContextAwareMatcher and(ContextAwareMatcher left, ContextAwareMatcher right) {
        return (t, context) -> nonNull(left, "left").matches(t, context)
                && nonNull(right, "right").matches(t, context);
    }

    public static ContextAwareMatcher or(ContextAwareMatcher left, ContextAwareMatcher right) {
        return (t, context) -> nonNull(left, "left").matches(t, context)
                || nonNull(right, "right").matches(t, context);
    }

    public static ContextAwareMatcher not(ContextAwareMatcher matcher) {
        nonNull(matcher, "matcher");
        return (t, context) -> !matcher.matches(t, context);
    }

    public static boolean constantAttributeValues(Map<String, ValueExpression> attributes, String name, String expected) {
        ValueExpression value = attributes.get(name);
        if (value instanceof ValueExpression.ConstantValue(Object constant)) {
            return Objects.equals(expected, String.valueOf(constant));
        }
        return false;
    }

    public static boolean constantAttributeValues(NodeTemplate.Element element, String name, String expected) {
        return constantAttributeValues(element.attributes(), name, expected);
    }

}
