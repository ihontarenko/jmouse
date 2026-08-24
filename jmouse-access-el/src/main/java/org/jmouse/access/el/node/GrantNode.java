package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyEffect;
import org.jmouse.access.policy.model.PolicyGrant;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

import static org.jmouse.access.policy.model.PolicyEffect.ALLOW;

/**
 * One scoped permission: {@code @SCOPE[:instance] permission [allow|deny] [when …]}.
 *
 * <p>The same five tokens mean two different things depending on where they are written — a bundle
 * entry inside a {@code role}, a grant inside a {@code subject} — so they parse to one node and the
 * enclosing block decides. {@link RoleNode} takes the narrow reading and refuses everything a bundle
 * cannot carry; {@link SubjectNode} takes the full one.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class GrantNode extends AbstractExpression {

    private SingleScopeNode scope;
    private String          permission;
    private String          condition;
    private String          reason;
    private PolicyEffect    effect;

    public SingleScopeNode getScope() {
        return scope;
    }

    public void setScope(SingleScopeNode scope) {
        this.scope = scope;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * Returns the condition source exactly as it was typed, or {@code null}.
     *
     * <p>⚠️ Raw text on purpose. Nothing here looks inside it: modelling the expression would be a
     * second implementation of a grammar jMouse EL already owns, and the day the two disagree is a
     * security incident. Stage 2 compiles it, and the control room shows this string back.</p>
     *
     * @return the condition as written, or {@code null} where none was
     */
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Whether {@code when …} was written.
     *
     * @return {@code true} when this grant is conditional
     */
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

    /**
     * Returns the effect as written, or {@code null} where none was.
     *
     * <p>Distinct from {@link #resolveEffect()}: a role may not carry an effect at all, and only the
     * unresolved value can tell "nothing was written" from "somebody wrote {@code allow}".</p>
     *
     * @return {@link PolicyEffect#ALLOW}, {@link PolicyEffect#DENY}, or {@code null}
     */
    public PolicyEffect getEffect() {
        return effect;
    }

    public void setEffect(PolicyEffect effect) {
        this.effect = effect;
    }

    /**
     * Returns the effect this grant carries, with {@code allow} implied where nothing was written.
     *
     * @return the effective effect, never {@code null}
     */
    public PolicyEffect resolveEffect() {
        return effect == null ? ALLOW : effect;
    }

    /**
     * Returns this grant as the record stage 2 receives.
     *
     * @return the grant, with its scope, effect, raw condition and position
     */
    public PolicyGrant toPolicyGrant() {
        return new PolicyGrant(
                getPermission(),
                getScope().toPolicyScope(),
                resolveEffect(),
                getCondition(),
                getReason(),
                SourceSpanNode.at(this)
        );
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toPolicyGrant();
    }

    @Override
    public String toSource() {
        StringBuilder builder = new StringBuilder(getScope().toSource()).append(' ').append(getPermission());

        if (effect != null) {
            builder.append(' ').append(effect.name().toLowerCase());
        }

        if (isConditional()) {
            builder.append(" when ").append(getCondition());
        }

        // ⚠️ Appended only when there is one, so a document without a reason is written back byte-for-byte
        // as it was. PolicySeedStep's checksum comes from PolicyWriter's output, so a writer that
        // respelled unchanged documents would fail every installation's bootstrap ledger exactly once —
        // for a feature none of them use.
        if (isExplained()) {
            builder.append(" reason ").append(SourceWriter.literal(getReason()));
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return toSource();
    }
}
