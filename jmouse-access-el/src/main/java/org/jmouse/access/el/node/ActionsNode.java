package org.jmouse.access.el.node;

import org.jmouse.access.policy.model.PolicyActionDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code actions { … }} — what this installation's calls say they are doing.
 *
 * <p>An action is <em>what is being done</em>, as against a permission, which is what somebody
 * <em>may</em> do. One permission covers several routes that are not equally safe, and this block is
 * the vocabulary a rule uses to tell them apart.
 *
 * <p>It exists for the reason {@link PermissionsNode} does: an action is a bare string everywhere
 * else, and the moment somebody types {@code action == 'entry.listByPurpos'} into a file it loads, it
 * never matches, and nothing says so — except that this failure is worse than a permission's, because
 * a rule that never fires is indistinguishable from a rule that is working.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ActionsNode extends PolicyBlockNode {

    /**
     * Returns the declarations in this block, in the order they were written.
     *
     * @return the actions this file states
     * @throws org.jmouse.access.el.PolicyParseException when the block holds anything else
     */
    public List<PolicyActionDeclaration> toActionDeclarations() {
        List<PolicyActionDeclaration> declarations = new ArrayList<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof ActionDeclarationNode declaration) {
                declarations.add(declaration.toActionDeclaration());
            } else {
                throw reject(expression, "an actions block holds only declarations like "
                        + "'entry.listByPurpose \"List one purpose\" produces purpose'");
            }
        }

        return declarations;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toActionDeclarations();
    }

    @Override
    protected String describe() {
        return "an actions block";
    }

    @Override
    public String toSource() {
        return renderBlock("actions");
    }

    @Override
    public String toString() {
        return "actions";
    }
}
