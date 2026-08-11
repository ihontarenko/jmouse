package org.jmouse.access.jpa;

import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeReference;

import java.util.List;

/**
 * Everything that <strong>changes</strong> authorization — the half the engine implied and never had.
 *
 * <h2>Why this exists</h2>
 *
 * <p>{@code GrantStore} answers <em>"what does this subject hold"</em> and is deliberately read-only,
 * on the grounds that <em>issuing</em> a grant is a product's business: its screens, its rules about
 * who may, its audit trail. That reasoning is sound and it was over-applied. It does not follow that a
 * product must own {@code @Entity} classes on this library's tables — a screen and an audit trail are
 * the product's; a row in {@code access_role_assignments} is not.
 *
 * <p>So the product asks for a change and records that somebody made it; the library performs it. Every
 * write below is deliberately shaped to make that possible.
 *
 * <h2>⚠️ Two things this must never become</h2>
 *
 * <p><strong>A store the engine can walk.</strong> This administers <em>roles and grants</em> and must
 * never enumerate subjects — <em>"a store that could list every account would be a store the engine
 * could walk"</em>. Listing roles is a bounded vocabulary; listing accounts is bounded by how big the
 * customer is. Every method here that reads takes a subject or a role and never asks for all of them.
 *
 * <p><strong>A second policy.</strong> A policy document may own a role's bundle, and the boot
 * cross-checks it. An administration call that could edit a bundle the file owns would let a screen
 * quietly disagree with a reviewed document — so {@link #setBundle} refuses one, and the refusal names
 * the file.
 */
public interface AccessAdministration {

    /** Somebody decided this deliberately — what an assignment is where nothing else says otherwise. */
    String DIRECT = "DIRECT";

    // ── Roles ─────────────────────────────────────────────────────────────────

    /** Every role this installation has, in name order. A bounded vocabulary, so listing it is safe. */
    List<RoleView> roles();

    /**
     * Brings a role into being.
     *
     * @param assignableAt the <em>widest</em> place it may be handed out at
     */
    RoleView defineRole(String roleName, ScopeKind assignableAt);

    /**
     * Replaces what a role carries.
     *
     * @throws IllegalStateException where a policy document owns this role's bundle — editing it here
     *                               would let a screen disagree with a file that was reviewed
     */
    RoleView setBundle(String roleName, List<BundleEntry> bundle);

    /** Retires a role, and every assignment of it. */
    void retireRole(String roleName);

    // ── Who holds what ────────────────────────────────────────────────────────

    /**
     * Hands a subject a role at a place, as something somebody decided directly.
     *
     * @param by who did it, for the record. ⚠️ Kept as a plain identifier: a granter who later leaves
     *           must not blank the grant, because "granted by somebody who has since left" is the
     *           fact an audit asks for
     * @return what changed, so the caller can audit it without owning the row
     */
    default Change assign(String subjectId, String roleName, ScopeReference at, String by) {
        return assign(subjectId, roleName, at, DIRECT, by);
    }

    /**
     * The same, from a mechanism of the product's own.
     *
     * @param source ⚠️ <strong>Load-bearing rather than decorative.</strong> Revoking a membership has
     *               to take the assignment that came with it and leave the one somebody granted by
     *               hand, and without this the two are the same row. Kept as a plain name because a
     *               library legislating about a product's provenance vocabulary is a library that can
     *               be adopted once
     */
    Change assign(String subjectId, String roleName, ScopeReference at, String source, String by);

    /** Takes a role back at one place. Silent where it was not held — un-assigning twice is not an error. */
    Change unassign(String subjectId, String roleName, ScopeReference at);

    /**
     * Takes back <em>every</em> role a subject holds at one place.
     *
     * <p>What leaving somewhere means. A caller that had to name the role would have to read it first,
     * and would then be one race away from revoking a membership while leaving its authority behind.
     *
     * @return how many went, so a caller can tell "they were not here" from "they are not now"
     */
    int unassignAllAt(String subjectId, ScopeReference at);

    /**
     * Writes a personal allow or deny.
     *
     * <p>⚠️ A {@code DENY} is the only way to take away what a role gives without editing the role,
     * and {@code reason} is what makes the refusal answerable a year later.
     */
    Change grant(String subjectId, String permission, ScopeReference at, Effect effect,
                 String reason, String by);

    /** Removes a personal allow or deny — which restores whatever a role was already saying. */
    Change ungrant(String subjectId, String permission, ScopeReference at);

    /**
     * ⚠️ Everything one subject holds, gone.
     *
     * <p>This is the deletion path, and it lives here because the reason it is needed is the library's
     * own: a library table cannot foreign-key into a product's accounts, so nothing cascades any more.
     * A product that forgets to call it leaves orphan grants behind <strong>silently</strong> — which
     * is exactly the sort of gap an adopting product should not have to discover for itself.
     *
     * @return how many rows went, so the caller can record a deletion that actually deleted something
     */
    int revokeAllFor(String subjectId);

    // ── What it answers with ──────────────────────────────────────────────────

    /** A role and what it carries — the shape an administration screen draws. */
    record RoleView(String id, String name, String assignableAt, List<BundleEntry> bundle) {
    }

    /**
     * @param scopeType the scope <em>name</em> and never an instance — see
     *                  {@code AccessRolePermission} for why a bundle cannot name a place
     */
    record BundleEntry(String permission, String scopeType) {
    }

    enum Effect {
        ALLOW, DENY
    }

    /**
     * What one call changed.
     *
     * <p>⚠️ Returned rather than logged, and that is the whole mechanism by which the audit trail stays
     * the product's while the row belongs to the library. {@code changed} is false where the call was a
     * no-op, so a product does not record an event for a button somebody pressed twice.
     */
    record Change(boolean changed, String subjectId, String what, ScopeReference at) {

        public static Change nothing(String subjectId, String what, ScopeReference at) {
            return new Change(false, subjectId, what, at);
        }

        public static Change made(String subjectId, String what, ScopeReference at) {
            return new Change(true, subjectId, what, at);
        }
    }
}
