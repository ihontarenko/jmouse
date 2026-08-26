package org.jmouse.mapper.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * One {@code include name} line — a block pulling in a file-level fragment.
 *
 * <h2>⚠️ A node rather than the bare string it used to be</h2>
 *
 * <p>The name alone was enough to resolve the fragment, and not enough to report anything: a file that
 * includes a fragment nobody declared was refused at {@code 0:0}, in a file whose lines are the only
 * thing the reader can act on. A node carries the position the parser stamped, which is what every other
 * construction in this language already does.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class IncludeNode extends AbstractExpression {

    private String name;

    /** @return the fragment this line pulls in */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "include %s".formatted(name);
    }
}
