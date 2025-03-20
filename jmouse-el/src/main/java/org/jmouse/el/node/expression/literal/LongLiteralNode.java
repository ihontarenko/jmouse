package org.jmouse.el.node.expression.literal;

import org.jmouse.el.node.expression.LiteralNode;

/**
 * Represents a literal expression node that encapsulates a {@code Long} value.
 */
public class LongLiteralNode extends LiteralNode<Long> {

    /**
     * Constructs a {@code LongLiteralNode} with the specified {@code Long} value.
     *
     * @param value the long value to be encapsulated as a literal
     */
    public LongLiteralNode(Long value) {
        super(value);
    }
}
