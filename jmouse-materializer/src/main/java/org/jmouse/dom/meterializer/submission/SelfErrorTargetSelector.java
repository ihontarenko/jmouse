package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

/**
 * {@link ErrorTargetSelector} that returns the control node itself. 🎯
 *
 * <p>
 * This selector does not perform any traversal or lookup.
 * The error target is the provided control node.
 * </p>
 *
 * <p>
 * Typically used as a fallback when no specialized
 * error target can be determined.
 * </p>
 */
public final class SelfErrorTargetSelector implements ErrorTargetSelector {

    /**
     * Returns the control node as the error target.
     */
    @Override
    public Node resolve(Node control) {
        return control;
    }

}