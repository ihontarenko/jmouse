package org.jmouse.security.authorization.method.attribute;

import org.jmouse.el.node.Expression;
import org.jmouse.security.authorization.method.ExpressionAttribute;

/**
 * 🧪 Factory used by generic resolvers to wrap compiled EL into your concrete attribute type.
 */
@FunctionalInterface
public interface ExpressionAttributeFactory {
    ExpressionAttribute create(Expression expression);
}