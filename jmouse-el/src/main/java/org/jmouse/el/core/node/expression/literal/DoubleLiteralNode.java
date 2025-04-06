package org.jmouse.el.core.node.expression.literal;

import org.jmouse.el.core.node.expression.LiteralNode;

/**
 * Represents a literal expression node that encapsulates a {@code Double} value.
 */
public class DoubleLiteralNode extends LiteralNode<Double> {

    /**
     * Constructs a {@code DoubleLiteralNode} with the specified {@code Double} value.
     *
     * @param value the double value to be encapsulated as a literal
     */
    public DoubleLiteralNode(Double value) {
        super(value);
    }
}
