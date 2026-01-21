package org.jmouse.crawler.runtime.politeness.defaults;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.PolitenessPolicy;
import org.jmouse.crawler.api.ProcessingTask;

import java.time.Instant;
import java.util.List;

/**
 * Composite {@link PolitenessPolicy} that combines multiple policies into a single decision. 🧩
 *
 * <p>This policy evaluates each configured {@link PolitenessPolicy} and returns the
 * <em>most restrictive</em> eligibility instant, i.e. the maximum (latest) instant returned
 * by any policy.</p>
 *
 * <p>Interpretation:</p>
 * <ul>
 *   <li>Each policy returns the earliest instant at which the request becomes eligible.</li>
 *   <li>The composite returns the latest of those instants to ensure all constraints are satisfied.</li>
 * </ul>
 *
 * <p>Null handling:</p>
 * <ul>
 *   <li>If a child policy returns {@code null}, it is treated as "no additional restriction".</li>
 *   <li>If {@code now} is {@code null}, the composite simply propagates it to children
 *       (but callers should avoid passing {@code null}).</li>
 * </ul>
 *
 * <p>Thread-safety depends on the thread-safety of the underlying policy implementations.</p>
 */
public final class CompositePolitenessPolicy implements PolitenessPolicy {

    private final List<PolitenessPolicy> policies;

    /**
     * @param policies ordered list of policies to apply (must be non-null)
     */
    public CompositePolitenessPolicy(List<PolitenessPolicy> policies) {
        this.policies = List.copyOf(Verify.nonNull(policies, "policies"));
    }

    /**
     * Compute the earliest instant at which the request becomes eligible under
     * all configured policies.
     *
     * <p>The returned value is the maximum instant among all policy results.</p>
     *
     * @param task the request URL
     * @param now current time reference
     * @return the composite eligibility instant (may equal {@code now})
     */
    @Override
    public Instant eligibleAt(ProcessingTask task, Instant now) {
        Instant eligibleAt = now;

        for (PolitenessPolicy policy : policies) {
            // Each policy may depend on "now" as a reference point; we keep it stable.
            Instant candidate = policy.eligibleAt(task, now);

            if (candidate != null && (eligibleAt == null || candidate.isAfter(eligibleAt))) {
                eligibleAt = candidate;
            }
        }

        return eligibleAt;
    }
}
