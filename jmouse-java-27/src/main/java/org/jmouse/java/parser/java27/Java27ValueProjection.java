package org.jmouse.java.parser.java27;

import org.jmouse.el.translate.NodeOperation;
import org.jmouse.java.parser.model.Expression;
import org.jmouse.java.parser.projection.JavaProjectionContext;

public final class Java27ValueProjection
        implements NodeOperation<Java27ValueNode, JavaProjectionContext, Expression> {
    @Override
    public Expression apply(Java27ValueNode node, JavaProjectionContext context) {
        Expression child = context.expression(node.expression());
        return new Expression(
                child.kind(),
                child.name(),
                child.nameSpan(),
                node.span(),
                child.receiver(),
                child.arguments());
    }
}
