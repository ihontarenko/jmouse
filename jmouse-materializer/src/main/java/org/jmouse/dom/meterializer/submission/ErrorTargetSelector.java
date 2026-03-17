package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

/**
 * Selects the DOM node used as the error target. 🎯
 *
 * <p>
 * Determines where validation errors for a given control
 * should be rendered in the DOM tree.
 * </p>
 */
public interface ErrorTargetSelector {

    /**
     * Resolves the node that should receive the error state
     * or error message for the given control.
     *
     * @param control form control node
     *
     * @return target node for error rendering
     */
    Node resolve(Node control);
}