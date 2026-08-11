package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyPlan;
import org.jmouse.access.policy.model.PolicyPlanGrant;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code plan business "Business" order 30 extends team note "…" { … }} — one named bundle.
 *
 * <p>⚠️ <strong>Metadata is in the header and only capabilities are in the body.</strong> A body
 * holding both {@code note "…"} and {@code seat 5} would make the grammar depend on the product's
 * catalogue never containing a capability called {@code note} — and the catalogue is the product's to
 * write. Every line in the body is the same kind of line, so a parser never has to work out what a
 * bare word meant.</p>
 *
 * <p>⚠️ <strong>There is no deny here.</strong> A bundle may only give. Taking something away from one
 * customer is an act with a person and a reason behind it, and a catalogue entry carries neither —
 * that is a {@code deny} in {@code entitlements { }}, where it can be attributed and explained.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PlanNode extends PolicyBlockNode {

    private final String code;

    private String displayName;
    private int    order;
    private String note;
    private String extendsCode;

    public PlanNode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getExtendsCode() {
        return extendsCode;
    }

    public void setExtendsCode(String extendsCode) {
        this.extendsCode = extendsCode;
    }

    /**
     * Returns this bundle as the record stage 2 receives.
     *
     * @throws org.jmouse.access.el.PolicyParseException when the body holds anything but capabilities
     */
    public PolicyPlan toPlan() {
        List<PolicyPlanGrant> grants = new ArrayList<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof PlanGrantNode grant) {
                grants.add(grant.toPlanGrant());
            } else {
                throw reject(expression, "a plan body holds only capabilities and how much of each — "
                        + "'seat 5', 'storage-byte 100GB per month', 'entry unlimited' or a bare "
                        + "'custody'. Its name, order and note belong in the header");
            }
        }

        return new PolicyPlan(getCode(), getDisplayName(), getOrder(), getNote(), getExtendsCode(),
                              grants, SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toPlan();
    }

    @Override
    protected String describe() {
        return "a plan";
    }

    @Override
    public String toSource() {
        StringBuilder header = new StringBuilder("plan ").append(SourceWriter.name(getCode()));

        if (displayName != null) {
            header.append(' ').append(SourceWriter.literal(getDisplayName()));
        }
        if (order != 0) {
            header.append(" order ").append(order);
        }
        if (extendsCode != null) {
            header.append(" extends ").append(SourceWriter.name(getExtendsCode()));
        }
        if (note != null) {
            header.append(" note ").append(SourceWriter.literal(getNote()));
        }

        return renderBlock(header.toString());
    }

    @Override
    public String toString() {
        return "plan " + code;
    }
}
