package org.jmouse.access;

import java.util.Optional;

/**
 * How much of a capability a grant confers — a number, or no ceiling at all.
 *
 * <h2>⚠️ Why "no ceiling" is not a very large number</h2>
 *
 * <p>The tempting shortcut is to write unlimited as {@code Long.MAX_VALUE} and let arithmetic take
 * care of the rest. It cannot, because the two are different <em>facts</em> and every screen above
 * this has to tell them apart: "5 of 25 workspaces" and "5 workspaces" are different sentences, and a
 * progress bar drawn against nine quintillion is a progress bar that is always empty. A tier sold as
 * "unlimited users" that renders as a number is a tier that has lied to the person paying for it.
 *
 * <p>So the absence of a ceiling is modelled, not encoded, and reading the number requires admitting
 * it might not be there — which is the point.
 *
 * <h2>What this type is not</h2>
 *
 * <p>It is not consumption. An allowance is the <em>ceiling</em>; how much has been used is a
 * counter, it is written on every metered action, and it belongs to whoever is doing the metering. An
 * authorization library that owned that counter would own a write on the hot path of every product
 * using it.
 *
 * @param quantity how much, or null for no ceiling
 * @param period   the window {@link #quantity} is counted over, or null where the capability is a
 *                 standing count rather than a consumed one
 */
public record Allowance(Long quantity, AllowancePeriod period) {

    private static final Allowance UNLIMITED = new Allowance(null, null);

    public Allowance {
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException(
                    "An allowance cannot be negative, and " + quantity + " is. A capability nobody may "
                    + "use at all is a DENY, which is a different fact with a reason attached to it.");
        }
    }

    /** No ceiling. What a tier means when it sells "unlimited". */
    public static Allowance unlimited() {
        return UNLIMITED;
    }

    /** A standing count — how many may exist, with no window to count it over. */
    public static Allowance of(long quantity) {
        return new Allowance(quantity, null);
    }

    /** A consumed quantity, counted over a window. */
    public static Allowance of(long quantity, AllowancePeriod period) {
        return new Allowance(quantity, period);
    }

    /** Whether this confers a ceiling at all. */
    public boolean isUnlimited() {
        return quantity == null;
    }

    /** The ceiling, where there is one. */
    public Optional<Long> ceiling() {
        return Optional.ofNullable(quantity);
    }

    /**
     * Whether this confers strictly more than {@code other} — how resolution chooses among the grants
     * that survive.
     *
     * <p><strong>Generosity is the rule here, and subtraction is still the last word.</strong> Two
     * allows are two promises, and honouring the smaller one silently breaks the larger; whereas taking
     * a capability away is a {@code DENY}, which removes it outright and never has to compete with a
     * number. So this is only ever asked about grants that already survived the deny pass.
     *
     * <p>⚠️ <strong>A question rather than a fold, deliberately.</strong> Folding two allowances into
     * the more generous one loses the grant it came from, and resolution needs that grant: a metered
     * capability is counted across the place its allowance sits at, so the number and the place have to
     * travel together. Folded, a ceiling bought for one place is silently applied to the population of
     * another.
     *
     * @param other the incumbent, or null where nothing has conferred a ceiling yet
     */
    public boolean isMoreGenerousThan(Allowance other) {
        if (other == null) {
            return true;
        }
        if (other.isUnlimited()) {
            return false;
        }

        return isUnlimited() || quantity > other.quantity;
    }

    /** What a screen prints where there is no room for a sentence. */
    public String describe() {
        if (isUnlimited()) {
            return "unlimited";
        }

        return period == null || period == AllowancePeriod.EVER
                ? String.valueOf(quantity)
                : quantity + " per " + period.name().toLowerCase();
    }
}
