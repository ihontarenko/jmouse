package org.jmouse.dom.util;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for traversing a DOM tree and selecting nodes. 🔎
 *
 * <p>
 * Provides simple depth-first search helpers based on:
 * </p>
 * <ul>
 *     <li>tag name</li>
 *     <li>attribute presence</li>
 *     <li>predefined HTML form control tags</li>
 * </ul>
 *
 * <p>
 * Traversal relies on {@link Node#execute(java.util.function.Consumer)},
 * which performs depth-first iteration.
 * </p>
 *
 * <p>
 * All methods return new mutable lists containing matching {@link ElementNode}s.
 * </p>
 */
public final class NodeTraversal {

    /**
     * HTML form control tag names.
     */
    private static final List<TagName> FORM_CONTROLS =
            List.of(TagName.INPUT, TagName.SELECT, TagName.TEXTAREA);

    private NodeTraversal() {}

    /**
     * Finds all element nodes with the given tag name.
     *
     * <h3>Example</h3>
     *
     * <pre>{@code
     * List<ElementNode> inputs =
     *     NodeTraversal.findByTag(root, TagName.INPUT);
     * }</pre>
     *
     * @param root traversal root
     * @param tagName tag name to match
     * @return list of matching elements (possibly empty)
     */
    public static List<ElementNode> findByTag(Node root, TagName tagName) {
        List<ElementNode> result = new ArrayList<>();

        root.execute(node -> {
            if (node instanceof ElementNode element &&
                    element.getTagName() == tagName) {
                result.add(element);
            }
        });

        return result;
    }

    /**
     * Finds all element nodes that contain the specified attribute.
     *
     * <h3>Example</h3>
     *
     * <pre>{@code
     * List<ElementNode> withId =
     *     NodeTraversal.findByAttribute(root, "id");
     * }</pre>
     *
     * @param root traversal root
     * @param attributeName attribute name to check
     * @return list of matching elements (possibly empty)
     */
    public static List<ElementNode> findByAttribute(Node root, String attributeName) {
        List<ElementNode> result = new ArrayList<>();

        root.execute(node -> {
            if (node instanceof ElementNode element &&
                    element.getAttribute(attributeName) != null) {
                result.add(element);
            }
        });

        return result;
    }

    /**
     * Finds standard HTML form control elements.
     *
     * <p>
     * Currently matches:
     * </p>
     * <ul>
     *     <li>{@link TagName#INPUT}</li>
     *     <li>{@link TagName#SELECT}</li>
     *     <li>{@link TagName#TEXTAREA}</li>
     * </ul>
     *
     * @param root traversal root
     * @return list of form control elements (possibly empty)
     */
    public static List<ElementNode> findFormControls(Node root) {
        List<ElementNode> result = new ArrayList<>();

        root.execute(node -> {
            if (node instanceof ElementNode element) {
                if (FORM_CONTROLS.contains(element.getTagName())) {
                    result.add(element);
                }
            }
        });

        return result;
    }
}