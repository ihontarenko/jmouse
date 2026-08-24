package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

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

    /**
     * Answers whether the left value is one of the elements on the right.
     *
     * <p>⚠️ <strong>The membership is tested here rather than delegated to {@code hasAny}, and that is
     * the whole point of this method.</strong> Handing an already-unpacked collection to that test as
     * <em>varargs</em> let it re-interpret a single argument as a container of its own — so a
     * one-element list of one string was expanded into that string's characters, and
     * {@code 'abc' in ['abc']} answered <em>false</em> while {@code 'a' in ['abc']} answered
     * <em>true</em>. Both silently. That unwrapping is correct behaviour for {@code hasAny}, whose
     * contract is variadic; it was simply never the question this operator asks.</p>
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Collection<?> elements;
        Object        target = right.evaluate(context);

        if (target instanceof Collection<?> collection) {
            elements = collection;
        } else if (target instanceof Object[] array) {
            elements = Arrays.asList(array);
        } else {
            return false;
        }

        Object value = left.evaluate(context);

        for (Object element : elements) {
            if (Objects.equals(value, element)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toSource() {
        return "%s in %s".formatted(left.toSource(), right.toSource());
    }

    @Override
    public String toString() {
        return "IN[%s -> %s]".formatted(left, right);
    }

}
