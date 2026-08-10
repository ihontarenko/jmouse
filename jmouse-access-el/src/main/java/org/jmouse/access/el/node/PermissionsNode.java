package org.jmouse.access.el.node;

import org.jmouse.access.policy.model.PolicyPermissionDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code permissions { … }} — the permissions this file says the installation has.
 *
 * <p>Permissions are bare strings everywhere else, which is fine while grants are written by code
 * holding a constant and not fine the moment somebody types {@code @SPACE frm:write} into a file: it
 * loads, it never matches, and nothing says so. Declaring them is what lets stage 2 answer.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PermissionsNode extends PolicyBlockNode {

    /**
     * Returns the declarations in this block, in the order they were written.
     *
     * @return the permissions this file states
     * @throws org.jmouse.access.el.PolicyParseException when the block holds anything else
     */
    public List<PolicyPermissionDeclaration> toPermissionDeclarations() {
        List<PolicyPermissionDeclaration> declarations = new ArrayList<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof PermissionDeclarationNode declaration) {
                declarations.add(declaration.toPermissionDeclaration());
            } else {
                throw reject(expression, "a permissions block holds only declarations like "
                        + "'form:read \"Read forms\"'");
            }
        }

        return declarations;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toPermissionDeclarations();
    }

    @Override
    protected String describe() {
        return "a permissions block";
    }

    @Override
    public String toSource() {
        return renderBlock("permissions");
    }

    @Override
    public String toString() {
        return "permissions";
    }
}
