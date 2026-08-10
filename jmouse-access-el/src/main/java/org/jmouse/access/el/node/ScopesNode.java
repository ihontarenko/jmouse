package org.jmouse.access.el.node;

import org.jmouse.access.policy.model.PolicyScopeDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code scopes { … }} — the floors this file says the installation has.
 *
 * <p>⚠️ <strong>Declaration order is width order.</strong> There is no rank beside each scope and
 * there must not be one: a rank states the same fact twice, and the day the two disagree the
 * covering chain reorders with nobody having touched it. Position in this block <em>is</em> the
 * rank, which is the rule an enum's declaration order already follows — so the list this returns is
 * ordered and stage 2 compares it by position.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScopesNode extends PolicyBlockNode {

    /**
     * Returns the declarations in this block, in the order they were written.
     *
     * @return the floors this file states, widest first
     * @throws org.jmouse.access.el.PolicyParseException when the block holds anything else
     */
    public List<PolicyScopeDeclaration> toScopeDeclarations() {
        List<PolicyScopeDeclaration> declarations = new ArrayList<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof ScopeDeclarationNode declaration) {
                declarations.add(declaration.toScopeDeclaration());
            } else {
                throw reject(expression, "a scopes block holds only declarations like "
                        + "'@SPACE place parameter=spaceId'");
            }
        }

        return declarations;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toScopeDeclarations();
    }

    @Override
    protected String describe() {
        return "a scopes block";
    }

    @Override
    public String toSource() {
        return renderBlock("scopes");
    }

    @Override
    public String toString() {
        return "scopes";
    }
}
