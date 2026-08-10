package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyScope;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * A scope as written: {@code @SPACE}, or {@code @SPACE:kyiv}.
 *
 * <p>The instance is kept exactly as it appeared, including a {@code ${placeholder}} — resolving one
 * needs a property source, which is the dependency this stage exists to keep out.</p>
 *
 * <p>⚠️ Whether an instance is <em>allowed</em> here is not this node's call. Inside a role it is a
 * privilege escalation and {@link RoleNode} refuses it; inside a subject a place written without one
 * is the same mistake and stage 2 refuses that, because only stage 2 knows which kinds are places.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SingleScopeNode extends AbstractExpression {

    private String kind;
    private String instance;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    /**
     * Whether an instance was written after the kind.
     *
     * @return {@code true} for the {@code @SPACE:kyiv} form
     */
    public boolean namesAnInstance() {
        return instance != null && !instance.isBlank();
    }

    /**
     * Returns this reference as the record stage 2 receives.
     *
     * @return the scope, with a {@code null} instance for the kind-only form
     */
    public PolicyScope toPolicyScope() {
        return new PolicyScope(getKind(), getInstance());
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toPolicyScope();
    }

    @Override
    public String toSource() {
        if (!namesAnInstance()) {
            return "@" + getKind();
        }

        return "@%s:%s".formatted(getKind(), SourceWriter.name(getInstance()));
    }

    @Override
    public String toString() {
        return toSource();
    }

}
