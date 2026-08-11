package org.jmouse.access.jpa;

import org.jmouse.access.Allowance;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.Validity;

import java.util.List;
import java.util.Optional;

/**
 * Everything that <strong>changes</strong> the entitlement axis — the counterpart of
 * {@link AccessAdministration} for what a place may have rather than for what a person may do.
 *
 * <h2>Why this exists</h2>
 *
 * <p>{@code EntitlementStore} answers <em>"what does this place hold"</em> and is read-only for the
 * same reason {@code GrantStore} is: issuing a grant is a governed act with a product's screens and a
 * product's audit trail behind it. What that reasoning never justified was the product owning
 * {@code @Entity} classes on {@code access_capability_grants} — and while it did, a library release
 * changing a column was caught by somebody's failed startup rather than by a compiler.
 *
 * <h2>⚠️ Two things, and they are not the same thing</h2>
 *
 * <p>A <strong>grant</strong> says what a place <em>may</em> have; a <strong>switch</strong> says what
 * it <em>wants</em>. The first is issued by whoever decides entitlements and carries a provenance, an
 * allowance and a validity window; the second is flipped from an ordinary settings screen and carries
 * none of those. They share this port because they share a schema and an owner — never because they
 * are one idea.
 *
 * <h2>⚠️ Nothing here filters expired rows, and nothing here should</h2>
 *
 * <p>{@link #grantsAt} returns a grant whose window closed last March. <em>"Your trial ended on the
 * 12th"</em> and <em>"you never had this"</em> are different answers with different next moves, and a
 * predicate in the query collapses them into the same silence.
 */
public interface EntitlementAdministration {

    // ── Grants ────────────────────────────────────────────────────────────────

    /**
     * Issues a grant, or restates the one already standing.
     *
     * <p>⚠️ <strong>Idempotent per (place, capability, kind, source), and deliberately not narrower.</strong>
     * Two grants of <em>different</em> sources over one capability are two true facts rather than a
     * contradiction — a purchase on top of a plan is exactly that — so merging them would make a
     * hand-made arrangement disappear the day billing starts issuing its own.
     *
     * @return the grant as it now stands, with {@code issued} false where nothing about it moved
     */
    Issued issue(GrantTerms terms);

    /**
     * Takes one grant back by identifier.
     *
     * @return what went, or empty where there was nothing to take. ⚠️ The grant is answered rather
     *         than merely confirmed, because whoever records the withdrawal has to name what was
     *         withdrawn — and a removed row cannot be read afterwards
     */
    Optional<GrantView> withdraw(String grantId);

    /**
     * Withdraws every grant a place holds from one source — how a tier change lets go of what the
     * previous tier issued without touching a gift, a purchase or a trial.
     *
     * @return the grants that went, so the caller can record each one it actually removed
     */
    List<GrantView> withdrawAllFrom(ScopeReference at, String source);

    /** Every grant a place holds, standing or not, in no particular order. */
    List<GrantView> grantsAt(ScopeReference at);

    /**
     * Which bundle issued each of these places' grants — <strong>one query for a whole listing</strong>.
     *
     * <p>⚠️ This exists because there is no column holding <em>"what tier is this account on"</em>, and
     * there should not be: it would be a second copy of what these rows already say, written by the
     * same method that writes them. What removing it cost was turning that question from a join into a
     * lookup, and a listing of every place in an installation cannot afford one of those per row.
     *
     * <p>A place with no grant from this source simply has no entry, which is the honest way to say an
     * account is on no tier.
     */
    List<SourceReference> sourceReferencesAt(String scopeType, List<String> scopeIds, String source);

    // ── Switches ──────────────────────────────────────────────────────────────

    /** Every stored switch at a place. ⚠️ Absence means <em>on</em> — this table names the exceptions. */
    List<SwitchView> switchesAt(ScopeReference at);

    /**
     * Sets a switch, and says whether anything moved.
     *
     * <p>⚠️ Writes a row even to say "on", because {@code forced} is a decision somebody made and an
     * absent row cannot carry it. What must never happen is the reverse — a table listing everything
     * that is <em>on</em> would answer wrongly for every capability introduced after its rows were
     * written.
     */
    boolean setSwitch(ScopeReference at, String capability, boolean enabled, boolean forced);

    /** Removes a stored switch, returning the capability to the default. */
    boolean clearSwitch(ScopeReference at, String capability);

    // ── What it takes and what it answers with ────────────────────────────────

    /**
     * Everything one grant needs to be written.
     *
     * @param allowed         true to give, false to withhold — and withholding wins, at every level
     * @param source          the <em>product's</em> provenance vocabulary, kept as a plain name: a
     *                        library legislating about where a product's grants come from would be a
     *                        library nobody can adopt twice
     * @param sourceReference which bundle issued this, where one did — null for anything given by hand
     * @param allowance       ⚠️ null means <strong>no ceiling</strong> on a metered capability and
     *                        <strong>not metered</strong> on any other, and only the period tells them
     *                        apart. Writing "unlimited" as a very large number is a lie every screen
     *                        then has to render
     */
    record GrantTerms(
            ScopeReference at,
            String         capability,
            boolean        allowed,
            String         source,
            String         sourceReference,
            Validity       validity,
            Allowance      allowance,
            String         reason,
            String         grantedBy
    ) {
    }

    /** One grant, as whoever administers grants reads it. */
    record GrantView(
            String         id,
            ScopeReference at,
            String         capability,
            boolean        allowed,
            String         source,
            String         sourceReference,
            Validity       validity,
            Allowance      allowance,
            String         reason,
            String         grantedBy
    ) {
    }

    /**
     * A grant and whether writing it changed anything.
     *
     * <p>Same mechanism as {@link AccessAdministration.Change}: returned rather than logged, so the
     * audit trail stays the product's while the row belongs to the library, and false on a no-op so
     * nothing records an event for a button pressed twice.
     */
    record Issued(GrantView grant, boolean issued) {
    }

    /** One place and the bundle its grants name. */
    record SourceReference(String scopeId, String sourceReference) {
    }

    /** One stored switch. */
    record SwitchView(String capability, boolean enabled, boolean forced) {
    }
}
