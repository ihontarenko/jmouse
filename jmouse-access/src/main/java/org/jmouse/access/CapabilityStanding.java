package org.jmouse.access;

import org.jmouse.access.spi.CapabilityGrant;
import org.jmouse.access.spi.CapabilityProvenance;

import java.time.Instant;
import java.util.Optional;

/**
 * Where one capability stands for one subject at one place — and, where it does not stand, the
 * sentence explaining that.
 *
 * <h2>⚠️ Five outcomes, not two</h2>
 *
 * <p>A boolean would collapse the reasons together, and the reasons are the entire value of this
 * axis. <em>"An administrator withheld this"</em> is somebody to go and see; <em>"your trial ended on
 * the 12th"</em> is a date to quote; <em>"this opens on Monday"</em> is something to wait for; and
 * <em>"nobody has ever given you this"</em> is none of the three. The engine can tell them apart
 * because an expired grant is still a row it read, and a withheld one is a denial carrying words — so
 * nothing here has to be guessed from an absence.
 *
 * <p>⚠️ Note which sentence is <strong>not</strong> in that list: <em>"your plan does not include
 * this"</em>. {@link Outcome#UNGRANTED} is an absence, and whether an absence means <em>free</em> or
 * means <em>unpaid for</em> is a fact about a product's catalogue, which this engine does not have.
 * The product turns the absence into its own words.
 *
 * <h2>⚠️ The place is part of the answer, not decoration</h2>
 *
 * <p>{@link #at} is where the grant that decided this sits, and a metering product cannot do without
 * it: an allowance of 25 held at an account is counted across everything inside that account, and the
 * same allowance held at one place inside it counts only that one. Dropping it would leave the product
 * resolving the grant a second time, beside the engine, to recover a fact the engine already had —
 * which is the duplication this axis exists to end.
 *
 * <h2>⚠️ The window travels with the verdict</h2>
 *
 * <p>{@link #validity} is the deciding grant's window, and without it three of the five outcomes cannot
 * say the only thing worth saying about themselves. <em>"Your trial ended"</em> is a shrug;
 * <em>"your trial ended on the 12th"</em> is a date somebody can act on, and <em>"this opens on
 * Monday"</em> is the difference between waiting and buying. The engine has the window in hand while it
 * decides, so reporting the outcome without it would send every reader back to the store for a row the
 * resolver already read.
 *
 * @param capability what this is about
 * @param outcome    how it stands
 * @param allowance  the ceiling, where it is granted and metered — otherwise null
 * @param at         where the deciding grant sits, or null where nothing decided
 * @param validity   the deciding grant's window, or null where nothing decided
 * @param provenance what granted it, or what refused it, where anything did
 * @param reason     the words recorded against the grant that decided this, where there were any
 */
public record CapabilityStanding(
        String               capability,
        Outcome              outcome,
        Allowance            allowance,
        ScopeReference       at,
        Validity             validity,
        CapabilityProvenance provenance,
        String               reason
) {

    /**
     * How a capability stands.
     *
     * <p>⚠️ The order matters to nothing here and must not be read as precedence — resolution decides
     * the outcome, this only names it. Precedence is one sentence and it lives in
     * {@code CapabilityResolver}: deny wins, subtraction last.
     */
    public enum Outcome {

        /** Open, and — where metered — up to {@link #allowance}. */
        GRANTED,

        /** Somebody took it away on purpose, and {@link #reason} says why. */
        WITHHELD,

        /** It was granted, and the window has closed. The grant is still readable. */
        EXPIRED,

        /** It was granted, and the window has not opened yet. */
        NOT_YET,

        /** Nothing ever granted it. Not a refusal — an absence. */
        UNGRANTED
    }

    /** Whether the capability is available right now. */
    public boolean isGranted() {
        return outcome == Outcome.GRANTED;
    }

    /**
     * Whether anything was ever granted here.
     *
     * <p>What separates <em>"not included"</em> from <em>"it ran out"</em> on a screen — and the two
     * call for different next moves, so a screen that cannot tell them apart offers the wrong one.
     */
    public boolean wasEverGranted() {
        return outcome != Outcome.UNGRANTED;
    }

    /** The ceiling, where this is granted and carries one. */
    public Optional<Allowance> ceiling() {
        return Optional.ofNullable(allowance);
    }

    /** Where the deciding grant sits — what a metered capability is counted across. */
    public Optional<ScopeReference> decidedAt() {
        return Optional.ofNullable(at);
    }

    /** When the deciding grant stops, where it stops. The date an expired or pending outcome quotes. */
    public Optional<Instant> until() {
        return validity == null ? Optional.empty() : Optional.ofNullable(validity.until());
    }

    /**
     * Open, up to {@code allowance} where it is metered.
     *
     * @param governing the widest surviving allow — the one that supplies the place, the window and
     *                  the words
     */
    public static CapabilityStanding granted(String capability, Allowance allowance,
                                             CapabilityGrant governing) {
        return decidedBy(capability, Outcome.GRANTED, allowance, governing);
    }

    /** Refused on purpose — {@code reason} is the whole point and is carried from the grant. */
    public static CapabilityStanding withheld(String capability, CapabilityGrant denial) {
        return decidedBy(capability, Outcome.WITHHELD, null, denial);
    }

    /** Granted once, and the window has closed. The expired grant is named so a date can be quoted. */
    public static CapabilityStanding expired(String capability, CapabilityGrant lapsed) {
        return decidedBy(capability, Outcome.EXPIRED, null, lapsed);
    }

    /** Granted, and the window has not opened. Something to wait for rather than something to buy. */
    public static CapabilityStanding notYet(String capability, CapabilityGrant pending) {
        return decidedBy(capability, Outcome.NOT_YET, null, pending);
    }

    /** Nothing ever granted it — so there is no place, no window and nothing anybody wrote down. */
    public static CapabilityStanding ungranted(String capability) {
        return new CapabilityStanding(capability, Outcome.UNGRANTED, null, null, null, null, null);
    }

    /**
     * Every outcome that something decided, built the one way.
     *
     * <p>Four of the five differ only in which outcome they name and whether a ceiling survives, and
     * writing them out separately is how one of them comes to be missing a field the others carry —
     * which is exactly what happened to the window.
     */
    private static CapabilityStanding decidedBy(
            String capability, Outcome outcome, Allowance allowance, CapabilityGrant deciding) {

        return new CapabilityStanding(
                capability, outcome, allowance, deciding.at(), deciding.validity(),
                deciding.provenance(), deciding.reason());
    }
}
