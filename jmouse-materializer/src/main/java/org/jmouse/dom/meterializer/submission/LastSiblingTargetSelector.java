package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;
import org.jmouse.util.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TargetSelector} that places feedback after the last sibling branch
 * representing the same logical field. 🧭
 *
 * <p>
 * Useful for grouped controls such as radio buttons or checkbox collections,
 * where multiple DOM branches correspond to the same logical error key.
 * </p>
 *
 * <p>
 * Resolution algorithm:
 * </p>
 * <ol>
 *     <li>Resolve the logical error key for the control</li>
 *     <li>Walk ancestor nodes upward</li>
 *     <li>For each parent, collect child branches containing that key</li>
 *     <li>If multiple matching branches exist, return the last one</li>
 *     <li>Otherwise fallback to the configured selector</li>
 * </ol>
 */
public final class LastSiblingTargetSelector implements TargetSelector {

    private final FieldKeyResolver keyResolver;
    private final TargetSelector   fallback;

    /**
     * Creates selector with {@link SelfTargetSelector} fallback.
     *
     * @param keyResolver field key resolver
     */
    public LastSiblingTargetSelector(FieldKeyResolver keyResolver) {
        this(keyResolver, new SelfTargetSelector());
    }

    /**
     * Creates selector with explicit fallback.
     *
     * @param keyResolver field key resolver
     * @param fallback    fallback selector
     */
    public LastSiblingTargetSelector(
            FieldKeyResolver keyResolver,
            TargetSelector fallback
    ) {
        this.keyResolver = keyResolver;
        this.fallback = fallback;
    }

    /**
     * Resolves the last sibling branch matching the same logical error key.
     *
     * @param control control node
     *
     * @return target node for feedback placement
     */
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

    /**
     * Collects direct child branches that contain a control
     * with the given logical error key.
     */
    private List<Node> matchingChildren(Node parent, String errorKey) {
        List<Node> matches = new ArrayList<>();

        for (Node child : parent.getChildren()) {
            if (containsNeeded(child, errorKey)) {
                matches.add(child);
            }
        }

        return matches;
    }

    /**
     * Returns whether the given subtree contains a control node
     * matching the requested error key.
     */
    private boolean containsNeeded(Node node, String errorKey) {
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
            if (containsNeeded(child, errorKey)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether the node looks like a form control.
     *
     * <p>
     * The default heuristic treats nodes with a non-empty {@code name}
     * attribute as controls.
     * </p>
     */
    private boolean isControlNode(Node node) {
        return Strings.isNotEmpty(node.getAttribute("name"));
    }
}