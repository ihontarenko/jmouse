package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyScopeDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * One line of a {@code scopes} block: {@code @SPACE place parameter=spaceId}.
 *
 * <p>The nature is one of {@code everything}, {@code place} or {@code own-rows}. The parser does not
 * know what any of those mean — it reports the word it read, and binding maps it onto a nature the
 * engine understands.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScopeDeclarationNode extends AbstractExpression {

    private String name;
    private String nature;
    private String parameter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNature() {
        return nature;
    }

    public void setNature(String nature) {
        this.nature = nature;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    /**
     * Returns this declaration as the record stage 2 receives.
     *
     * @return the name, nature, optional parameter, and where it was written
     */
    public PolicyScopeDeclaration toScopeDeclaration() {
        return new PolicyScopeDeclaration(getName(), getNature(), getParameter(), SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toScopeDeclaration();
    }

    @Override
    public String toSource() {
        String declaration = "@%s %s".formatted(getName(), SourceWriter.name(getNature()));

        if (parameter == null) {
            return declaration;
        }

        return declaration + " parameter=" + SourceWriter.name(getParameter());
    }

    @Override
    public String toString() {
        return toSource();
    }
}
