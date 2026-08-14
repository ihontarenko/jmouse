package org.jmouse.access.el.node;

import org.jmouse.access.policy.model.PolicyVariableDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code variables { … }} — what is true of every call this installation decides.
 *
 * <p>The companion of {@link ActionsNode} and the correction of it. An action produces what its call
 * is <em>about</em>; a variable is simply there — on the routes that name an action and on the ones
 * that name none. While the two shared one list every action had to repeat every variable, and the
 * file ended up stating that a route produces the deployment name, which no route does.
 *
 * <p>Declared here they are stated once, a condition may read one under any action or under none, and
 * the reason for writing them down at all is {@link PermissionsNode}'s: a name nobody wrote down is
 * one a rule can misspell in silence.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class VariablesNode extends PolicyBlockNode {

    /**
     * Returns the declarations in this block, in the order they were written.
     *
     * @return the variables this file states
     * @throws org.jmouse.access.el.PolicyParseException when the block holds anything else
     */
    public List<PolicyVariableDeclaration> toVariableDeclarations() {
        List<PolicyVariableDeclaration> declarations = new ArrayList<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof VariableDeclarationNode declaration) {
                declarations.add(declaration.toVariableDeclaration());
            } else {
                throw reject(expression, "a variables block holds only declarations like "
                        + "'constant deployment \"Which deployment this is\"' or "
                        + "'dynamic ambientType \"What this workspace counts\"'");
            }
        }

        return declarations;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toVariableDeclarations();
    }

    @Override
    protected String describe() {
        return "a variables block";
    }

    @Override
    public String toSource() {
        return renderBlock("variables");
    }

    @Override
    public String toString() {
        return "variables";
    }
}
