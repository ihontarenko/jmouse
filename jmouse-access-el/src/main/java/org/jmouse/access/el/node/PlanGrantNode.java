package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyPlanGrant;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * One line of a plan's body: {@code storage-byte 100GB per month}, {@code seat unlimited}, or a bare
 * {@code custody}.
 *
 * <p>⚠️ <strong>The amount is kept as text.</strong> {@code 100GB} arrives from the lexer as two
 * tokens and is rejoined here into the string the file wrote; what {@code GB} <em>means</em> is
 * decided far later, by the product's {@code QuantityScale}. A parser that resolved it would own a
 * units table, and one that dropped the suffix would silently turn a hundred gigabytes into a hundred
 * bytes — a mistake invisible in the file and unnoticed until an invoice.</p>
 *
 * <p>⚠️ {@code unlimited} is a flag rather than a very large number, all the way down. A sentinel
 * would be indistinguishable from a real ceiling to every screen above, and a tier sold as
 * "unlimited" that renders as a number has lied to whoever is paying for it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PlanGrantNode extends AbstractExpression {

    private String  capability;
    private String  quantity;
    private String  period;
    private boolean unlimited;

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public void setUnlimited(boolean unlimited) {
        this.unlimited = unlimited;
    }

    public PolicyPlanGrant toPlanGrant() {
        return new PolicyPlanGrant(
                getCapability(), getQuantity(), getPeriod(), isUnlimited(), SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toPlanGrant();
    }

    @Override
    public String toSource() {
        StringBuilder source = new StringBuilder(SourceWriter.name(getCapability()));

        if (unlimited) {
            return source.append(" unlimited").toString();
        }

        if (quantity != null) {
            source.append(' ').append(quantity);
        }

        if (period != null) {
            source.append(" per ").append(period);
        }

        return source.toString();
    }

    @Override
    public String toString() {
        return toSource();
    }
}
