package org.jmouse.access.spi;

import org.jmouse.access.Allowance;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.Validity;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * One capability given to — or taken away from — a subject, at a scope, possibly with a number on it
 * and possibly only for a while.
 *
 * <p>The entitlement axis's answer to what {@link DirectGrant} is for the permission axis, and
 * deliberately the same shape wherever it can be: a denial is a grant with {@link #allowed} false
 * rather than an absence, deny wins, and the subtraction runs last. One sentence for both axes, so an
 * installation learns one rule.
 *
 * <p>What it adds over a permission grant is the three things a capability has and a permission does
 * not: a {@link #allowance} — how much — a {@link #validity} — until when — and a
 * {@link #provenance} — what issued it.
 *
 * <h2>⚠️ The library does not know what is being counted</h2>
 *
 * <p>{@link #capability} is a string the product's catalogue defines. Nothing here knows the words
 * <em>seat</em>, <em>workspace</em>, <em>board</em> or <em>story point</em>, and nothing may learn
 * them: a second product with entirely different layers has to be able to adopt this by declaring its
 * own vocabulary and implementing one interface — the same bargain {@code ScopeKind} already makes
 * for scopes.
 *
 * <p>For the same reason there is no plan, tier or price anywhere in this type. A bundle is a
 * <em>name</em> that issued a set of these, recorded in {@link #provenance}, and the engine never
 * needs to know that the name meant money.
 *
 * @param capability what the grant is about, in the product's own vocabulary
 * @param allowed    true to give, false to take away — and taking away wins, at every level
 * @param at         where it applies
 * @param allowance  how much, or null where the capability carries no number at all
 * @param validity   from and until, never null — use {@link Validity#forever()}
 * @param provenance what issued it, never null
 * @param grantedBy  who recorded it, where that was recorded
 * @param reason     why, where that was recorded — the words a refusal repeats back to the reader
 * @param since      when, where that was recorded
 * @param origin     whether a row or a line holds this — what decides which editor a screen may offer
 */
public record CapabilityGrant(
        String              capability,
        boolean             allowed,
        ScopeReference      at,
        Allowance           allowance,
        Validity            validity,
        CapabilityProvenance provenance,
        String              grantedBy,
        String              reason,
        LocalDateTime       since,
        GrantOrigin         origin
) {

    public CapabilityGrant {
        validity   = validity == null ? Validity.forever() : validity;
        provenance = provenance == null ? CapabilityProvenance.unattributed() : provenance;
        origin     = origin == null ? GrantOrigin.stored() : origin;
    }

    /**
     * Whether this grant applies at a moment.
     *
     * <p>⚠️ <strong>Ask this; do not filter expired rows away before resolution.</strong> A grant
     * whose window has closed is still a fact worth reading — it is how a reader is told a trial ended
     * on the 12th rather than shown a capability that silently stopped existing. Resolution drops it;
     * the control room prints it.
     */
    public boolean appliesAt(Instant moment) {
        return validity.holdsAt(moment);
    }

    /** Whether this grant carries a ceiling, or is purely about whether the capability is open. */
    public boolean isMetered() {
        return allowance != null;
    }

    /** A plain allow a table holds, with no number and no end. */
    public static CapabilityGrant allow(String capability, ScopeReference at, CapabilityProvenance provenance) {
        return new CapabilityGrant(capability, true, at, null, Validity.forever(), provenance,
                                   null, null, null, GrantOrigin.stored());
    }

    /**
     * A refusal somebody recorded, with the words that explain it.
     *
     * <p>{@code reason} is not decoration on this one. A capability withheld by hand and one that was
     * never in the bundle are the same absence to the engine and completely different sentences to
     * whoever is reading the refusal, and only one of them is something they can act on.
     */
    public static CapabilityGrant deny(String capability, ScopeReference at,
                                       CapabilityProvenance provenance, String reason) {
        return new CapabilityGrant(capability, false, at, null, Validity.forever(), provenance,
                                   null, reason, null, GrantOrigin.stored());
    }
}
