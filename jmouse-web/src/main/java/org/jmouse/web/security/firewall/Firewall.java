package org.jmouse.web.security.firewall;

import org.jmouse.core.Sorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🛡️ Firewall engine that evaluates incoming requests against a list of {@link FirewallPolicy}.
 *
 * <p>Policies are sorted by priority (see {@link Sorter#PRIORITY_COMPARATOR}) and evaluated
 * in order. The first non-{@link Decision.Action#ALLOW} result becomes the effective decision.</p>
 *
 * <pre>{@code
 * List<FirewallPolicy> policies = List.of(
 *     new PathTraversalPolicy(),
 *     new SqlInjectionPolicy()
 * );
 *
 * Firewall fw = new Firewall(policies);
 *
 * Decision decision = fw.evaluate(new EvaluationInput(request));
 *
 * if (decision.isBlocked()) {
 *     response.sendError(d.statusCode());
 * }
 * }</pre>
 */
public class Firewall {

    /** Immutable ordered list of policies. */
    private final List<FirewallPolicy> policies;

    /**
     * Creates a firewall with given policies.
     * Policies are defensively copied and sorted by priority.
     *
     * @param policies list of firewall policies
     */
    public Firewall(List<FirewallPolicy> policies) {
        policies = new ArrayList<>(policies);
        policies.sort(Sorter.PRIORITY_COMPARATOR);
        this.policies = Collections.unmodifiableList(policies);
    }

    /**
     * Evaluates the input against all configured policies.
     *
     * @param evaluationInput request/context to analyze
     * @return {@link Decision} (ALLOW by default, or the last non-ALLOW decision)
     */
    public Decision evaluate(EvaluationInput evaluationInput) {
        Decision result = Decision.allow();

        for (FirewallPolicy policy : policies) {
            Decision decision = policy.apply(evaluationInput);
            if (decision != null && decision.action() != Decision.Action.ALLOW) {
                result = decision;
            }
        }

        return result;
    }

}
