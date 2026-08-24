package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyRoleAssignment;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * {@code grants SPACE_ADMIN @SPACE:kyiv} — this subject holds that role, there.
 *
 * <p>The scope is where the assignment lands, and it is what turns a role's {@code @SPACE} reach
 * into a concrete workspace. Whether the role exists, and whether naming an instance makes sense for
 * that kind of scope, are both stage 2's to answer.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RoleAssignmentNode extends AbstractExpression {

    private String          roleName;
    private SingleScopeNode scope;
    private String          condition;
    private String          reason;

    public String getRoleName() {
        return roleName;
    }

    /** What narrows this handing-out, verbatim as it was typed, or null. */
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public boolean isConditional() {
        return condition != null && !condition.isBlank();
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isExplained() {
        return reason != null && !reason.isBlank();
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public SingleScopeNode getScope() {
        return scope;
    }

    public void setScope(SingleScopeNode scope) {
        this.scope = scope;
    }

    /**
     * Returns this assignment as the record stage 2 receives.
     *
     * @return the role name, the scope it lands in, what narrows it, and where it was written
     */
    public PolicyRoleAssignment toRoleAssignment() {
        return new PolicyRoleAssignment(
                getRoleName(), getScope().toPolicyScope(), getCondition(), getReason(),
                SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toRoleAssignment();
    }

    @Override
    public String toSource() {
        String written = "grants %s %s".formatted(SourceWriter.name(getRoleName()), getScope().toSource());

        // ⚠️ Verbatim, never re-rendered. The control room's revert writes this back, and a condition
        // respelled is a line the administrator who wrote it cannot find again.
        String withCondition = isConditional() ? written + " when " + getCondition() : written;

        // Only when there is one — see GrantNode.toSource for why byte-for-byte matters here.
        return isExplained()
                ? withCondition + " reason " + SourceWriter.literal(getReason())
                : withCondition;
    }

    @Override
    public String toString() {
        return toSource();
    }
}
