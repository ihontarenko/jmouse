package org.jmouse.storage.policy;

import java.util.Objects;

/**
 * 🛃 One rule, everywhere — the answer for an installation that has exactly one.
 *
 * <p>What every product got before a destination could carry a policy of its own, and what every
 * product keeps until one does. Deliberately the cheapest possible implementation: one field, no
 * lookup, no cache to invalidate, nothing to go wrong on a path that runs on every upload.</p>
 *
 * <p>A module with a better answer publishes its own {@link UploadPolicyResolver} bean and this one
 * steps aside.</p>
 */
public final class FixedUploadPolicy implements UploadPolicyResolver {

    private final UploadPolicy policy;

    /**
     * 🏗️ Hold the one policy this installation applies.
     *
     * @param policy what may enter storage, anywhere in it
     */
    public FixedUploadPolicy(UploadPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "An upload policy resolver needs a policy.");
    }

    /**
     * 🛃 The same policy, whatever the destination.
     *
     * @param ownerType ignored
     * @param ownerId   ignored
     * @return the configured policy
     */
    @Override
    public UploadPolicy policyFor(String ownerType, String ownerId) {
        return policy;
    }

    /**
     * 🔎 The policy being applied, for a caller that needs it without a destination in hand.
     *
     * @return the configured policy
     */
    public UploadPolicy policy() {
        return policy;
    }
}
