package org.jmouse.template.transform;

import org.jmouse.template.NodeTemplate;
import org.jmouse.template.NodeDirective;
import org.jmouse.template.ValueExpression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.util.Strings.normalize;

/**
 * Factory for common blueprint changes.
 *
 * <p>Design goals:</p>
 * <ul>
 *   <li>Immutable rewrites (new blueprint instances returned)</li>
 *   <li>No regex for class token operations</li>
 *   <li>Readable control flow (no short-circuit one-liners)</li>
 * </ul>
 */
public final class Change {

    private static final String CLASS_ATTRIBUTE_NAME = "class";

    private Change() {
    }

    public static TemplateChange renameTag(String newTagName) {
        nonNull(newTagName, "newTagName");

        return (blueprint, execution) -> {
            if (blueprint == null) {
                return null;
            }

            if (blueprint instanceof NodeTemplate.Element elementBlueprint) {
                return new NodeTemplate.Element(
                        newTagName,
                        elementBlueprint.attributes(),
                        elementBlueprint.children(),
                        elementBlueprint.directives()
                );
            }

            return blueprint;
        };
    }

    public static TemplateChange setAttribute(String attributeName, ValueExpression attributeValue) {
        nonNull(attributeName, "attributeName");
        nonNull(attributeValue, "attributeValue");

        return (blueprint, execution) -> {
            if (blueprint == null) {
                return null;
            }

            if (blueprint instanceof NodeTemplate.Element(
                    String tagName,
                    Map<String, ValueExpression> attributes,
                    List<NodeTemplate> children,
                    List<NodeDirective> directives
            )) {
                Map<String, ValueExpression> copy = new LinkedHashMap<>(attributes);

                copy.put(attributeName, attributeValue);

                return new NodeTemplate.Element(
                        tagName,
                        Map.copyOf(copy),
                        children,
                        directives
                );
            }

            return blueprint;
        };
    }

    public static TemplateChange setAttribute(String attributeName, Object constantValue) {
        nonNull(attributeName, "attributeName");
        ValueExpression attributeValue = new ValueExpression.ConstantValue(constantValue);
        return setAttribute(attributeName, attributeValue);
    }

    public static TemplateChange addClass(String className) {
        nonNull(className, "className");

        return (blueprint, execution) -> {
            if (blueprint == null) {
                return null;
            }

            if (!(blueprint instanceof NodeTemplate.Element(
                    String tagName,
                    Map<String, ValueExpression> attributes,
                    List<NodeTemplate> children,
                    List<NodeDirective> directives
            ))) {
                return blueprint;
            }

            Map<String, ValueExpression> attributesCopy = new LinkedHashMap<>(attributes);

            ValueExpression existingClassValue   = attributesCopy.get(CLASS_ATTRIBUTE_NAME);
            String          mergedClassAttribute = mergeClass(existingClassValue, className);

            attributesCopy.put(CLASS_ATTRIBUTE_NAME, new ValueExpression.ConstantValue(mergedClassAttribute));

            return new NodeTemplate.Element(
                    tagName,
                    Map.copyOf(attributesCopy),
                    children,
                    directives
            );
        };
    }

    public static TemplateChange wrapWith(String tagName, TemplateChange change) {
        nonNull(tagName, "tagName");
        nonNull(change, "change");

        return (blueprint, execution) -> {
            if (blueprint == null) {
                return null;
            }

            NodeTemplate.Element wrapper = new NodeTemplate.Element(
                    tagName,
                    Map.of(),
                    List.of(blueprint),
                    List.of()
            );

            return change.apply(wrapper, execution);
        };
    }

    public static TemplateChange appendChild(NodeTemplate child) {
        nonNull(child, "child");

        return (blueprint, execution) -> {
            if (blueprint == null) {
                return null;
            }

            if (!(blueprint instanceof NodeTemplate.Element(
                    String tagName,
                    Map<String, ValueExpression> attributes,
                    List<NodeTemplate> children,
                    List<NodeDirective> directives
            ))) {
                return blueprint;
            }

            List<NodeTemplate> copy = new ArrayList<>(children);

            copy.add(child);

            return new NodeTemplate.Element(
                    tagName,
                    attributes,
                    List.copyOf(copy),
                    directives
            );
        };
    }

    public static TemplateChange prependChild(NodeTemplate child) {
        nonNull(child, "child");

        return (blueprint, execution) -> {
            if (blueprint == null) {
                return null;
            }

            if (!(blueprint instanceof NodeTemplate.Element(
                    String tagName,
                    Map<String, ValueExpression> attributes,
                    List<NodeTemplate> children,
                    List<NodeDirective> directives
            ))) {
                return blueprint;
            }

            List<NodeTemplate> copy = new ArrayList<>(children.size() + 1);

            copy.add(child);
            copy.addAll(children);

            return new NodeTemplate.Element(
                    tagName,
                    attributes,
                    List.copyOf(copy),
                    directives
            );
        };
    }

    public static TemplateChange chain(TemplateChange... changes) {
        nonNull(changes, "changes");

        return (blueprint, execution) -> {
            NodeTemplate currentBlueprint = blueprint;

            for (TemplateChange change : changes) {
                if (currentBlueprint == null) {
                    return null;
                }
                if (change == null) {
                    continue;
                }
                currentBlueprint = change.apply(currentBlueprint, execution);
            }

            return currentBlueprint;
        };
    }

    // ---------------------------------------------------------------------
    // Class attribute merge helpers
    // ---------------------------------------------------------------------

    private static String mergeClass(ValueExpression existing, String classNames) {
        String normalizedToken = normalize(classNames, String::trim);;

        if (normalizedToken.isEmpty()) {
            return readExistingClassAsString(existing);
        }

        String existingClasses = readExistingClassAsString(existing);

        if (existingClasses.isEmpty()) {
            return normalizedToken;
        }

        boolean alreadyPresent = containsToken(existingClasses, normalizedToken);

        if (alreadyPresent) {
            return existingClasses;
        }

        return existingClasses + " " + normalizedToken;
    }

    private static String readExistingClassAsString(ValueExpression existing) {
        if (existing == null) {
            return "";
        }

        if (existing instanceof ValueExpression.ConstantValue(Object value)) {
            return value == null ? "" : String.valueOf(value).trim();
        }

        return "";
    }

    /**
     * Token search in whitespace-separated class attribute without regex.
     */
    private static boolean containsToken(String token, String expected) {
        String haystack = normalize(token, String::trim);
        String needle   = normalize(expected, String::trim);

        if (haystack.isEmpty()) {
            return false;
        }

        if (needle.isEmpty()) {
            return false;
        }

        int index  = 0;
        int length = haystack.length();

        while (index < length) {
            while (index < length && Character.isWhitespace(haystack.charAt(index))) {
                index++;
            }

            if (index >= length) {
                break;
            }

            int tokenStart = index;

            while (index < length && !Character.isWhitespace(haystack.charAt(index))) {
                index++;
            }

            int tokenEnd = index;

            if (equalsRegion(haystack, tokenStart, tokenEnd, needle)) {
                return true;
            }
        }

        return false;
    }

    private static boolean equalsRegion(String text, int startIndex, int endIndex, String token) {
        int segmentLength = endIndex - startIndex;

        if (segmentLength != token.length()) {
            return false;
        }

        for (int i = 0; i < segmentLength; i++) {
            char lc = text.charAt(startIndex + i);
            char rc = token.charAt(i);
            if (lc != rc) {
                return false;
            }
        }

        return true;
    }

}
