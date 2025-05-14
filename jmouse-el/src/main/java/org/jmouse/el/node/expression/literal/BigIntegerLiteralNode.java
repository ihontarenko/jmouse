package org.jmouse.el.node.expression.literal;

import org.jmouse.el.node.expression.LiteralNode;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents a literal expression node that encapsulates a {@code BigInteger} value.
 */
public class BigIntegerLiteralNode extends LiteralNode<BigInteger> {

    /**
     * Constructs a {@code BigIntegerLiteralNode} with the specified {@code BigInteger} value.
     *
     * @param value the BigInteger value to be encapsulated as a literal
     */
    public BigIntegerLiteralNode(BigInteger value) {
        super(value);
    }
}
