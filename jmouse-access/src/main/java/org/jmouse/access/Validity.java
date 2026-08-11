package org.jmouse.access;

import java.time.Instant;

/**
 * When a grant applies from, and when it stops.
 *
 * <h2>⚠️ Why this is a field and not a condition</h2>
 *
 * <p>A validity window is expressible as a predicate — {@code now >= x and now < y} — and modelling it
 * that way would cost nothing to write and everything to read. A {@code GrantCondition} is opaque on
 * purpose: it answers {@code holds()} and reports its own {@code source()}, and nothing looks inside.
 * Put a window in one and the resolver stops being able to tell an <em>expired</em> grant from a
 * <em>refused</em> one — so an editor cannot grey it out, a lint cannot warn that a rule died in
 * September, and a customer cannot be told their trial ended on the 12th instead of being shown a
 * capability that silently stopped existing.
 *
 * <p>It is also why {@code ConditionContext} carries no clock and must not gain one: an authorization
 * predicate compares what it was handed to what the rule says, and the moment it can read the time,
 * every rule's answer depends on something the rule does not mention.
 *
 * <h2>⚠️ Expiry is reading a date, never a job</h2>
 *
 * <p>Nothing expires by being written to. A grant that has run out is a row whose {@link #until} is
 * in the past, still there and still readable. A sweeper that marked them instead would, on the day
 * it failed to run, hand somebody an unlimited month — and would destroy the only record of what they
 * used to have.
 *
 * @param from  when it starts applying, or null for "always has"
 * @param until when it stops, or null for "no end"
 */
public record Validity(Instant from, Instant until) {

    private static final Validity FOREVER = new Validity(null, null);

    public Validity {
        if (from != null && until != null && !until.isAfter(from)) {
            throw new IllegalArgumentException(
                    "A validity window has to end after it starts, and " + from + " … " + until
                    + " does not. A grant that never applies is a grant nobody can find the mistake in.");
        }
    }

    /** No window — the ordinary grant, applying from the moment it exists until somebody removes it. */
    public static Validity forever() {
        return FOREVER;
    }

    /** Applies until a moment, and from now. */
    public static Validity until(Instant until) {
        return new Validity(null, until);
    }

    /** Applies between two moments. */
    public static Validity between(Instant from, Instant until) {
        return new Validity(from, until);
    }

    /** Whether this grant applies at a given moment. */
    public boolean holdsAt(Instant moment) {
        if (from != null && moment.isBefore(from)) {
            return false;
        }

        return until == null || moment.isBefore(until);
    }

    /** Whether the window has closed — which a screen renders differently from "never applied". */
    public boolean hasExpiredBy(Instant moment) {
        return until != null && !moment.isBefore(until);
    }

    /** Whether the window has not opened yet. */
    public boolean startsAfter(Instant moment) {
        return from != null && moment.isBefore(from);
    }

    public boolean isBounded() {
        return from != null || until != null;
    }
}
