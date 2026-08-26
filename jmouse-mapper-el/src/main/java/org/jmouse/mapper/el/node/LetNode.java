package org.jmouse.mapper.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * A named sub-expression: {@code let full = ucfirst(a) ~ " " ~ ucfirst(b)}.
 *
 * <p>⚠️ The expression is raw text for the same reason a rule's value is — see {@link RuleNode}.</p>
 *
 * <p>⚠️ A binding name may not collide with a readable property of the source. Shadowing is legal in
 * most languages and is a trap here, because a binding and a source path are both bare identifiers on
 * the same line, and the reader of the file cannot tell which one a name meant. The collision is
 * refused when the file is checked, not resolved by a precedence rule.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class LetNode extends AbstractExpression {

    private String name;
    private String expression;

    /** @return the name the expression is bound to within its block */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** @return the expression exactly as it was typed */
    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "let %s = %s".formatted(name, expression);
    }
}
