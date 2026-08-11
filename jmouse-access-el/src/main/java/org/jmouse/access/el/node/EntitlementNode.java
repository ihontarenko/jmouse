package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyEntitlement;
import org.jmouse.access.policy.model.PolicyScope;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * One line of an {@code entitlements} block — a bundle, a trial, an allow or a deny, addressed at a
 * place and optionally bounded in time.
 *
 * <p>⚠️ {@code from} and {@code until} are read as <strong>qualifiers on the grant</strong> rather
 * than folded into a condition. A condition is opaque — it answers and reports its own source, and
 * nothing looks inside — so a window written as one would leave the resolver unable to tell an
 * <em>expired</em> grant from a <em>refused</em> one, and nobody could be told their trial ended on
 * the 12th.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class EntitlementNode extends AbstractExpression {

    private PolicyScope            place;
    private PolicyEntitlement.Kind kind;
    private String                 subject;
    private String                 quantity;
    private boolean                unlimited;
    private String                 from;
    private String                 until;
    private String                 reason;

    public PolicyScope getPlace() {
        return place;
    }

    public void setPlace(PolicyScope place) {
        this.place = place;
    }

    public PolicyEntitlement.Kind getKind() {
        return kind;
    }

    public void setKind(PolicyEntitlement.Kind kind) {
        this.kind = kind;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public void setUnlimited(boolean unlimited) {
        this.unlimited = unlimited;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getUntil() {
        return until;
    }

    public void setUntil(String until) {
        this.until = until;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public PolicyEntitlement toEntitlement() {
        return new PolicyEntitlement(
                getPlace(), getKind(), getSubject(), getQuantity(), isUnlimited(),
                getFrom(), getUntil(), getReason(), SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toEntitlement();
    }

    @Override
    public String toSource() {
        StringBuilder source = new StringBuilder(place.toString())
                .append("  ").append(kind.name().toLowerCase())
                .append(' ').append(SourceWriter.name(getSubject()));

        if (unlimited) {
            source.append(" unlimited");
        } else if (quantity != null) {
            source.append(' ').append(quantity);
        }

        if (from != null) {
            source.append(" from ").append(from);
        }
        if (until != null) {
            source.append(" until ").append(until);
        }
        if (reason != null) {
            source.append(" reason ").append(SourceWriter.literal(getReason()));
        }

        return source.toString();
    }

    @Override
    public String toString() {
        return toSource();
    }
}
