package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

import java.util.function.Predicate;

public final class AncestorErrorTargetSelector implements ErrorTargetSelector {

    private final Predicate<Node> predicate;

    public AncestorErrorTargetSelector(Predicate<Node> predicate) {
        this.predicate = predicate;
    }

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