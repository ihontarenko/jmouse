package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

/**
 * Applies submission values to DOM controls. 🧩
 *
 * <p>
 * Implementations know how to assign a value to a specific
 * type of form control node (e.g. input, checkbox, select).
 * </p>
 */
public interface ControlValueApplier {

    /**
     * Returns whether this applier supports the given node.
     *
     * @param node control node
     *
     * @return {@code true} if the node type is supported
     */
    boolean supports(Node node);

    /**
     * Applies the provided value to the control node.
     *
     * @param node  control node
     * @param value submitted value
     */
    void apply(Node node, Object value);

}