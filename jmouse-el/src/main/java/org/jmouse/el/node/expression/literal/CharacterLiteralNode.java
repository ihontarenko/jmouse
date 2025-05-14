package org.jmouse.el.node.expression.literal;

import org.jmouse.el.node.expression.LiteralNode;

/**
 * Represents a literal expression node that encapsulates a {@code Character} value.
 */
public class CharacterLiteralNode extends LiteralNode<Character> {

    /**
     * Constructs a {@code CharacterLiteralNode} with the specified {@code Character} value.
     *
     * @param value the char value to be encapsulated as a literal
     */
    public CharacterLiteralNode(Character value) {
        super(value);
    }
}
