package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyGrant;
import org.jmouse.access.policy.model.PolicyRoleAssignment;
import org.jmouse.access.policy.model.PolicySubject;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code subject u-42 { … }} — one account's role assignments and personal grants.
 *
 * <p>A subject body holds {@code grants ROLE @SCOPE} lines and scoped permissions, in any order.
 * Unlike a role it may write an instance, an effect and a condition, because all three are decisions
 * about <em>this</em> account rather than about what a permission is worth.</p>
 *
 * <p>⚠️ The id may be a {@code ${placeholder}} and is kept verbatim; resolving it is stage 2's, at
 * load, against ordinary application configuration.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SubjectNode extends PolicyBlockNode {

    private final String id;

    public SubjectNode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Returns this subject as the record stage 2 receives.
     *
     * @return the id, its role assignments and personal grants, and where it was written
     * @throws org.jmouse.access.el.PolicyParseException when the body holds anything else
     */
    public PolicySubject toSubject() {
        List<PolicyRoleAssignment> roles  = new ArrayList<>();
        List<PolicyGrant>          grants = new ArrayList<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof RoleAssignmentNode assignment) {
                roles.add(assignment.toRoleAssignment());
            } else if (expression instanceof GrantNode grant) {
                grants.add(grant.toPolicyGrant());
            } else {
                throw reject(expression, "a subject body holds 'grants ROLE @SCOPE' lines "
                        + "and scoped permissions like '@SPACE:kyiv entry:read'");
            }
        }

        return new PolicySubject(getId(), roles, grants, SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toSubject();
    }

    @Override
    protected String describe() {
        return "a subject body";
    }

    @Override
    public String toSource() {
        return renderBlock("subject " + SourceWriter.name(getId()));
    }

    @Override
    public String toString() {
        return "subject " + getId();
    }
}
