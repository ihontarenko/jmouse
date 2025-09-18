package org.jmouse.core.cache;

/**
 * 🧠 TinyLFU admission policy composed from a {@link SeenFilter} and a {@link FrequencyEstimator}.
 *
 * <p>Usage:
 * <ul>
 *   <li>Call {@link #recordAccess(Object)} on every access (get/put).</li>
 *   <li>When a new candidate would cause eviction, call
 *       {@link #shouldAdmit(Object, Object)} with candidate and current victim.</li>
 *   <li>Periodically call {@link #maintenance()} to age the estimator and reset the filter.</li>
 * </ul>
 *
 * @param <K> key type
 */
public final class TinyLFU<K> {

    private final SeenFilter<K>         filter;
    private final FrequencyEstimator<K> estimator;

    public TinyLFU(SeenFilter<K> filter, FrequencyEstimator<K> estimator) {
        this.filter = filter;
        this.estimator = estimator;
    }

    /**
     * Records an access; first-seen keys are filtered, repeated keys update the estimator.
     */
    public void recordAccess(K key) {
        KeyWrapper<K> wrapped = KeyWrapper.of(key);
        if (filter.pass(wrapped)) {
            estimator.record(wrapped);
        }
    }

    /**
     * Admission rule: admit only if the candidate's estimated frequency is strictly
     * greater than the victim's. Using ">" avoids needless churn when equal.
     */
    public boolean shouldAdmit(K candidateKey, K victimKey) {
        KeyWrapper<K> candidate = KeyWrapper.of(candidateKey);
        KeyWrapper<K> victim    = KeyWrapper.of(victimKey);

        int candidateEstimate = estimator.estimate(candidate);
        int victimEstimate    = estimator.estimate(victim);

        return candidateEstimate > victimEstimate;
    }

    /**
     * Ages the estimator and clears the doorkeeper to stay adaptive.
     */
    public void maintenance() {
        estimator.age();
        filter.reset();
    }
}
