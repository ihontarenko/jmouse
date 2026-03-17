package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

import java.util.function.Predicate;

/**
 * {@link ErrorTargetSelector} that searches for an ancestor node
 * matching the given predicate. 🔎
 *
 * <p>
 * Traverses the DOM tree upwards starting from the control node
 * and returns the first ancestor that satisfies the predicate.
 * If no matching ancestor is found, the control node itself
 * is returned.
 * </p>
 */
public final class AncestorErrorTargetSelector implements ErrorTargetSelector {

    private final Predicate<Node> predicate;

    /**
     * Creates selector using the given predicate.
     *
     * @param predicate node matching predicate
     */
    public AncestorErrorTargetSelector(Predicate<Node> predicate) {
        this.predicate = predicate;
    }

    /**
     * Resolves the closest matching ancestor for the control node.
     */
    @Override
    public Node resolve(Node control) {
        Node current = control;

        while (current != null) {
            if (predicate.test(current)) {
                return current;
            }
            current = current.getParent();
        }

        return control;
    }
}