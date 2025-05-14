package org.jmouse.el.node.expression.literal;

import org.jmouse.el.node.expression.LiteralNode;

/**
 * Represents a literal expression node that encapsulates a {@code Byte} value.
 */
public class ByteLiteralNode extends LiteralNode<Byte> {

    /**
     * Constructs a {@code ByteLiteralNode} with the specified {@code Byte} value.
     *
     * @param value the byte value to be encapsulated as a literal
     */
    public ByteLiteralNode(Byte value) {
        super(value);
    }
}
