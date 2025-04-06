package org.jmouse.el.core.node.expression.literal;

import org.jmouse.el.core.node.expression.LiteralNode;

/**
 * Represents a literal expression node that encapsulates a {@code null} value.
 * <p>
 * This node always evaluates to {@code null} and is used to represent the literal null value
 * in an expression.
 * </p>
 */
public class NullLiteralNode extends LiteralNode<Object> {

    /**
     * Constructs a NullLiteralNode, representing a literal null value.
     */
    public NullLiteralNode() {
        super(null);
    }
}
