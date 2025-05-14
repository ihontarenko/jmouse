package org.jmouse.el.node.expression.literal;

import org.jmouse.el.node.expression.LiteralNode;

/**
 * Represents a literal expression node that encapsulates a {@code Short} value.
 */
public class ShortLiteralNode extends LiteralNode<Short> {

    /**
     * Constructs a {@code ShortLiteralNode} with the specified {@code Short} value.
     *
     * @param value the short value to be encapsulated as a literal
     */
    public ShortLiteralNode(Short value) {
        super(value);
    }
}
