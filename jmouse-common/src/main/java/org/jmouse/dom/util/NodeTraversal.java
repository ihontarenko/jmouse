package org.jmouse.dom.util;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;

import java.util.ArrayList;
import java.util.List;

public final class NodeTraversal {

    private static final List<TagName> FORM_CONTROLS = List.of(TagName.INPUT, TagName.SELECT, TagName.TEXTAREA);

    private NodeTraversal() {}

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
