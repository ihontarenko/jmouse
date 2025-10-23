package org.jmouse.security.authorization;

import org.jmouse.el.node.Expression;

import java.util.Map;

public class ExpressionAccessResult extends AccessResult.AbstractAccessResult {

    public ExpressionAccessResult(boolean granted, Expression expression) {
        this(granted, expression.toString(), Map.of("expression", expression));
    }

    public ExpressionAccessResult(boolean granted, String message, Map<String, Object> attributes) {
        super(granted, message, attributes);
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
