package org.jmouse.access.policy;

import org.jmouse.access.Allowance;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.policy.model.PolicyPlan;
import org.jmouse.access.policy.model.PolicyPlanGrant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a bundle actually contains once {@code extends} has been followed — the plan equivalent of
 * resolving a role's bundle.
 *
 * <p>A tier is written as a difference from another one so that <em>"the same, without the
 * ceilings"</em> is not a copy that drifts from its original at the first edit. Everything above this
 * class wants the resolved set, and nothing above it should have to know inheritance happened.
 *
 * <h2>⚠️ Restating a line overrides it; there is no merging of two values</h2>
 *
 * <p>{@code storage-byte unlimited} in a derived plan replaces the parent's {@code 100GB} outright
 * rather than being reconciled with it. Any other rule — take the larger, take the narrower — would
 * make a derived plan's meaning depend on arithmetic the reader has to perform, and the whole reason
 * the catalogue is a document is that it can be read.
 *
 * <p>⚠️ <strong>Order is preserved from the base outward</strong>, so a rendering of a resolved plan
 * reads in the order somebody wrote it rather than in hash order.
 */
public final class PolicyPlans {

    private PolicyPlans() {
    }

    /**
     * Every capability a bundle confers, with inheritance applied.
     *
     * @param document the document the plan and its bases live in
     * @param code     the plan to resolve
     * @throws PolicyException where the plan is unknown, a base is unknown, or {@code extends} cycles
     */
    public static Map<String, Allowance> contentsOf(PolicyDocument document, String code) {
        return contentsOf(document, code, QuantityScale.PLAIN);
    }

    /**
     * Every capability a bundle confers, with inheritance applied and amounts read in the product's
     * own units.
     *
     * @param scale what {@code 100GB} means here — see {@link QuantityScale} for why the engine does
     *              not decide that
     */
    public static Map<String, Allowance> contentsOf(
            PolicyDocument document, String code, QuantityScale scale) {

        Map<String, Allowance> contents = new LinkedHashMap<>();

        for (PolicyPlan plan : lineageOf(document, code)) {
            for (PolicyPlanGrant grant : plan.grants()) {
                contents.put(grant.capability(),
                             Allowances.parse(grant.quantity(), grant.period(), grant.unlimited(), scale));
            }
        }

        return contents;
    }

    /**
     * The chain from the furthest base down to the plan itself.
     *
     * <p>Base first, so that applying them in order lets a derived plan overwrite what it inherited
     * simply by being applied later.
     */
    public static List<PolicyPlan> lineageOf(PolicyDocument document, String code) {
        List<PolicyPlan> lineage = new ArrayList<>();
        Set<String>      seen    = new LinkedHashSet<>();
        String           current = code;

        while (current != null) {
            if (!seen.add(current)) {
                throw new PolicyException(
                        "Plan '" + code + "' extends itself, through " + String.join(" → ", seen)
                        + " → " + current + ". A bundle that inherits from itself has no contents to "
                        + "resolve, and the file cannot say what it was meant to.");
            }

            PolicyPlan plan = document.plan(current).orElseThrow(() -> new PolicyException(
                    "No plan '" + current(seen) + "' in '" + document.name() + "'. Known plans: "
                    + describe(document) + "."));

            lineage.addFirst(plan);
            current = plan.isDerived() ? plan.extendsCode() : null;
        }

        return lineage;
    }

    /**
     * ⚠️ The most recently visited code, for the failure message.
     *
     * <p>Read from the set rather than from the loop variable because a lambda may only close over
     * something effectively final, and naming the wrong plan in a "no such plan" error sends the
     * reader to the wrong line.
     */
    private static String current(Set<String> seen) {
        return seen.isEmpty() ? "?" : List.copyOf(seen).getLast();
    }

    private static String describe(PolicyDocument document) {
        return document.plans().isEmpty()
                ? "none are declared"
                : String.join(", ", document.plans().stream().map(PolicyPlan::code).toList());
    }
}
