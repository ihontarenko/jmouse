package org.jmouse.el.node.expression.literal;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.LiteralNode;

/**
 * Represents a literal expression node for string values.
 * <p>
 * This node encapsulates a constant string value and returns it upon evaluation.
 * </p>
 */
public class StringLiteralNode extends LiteralNode<String> {

    /**
     * Constructs a StringLiteralNode with the specified string value.
     *
     * @param value the string literal to encapsulate
     */
    public StringLiteralNode(String value) {
        super(value);
    }

    /**
     * Evaluates the literal string within the given evaluation context.
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        String value = (String) super.evaluate(context);

        if (value != null && !value.isBlank() && value.length() > 2) {
            value = value.substring(1, value.length() - 1);
        }

        return value;
    }
}
