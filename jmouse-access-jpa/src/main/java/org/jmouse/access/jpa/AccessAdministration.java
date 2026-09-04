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
 * <p><strong>A judge of what a change means.</strong> These methods perform writes; they do not decide
 * whether a write is wise. A product with a rule about its own installation — <em>"somebody has to be
 * left able to hand permissions back out"</em> — wraps this interface rather than asking the library to
 * carry a policy it cannot know it has. ⚠️ It follows that a caller who reaches
 * {@link JpaAccessAdministration} directly is a caller outside every such rule.
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
     * <p>⚠️ <strong>Replaces</strong>, so this is a removal as much as an addition: every account
     * holding the role loses whatever the new bundle leaves out, everywhere they hold it. It is the
     * shortest way to take a permission away from many people at once, which is worth knowing before
     * calling it from anywhere that has not thought about the consequence.
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
    default Change assign(String subjectId, String roleName, ScopeReference at, String source, String by) {
        return assign(subjectId, roleName, at, source, by, null);
    }

    /**
     * The same, applying only under a condition.
     *
     * @param conditionSource the expression, as source, or null for always. ⚠️ It narrows
     *                        <em>everything</em> the role carries at this place, which is a different
     *                        statement from narrowing one entry of its bundle — that one belongs on
     *                        {@link BundleEntry} and reaches everybody holding the role
     */
    default Change assign(String subjectId, String roleName, ScopeReference at, String source,
                          String by, String conditionSource) {

        return assign(subjectId, roleName, at, source, by, conditionSource, null);
    }

    /**
     * The same, with the sentence that says why.
     *
     * <p>⚠️ <strong>{@code reason} is not provenance, and {@code by} is not a substitute for it.</strong>
     * Who did it and why they did it are different facts, and only the second one survives the person
     * leaving. The same argument {@link #grant} has always made about a denial applies here and is asked
     * more often: <em>why does this contractor hold SPACE_ADMIN in Kyiv?</em></p>
     *
     * <p>⚠️ <strong>It is part of what makes an assignment "the same assignment".</strong> Rewriting only
     * the reason is a change, and a caller told nothing changed would go on showing the words they just
     * replaced. That is exactly what happened to grants before {@code reason} joined the comparison
     * there — a row seeded once kept saying <em>Seeded from the policy files</em> however often somebody
     * edited it.</p>
     *
     * @param reason why, in words, or null where there is nothing to say. A role handed out by a
     *               membership mechanism has none, and manufacturing one would put noise on the
     *               majority of rows in the table
     */
    Change assign(String subjectId, String roleName, ScopeReference at, String source, String by,
                  String conditionSource, String reason);

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
    default Change grant(String subjectId, String permission, ScopeReference at, Effect effect,
                         String reason, String by) {

        return grant(subjectId, permission, at, effect, reason, by, null);
    }

    /**
     * The same, applying only under a condition.
     *
     * <p>⚠️ {@code subjectId} may be {@link org.jmouse.access.jpa.entity.AccessSubjectPermission#EVERYBODY},
     * which is how a denial reaches every account without a row per account — and which is only
     * useful <em>with</em> a condition, since an unconditional universal denial is indistinguishable
     * from switching the permission off.
     */
    Change grant(String subjectId, String permission, ScopeReference at, Effect effect,
                 String reason, String by, String conditionSource);

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
    /**
     * @param conditionSource ⚠️ the expression this entry applies under, as <strong>source</strong>, or
     *                        null for always. Source rather than a compiled predicate, because this
     *                        library has no expression language and must not acquire one — and because
     *                        a condition has to survive being written back out, diffed and reviewed.
     *                        Whoever hands one over is the one that can refuse it: a condition naming
     *                        an action nothing publishes compiles perfectly and then never fires
     */
    record BundleEntry(String permission, String scopeType, String conditionSource) {

        /** The ordinary entry: it applies whenever the role is held. */
        public BundleEntry(String permission, String scopeType) {
            this(permission, scopeType, null);
        }
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
