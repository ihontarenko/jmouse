package org.jmouse.security.authorization.method;

import org.jmouse.el.node.Expression;
import org.jmouse.security.core.access.Phase;

import java.lang.annotation.Annotation;

public record AnnotationExpressionAttribute<A extends Annotation>(Phase phase, Expression expression, A attribute)
        implements ExpressionAttribute<A> {

}
