package org.jmouse.access.el.node;

import org.jmouse.access.policy.model.PolicyPlan;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code plans { … }} — every bundle this installation offers.
 *
 * <p>A bundle is to a capability what a role is to a permission, which is the whole reason the word
 * carries no money: whether a name meant a subscription, an internal allocation or a pilot is
 * provenance recorded on the grants it issues, and nothing here needs to know which.</p>
 *
 * <p>⚠️ {@code extends} is resolved by {@code PolicyPlans}, not here. A parser reports what the file
 * said; following an inheritance chain needs the whole document — including bundles declared in
 * another file that has not been merged yet.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PlansNode extends PolicyBlockNode {

    /**
     * Returns the bundles in this block, in the order they were written.
     *
     * @throws org.jmouse.access.el.PolicyParseException when the block holds anything else
     */
    public List<PolicyPlan> toPlans() {
        List<PolicyPlan> plans = new ArrayList<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof PlanNode plan) {
                plans.add(plan.toPlan());
            } else {
                throw reject(expression, "a plans block holds 'plan <code> \"Name\" { … }' bundles "
                        + "and nothing else");
            }
        }

        return plans;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toPlans();
    }

    @Override
    protected String describe() {
        return "a plans block";
    }

    @Override
    public String toSource() {
        return renderBlock("plans");
    }

    @Override
    public String toString() {
        return "plans";
    }
}
