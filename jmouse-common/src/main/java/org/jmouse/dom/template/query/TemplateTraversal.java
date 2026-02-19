package org.jmouse.dom.template.query;

import org.jmouse.dom.template.NodeTemplate;
import org.jmouse.util.Strings;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.jmouse.core.Verify.nonNull;

public final class TemplateTraversal {

    private TemplateTraversal() {
    }

    /**
     * Walks the template tree in depth-first order and calls {@code visitor} for every node.
     */
    public static void walk(NodeTemplate root, Consumer<NodeTemplate> visitor) {
        nonNull(root, "root");
        nonNull(visitor, "visitor");

        Deque<NodeTemplate> stack = new ArrayDeque<>();

        stack.push(root);

        while (!stack.isEmpty()) {
            NodeTemplate node = stack.pop();

            visitor.accept(node);

            List<NodeTemplate> children = childrenOf(node);

            for (int index = children.size() - 1; index >= 0; index--) {
                NodeTemplate child = children.get(index);
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    /**
     * Finds all nodes matching {@code predicate}.
     */
    public static List<NodeTemplate> find(NodeTemplate root, Predicate<NodeTemplate> predicate) {
        nonNull(root, "root");
        nonNull(predicate, "predicate");

        List<NodeTemplate> result = new ArrayList<>();

        walk(root, node -> {
            if (predicate.test(node)) {
                result.add(node);
            }
        });

        return List.copyOf(result);
    }

    public static NodeTemplate findFirst(NodeTemplate root, Predicate<NodeTemplate> predicate) {
        nonNull(root, "root");
        nonNull(predicate, "predicate");

        final NodeTemplate[] found = { null };

        walk(root, node -> {
            if (found[0] != null) {
                return;
            }
            if (predicate.test(node)) {
                found[0] = node;
            }
        });

        return found[0];
    }

    public static boolean anyMatch(NodeTemplate root, Predicate<NodeTemplate> predicate) {
        return findFirst(root, predicate) != null;
    }

    /**
     * Finds all {@link NodeTemplate.Element} nodes with the given tag name (case-insensitive).
     */
    public static List<NodeTemplate.Element> findElementsByTag(NodeTemplate root, String tagName) {
        nonNull(root, "root");
        nonNull(tagName, "tagName");

        String                     expected = tagName.trim().toLowerCase(Locale.ROOT);
        List<NodeTemplate.Element> result   = new ArrayList<>();

        walk(root, node -> {
            if (node instanceof NodeTemplate.Element element) {
                String actual = safeLower(element.tagName());
                if (expected.equals(actual)) {
                    result.add(element);
                }
            }
        });

        return List.copyOf(result);
    }

    /**
     * Finds all {@link NodeTemplate.Element} nodes that have an attribute with the given name.
     */
    public static List<NodeTemplate.Element> findElementsWithAttribute(NodeTemplate root, String attributeName) {
        nonNull(root, "root");
        nonNull(attributeName, "attributeName");

        String                     expected = attributeName.trim();
        List<NodeTemplate.Element> result   = new ArrayList<>();

        walk(root, node -> {
            if (node instanceof NodeTemplate.Element element) {
                if (element.attributes() != null && element.attributes().containsKey(expected)) {
                    result.add(element);
                }
            }
        });

        return List.copyOf(result);
    }

    /**
     * Finds all {@link NodeTemplate.Include} nodes.
     */
    public static List<NodeTemplate.Include> findIncludes(NodeTemplate root) {
        nonNull(root, "root");

        List<NodeTemplate.Include> result = new ArrayList<>();

        walk(root, node -> {
            if (node instanceof NodeTemplate.Include include) {
                result.add(include);
            }
        });

        return List.copyOf(result);
    }

    /**
     * Returns children for traversal.
     * Important: includes do not contribute children here (they reference external templates).
     */
    public static List<NodeTemplate> childrenOf(NodeTemplate node) {
        return switch (node) {
            case null -> List.of();
            case NodeTemplate.Element element ->
                    element.children() == null ? List.of() : element.children();
            case NodeTemplate.Conditional conditional -> merge(
                    conditional.whenTrue(),
                    conditional.whenFalse()
            );
            case NodeTemplate.Repeat repeat ->
                    repeat.body() == null ? List.of() : repeat.body();
            case NodeTemplate.Text ignored -> List.of();
            case NodeTemplate.Include ignored -> List.of();
        };
    }

    private static List<NodeTemplate> merge(List<NodeTemplate> left, List<NodeTemplate> right) {
        List<NodeTemplate> a = left == null ? List.of() : left;
        List<NodeTemplate> b = right == null ? List.of() : right;

        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }

        List<NodeTemplate> merged = new ArrayList<>(a.size() + b.size());
        merged.addAll(a);
        merged.addAll(b);
        return merged;
    }

    private static String safeLower(String value) {
        return Strings.normalize(Strings.normalize(value, String::trim), String::toLowerCase);
    }
}
