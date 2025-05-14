package org.jmouse.el.node.expression.literal;

import org.jmouse.el.node.expression.LiteralNode;

/**
 * Represents a literal expression node that encapsulates a {@code Float} value.
 */
public class FloatLiteralNode extends LiteralNode<Float> {

    /**
     * Constructs a {@code FloatLiteralNode} with the specified {@code Float} value.
     *
     * @param value the float value to be encapsulated as a literal
     */
    public FloatLiteralNode(Float value) {
        super(value);
    }
}
