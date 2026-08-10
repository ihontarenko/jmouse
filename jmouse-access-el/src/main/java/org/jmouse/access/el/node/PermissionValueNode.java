package org.jmouse.access.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * A permission as written: {@code form:read}, or {@code form:*}.
 *
 * <p>⚠️ The wildcard is stored as text and expanded by stage 2, at load, against the permission
 * catalogue. Expansion happens then rather than per request so the grant set stays concrete: a
 * wildcard matched on every decision would make <em>"what does this person hold"</em> unanswerable,
 * which is the one question the control room exists to answer.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PermissionValueNode extends AbstractExpression {

    private String namespace;
    private String action;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Returns the permission as one string, the form every other stage speaks.
     *
     * @return {@code namespace:action}
     */
    public String getPermission() {
        return getNamespace() + ":" + getAction();
    }

    /**
     * Whether this names a whole namespace rather than one permission.
     *
     * @return {@code true} for the {@code form:*} form
     */
    public boolean isWildcard() {
        return "*".equals(action);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return getPermission();
    }

    @Override
    public String toSource() {
        return getPermission();
    }

    @Override
    public String toString() {
        return getPermission();
    }
}
