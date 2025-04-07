package org.jmouse.el.node.expression.literal;

import org.jmouse.el.node.expression.LiteralNode;

/**
 * Represents a literal expression node that encapsulates a {@code Boolean} value.
 */
public class BooleanLiteralNode extends LiteralNode<Boolean> {

    /**
     * Constructs a {@code BooleanLiteralNode} with the specified {@code Boolean} value.
     *
     * @param value the boolean value to be encapsulated as a literal
     */
    public BooleanLiteralNode(Boolean value) {
        super(value);
    }
}
