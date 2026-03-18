package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;
import org.jmouse.dom.NodeType;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TargetSelector} that places feedback after the last sibling block
 * representing the same logical field. 🧭
 *
 * <p>
 * Useful for grouped controls such as radio/checkbox collections where multiple
 * DOM branches share the same logical field key.
 * </p>
 *
 * <p>
 * Resolution algorithm:
 * </p>
 * <ol>
 *     <li>Resolve logical error key for the control</li>
 *     <li>Walk ancestors upward</li>
 *     <li>Find the nearest parent having multiple child branches for that key</li>
 *     <li>Return the last matching child branch</li>
 *     <li>Fallback to delegate if no grouped branch is found</li>
 * </ol>
 */
public final class LastSiblingTargetSelector implements TargetSelector {

    private final FieldKeyResolver keyResolver;
    private final TargetSelector   fallback;

    public LastSiblingTargetSelector(FieldKeyResolver keyResolver) {
        this(keyResolver, new SelfTargetSelector());
    }

    public LastSiblingTargetSelector(
            FieldKeyResolver keyResolver,
            TargetSelector fallback
    ) {
        this.keyResolver = keyResolver;
        this.fallback = fallback;
    }

    @Override
    public Node resolve(Node control) {
        String errorKey = keyResolver.resolveErrorKey(control);

        if (errorKey == null || errorKey.isBlank()) {
            return fallback.resolve(control);
        }

        Node current = control;

        while (current != null) {
            Node parent = current.getParent();

            if (parent == null) {
                break;
            }

            List<Node> matchingChildren = matchingChildren(parent, errorKey);

            if (matchingChildren.size() > 1) {
                return matchingChildren.getLast();
            }

            current = parent;
        }

        return fallback.resolve(control);
    }

    private List<Node> matchingChildren(Node parent, String errorKey) {
        List<Node> matches = new ArrayList<>();

        for (Node child : parent.getChildren()) {
            if (containsControlWithErrorKey(child, errorKey)) {
                matches.add(child);
            }
        }

        return matches;
    }

    private boolean containsControlWithErrorKey(Node node, String errorKey) {
        if (node == null) {
            return false;
        }

        if (isControlNode(node)) {
            String candidate = keyResolver.resolveErrorKey(node);
            if (errorKey.equals(candidate)) {
                return true;
            }
        }

        for (Node child : node.getChildren()) {
            if (containsControlWithErrorKey(child, errorKey)) {
                return true;
            }
        }

        return false;
    }

    private boolean isControlNode(Node node) {
        return node.getNodeType() == NodeType.ELEMENT
                && node.getAttribute("name") != null
                && !node.getAttribute("name").isBlank();
    }
}