package org.jmouse.el.node.expression;

import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.test.HasAnyTest;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

import java.util.Collection;

public class InOperationNode extends AbstractExpression {

    private Expression left;
    private Expression right;

    public Expression getLeft() {
        return left;
    }

    public void setLeft(Expression left) {
        this.left = left;
    }

    public Expression getRight() {
        return right;
    }

    public void setRight(Expression right) {
        this.right = right;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Object[] array;
        Object   target = right.evaluate(context);

        if (target instanceof Collection<?> objects) {
            array = objects.toArray();
        } else if (target instanceof Object[] objects) {
            array = objects;
        } else {
            return false;
        }

        Object value = left.evaluate(context);

        return new HasAnyTest().test(
                value,
                Arguments.forArray(array),
                context,
                TypeInformation.forInstance(value)
        );
    }

    @Override
    public String toString() {
        return "IN[%s -> %s]".formatted(left, right);
    }

}
