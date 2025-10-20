package org.jmouse.security.authorization.method;

import org.jmouse.el.node.Expression;
import org.jmouse.security.core.access.Phase;

public record AuthorizedExpressionAttribute(Phase phase, Expression expression) implements ExpressionAttribute {

}
