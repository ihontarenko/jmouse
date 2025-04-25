package org.jmouse.el.node.expression.literal;

import org.jmouse.el.node.expression.LiteralNode;

/**
 * Represents a literal expression node that encapsulates a {@code Integer} value.
 */
public class IntegerLiteralNode extends LiteralNode<Integer> {

    /**
     * Constructs a {@code IntegerLiteralNode} with the specified {@code Integer} value.
     *
     * @param value the integer value to be encapsulated as a literal
     */
    public IntegerLiteralNode(Integer value) {
        super(value);
    }

}
