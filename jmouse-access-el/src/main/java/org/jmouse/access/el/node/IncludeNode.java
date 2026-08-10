package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyInclude;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * {@code include 'roles/workspace.jmp'} — composition, recorded and not followed.
 *
 * <p>⚠️ The parser does not read the included file. It stores the path, because "relative to what"
 * is a loader's question and a cycle has to be detected across a whole load rather than inside one
 * parse — neither of which a parser following includes could do.</p>
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

    /**
     * Returns this include as the record stage 2 receives.
     *
     * @return the path, and where it was written
     */
    public PolicyInclude toInclude() {
        return new PolicyInclude(getPath(), SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toInclude();
    }

    @Override
    public String toSource() {
        return "include " + SourceWriter.literal(getPath());
    }

    @Override
    public String toString() {
        return toSource();
    }

}
