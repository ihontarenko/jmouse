package org.jmouse.common.dom.rendering;

import org.jmouse.core.Verify;
import org.jmouse.common.dom.Node;
import org.jmouse.common.dom.NodeType;
import org.jmouse.common.dom.TagName;

import java.util.function.Predicate;

/**
 * Common predicates for renderer registrations.
 */
public final class Renderers {

    private Renderers() {}

    public static Predicate<Node> nodeType(NodeType nodeType) {
        Verify.nonNull(nodeType, "nodeType");
        return node -> node.getNodeType() == nodeType;
    }

    public static Predicate<Node> tagName(TagName tagName) {
        Verify.nonNull(tagName, "qName");
        return node -> node.getTagName() == tagName;
    }

}
