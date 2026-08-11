package org.jmouse.access.el.node;

import org.jmouse.access.policy.model.PolicyEntitlement;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code entitlements { … }} — who is on what, and until when.
 *
 * <p>⚠️ <strong>A file may deny a row; it may never delete one.</strong> Everything in this block adds
 * to what the tables already say, because the document and the database are two stores whose grants
 * are concatenated. A block that could remove would let editing a document silently undo a decision
 * somebody else made on a screen.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class EntitlementsNode extends PolicyBlockNode {

    /**
     * Returns the lines in this block, in the order they were written.
     *
     * @throws org.jmouse.access.el.PolicyParseException when the block holds anything else
     */
    public List<PolicyEntitlement> toEntitlements() {
        List<PolicyEntitlement> entitlements = new ArrayList<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof EntitlementNode entitlement) {
                entitlements.add(entitlement.toEntitlement());
            } else {
                throw reject(expression, "an entitlements block holds lines like "
                        + "'@ORGANIZATION:acme plan team' or '@SPACE:warehouse deny custody "
                        + "reason \"…\"'");
            }
        }

        return entitlements;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toEntitlements();
    }

    @Override
    protected String describe() {
        return "an entitlements block";
    }

    @Override
    public String toSource() {
        return renderBlock("entitlements");
    }

    @Override
    public String toString() {
        return "entitlements";
    }
}
