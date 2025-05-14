package org.jmouse.el.node.expression.literal;

import org.jmouse.el.node.expression.LiteralNode;

import java.math.BigDecimal;

/**
 * Represents a literal expression node that encapsulates a {@code BigDecimal} value.
 */
public class BigDecimalLiteralNode extends LiteralNode<BigDecimal> {

    /**
     * Constructs a {@code BigDecimalLiteralNode} with the specified {@code BigDecimal} value.
     *
     * @param value the BigDecimal value to be encapsulated as a literal
     */
    public BigDecimalLiteralNode(BigDecimal value) {
        super(value);
    }
}
