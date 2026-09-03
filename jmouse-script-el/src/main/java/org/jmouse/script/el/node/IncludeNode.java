package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * {@code include 'common.jms'} — composition, recorded and not followed.
 *
 * <p>⚠️ The parser does not read the included file. It stores the path, because "relative to what" is a
 * loader's question and a cycle has to be detected across a whole load rather than inside one parse —
 * neither of which a parser following includes could do.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class IncludeNode extends AbstractExpression {

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return getPath();
    }

    @Override
    public String toSource() {
        return "include '" + getPath() + "'";
    }

    @Override
    public String toString() {
        return toSource();
    }

}
