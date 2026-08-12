package org.jmouse.access;

import org.jmouse.access.spi.BundledPermission;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.RoleGrant;
import org.jmouse.access.spi.ResolutionCache;
import org.jmouse.access.spi.ShareGrants;

import java.util.List;

/**
 * What a subject may do at a target, worked out once and with its provenance.
 *
 * <pre>
 * effective(subject, target) =
 *       ⋃ { role bundles of every assignment whose scope covers target }
 *     ∪ { ALLOW overrides whose scope covers target }
 *     − { DENY  overrides whose scope covers target }
 * </pre>
 *
 * <p><strong>Two queries per (subject, scope chain), never one per permission.</strong> A menu render
 * asks about a dozen permissions and a paginated listing about a page of rows; both resolve one set,
 * once, and the {@link ResolutionCache} keeps it for the rest of the unit of work. Whoever adds a
 * third query here should first check it is not the same question asked a different way.
 *
 * <p><strong>It answers with provenance rather than with a set of strings</strong>, and that is not
 * an extra: {@code /admin/access} renders exactly this object, and a control room computing its own
 * answer is a control room that lies on the day it matters.
 */
public class EffectivePermissionsResolver {

    private final GrantStore      grants;
    private final ScopeCatalog    scopes;
    private final ResolutionCache cache;
    private final ShareGrants     shareGrants;

    /**
     * @param cache       where to keep an answer for the length of one unit of work.
     *                    {@link ResolutionCache#none()} is valid and merely slower
     * @param shareGrants what a share token grants, or null in an installation that has no shares —
     *                    in which case a share subject resolves to nothing, which is the right answer
     */
    public EffectivePermissionsResolver(
            GrantStore      grants,
            ScopeCatalog    scopes,
            ResolutionCache cache,
            ShareGrants     shareGrants) {

        this.grants      = grants;
        this.scopes      = scopes;
        this.cache       = cache == null ? ResolutionCache.none() : cache;
        this.shareGrants = shareGrants;
    }

    /**
     * The effective set for one subject at one target.
     *
     * <p>An agent's is additionally capped against its master's <em>in the same scope</em>, which is
     * strictly tighter than the unscoped intersection it replaces and never looser: a master who has
     * lost a workspace loses it for its agents in the same request. That is what the hand-written
     * membership half of the cap existed to guarantee (ADR-0006), arrived at by ordinary resolution.
     *
     * <p>Memoised inside this method rather than by its callers. A caller that has to remember to
     * cache is a caller that forgets inside a loop, which is the N+1 spec risk 1 names.
     */
    public EffectivePermissions resolve(Subject subject, AccessTarget target) {
        if (subject.isShare()) {
            return shared(subject, target);
        }
        if (!subject.isAuthenticated()) {
            return EffectivePermissions.none();
        }

        List<ScopeReference> chain = scopes.covering(target, subject.ownedRowsBelongTo());

        return cache.permissions(subject.principalId(), chain, () -> resolveSubject(subject, chain));
    }

    private EffectivePermissions resolveSubject(Subject subject, List<ScopeReference> chain) {
        EffectivePermissions own = resolveFor(subject.principalId(), chain);

        if (!subject.isAgent()) {
            return own;
        }

        // The master is asked about the same chain, so the cap is scoped rather than global.
        return own.cappedBy(resolveFor(subject.masterId(), chain), narrowestOf(chain));
    }

    /**
     * What a share link grants — one permission, over one row, in the place that row lives.
     *
     * <p>An anonymous subject with a derived set rather than a route that skips the question. The
     * grant only holds where the request is actually aimed at the shared row: a token for one form
     * says nothing about a second one, and reading it as a permission over the workspace would turn
     * every share into a membership.
     *
     * <p>Everything before axis 5 still runs. That is the point of bringing shares inside: a
     * workspace that has switched a module off, or whose plan no longer includes it, stops serving
     * its own links — and nobody had to write that down twice.
     */
    private EffectivePermissions shared(Subject subject, AccessTarget target) {
        if (shareGrants == null) {
            return EffectivePermissions.none();
        }

        return shareGrants.of(subject.shareToken())
                .filter(grant -> aimsAtTheSharedRow(grant.target(), target))
                .map(grant -> EffectivePermissions.builder()
                        .granted(grant.permission(), PermissionSource.share(
                                grant.target().narrowestPlace().orElseGet(scopes::everythingReference)))
                        .build())
                .orElseGet(EffectivePermissions::none);
    }

    /**
     * Whether the request is about the row the token names, rather than merely about its workspace.
     *
     * <p>A share is over <em>exactly</em> one resource, so the match is exact: the target asked about
     * must be the target the token resolved to. It compared workspaces once, which is the difference
     * between sharing a record and handing out a key to the building — a link to one entry would have
     * granted {@code entry:read} over every entry beside it.
     *
     * <p>Equality is deliberately unforgiving about the owner, too. {@code SharedResourceGrants}
     * drops it, so a target that still carries one does not match: <strong>a share is not "act as the
     * owner"</strong>, and matching loosely would hand the holder of a link every {@code SELF}-scoped
     * grant that owner has anywhere.
     */
    private boolean aimsAtTheSharedRow(AccessTarget shared, AccessTarget asked) {
        return shared.equals(asked);
    }

    /** The installation-wide set, for the routes and screens that are about the installation. */
    public EffectivePermissions resolveInstallationWide(Subject subject) {
        return resolve(subject, AccessTarget.installation());
    }

    private EffectivePermissions resolveFor(String subjectId, List<ScopeReference> chain) {
        EffectivePermissions.Builder resolved = EffectivePermissions.builder();

        for (RoleGrant role : grants.rolesCovering(subjectId, chain)) {
            // Recorded even where the role bundles nothing: being present in a workspace and having
            // no permission there are two different refusals with two different next moves.
            resolved.reaches(role.at());

            for (BundledPermission entry : role.bundle()) {
                ScopeReference conferredAt = conferredScope(entry, role.at(), chain);

                if (conferredAt == null) {
                    continue;
                }

                // ⚠️ Two narrowings may apply and both were written down: the assignment's ("you hold
                // this role when …") rides in the attribution, the entry's ("the role carries this
                // one only when …") is passed alongside, and PermissionSource.role composes them.
                resolved.granted(entry.permission(), PermissionSource.role(
                        role.roleName(),
                        conferredAt,
                        role.attribution(),
                        entry.condition()));
            }
        }

        for (DirectGrant direct : grants.directCovering(subjectId, chain)) {
            PermissionSource source = PermissionSource.override(
                    direct.allowed(), direct.at(), direct.attribution());

            if (!direct.allowed() && direct.isConditional()) {
                // ⚠️ A conditional denial is not a denial yet, and recording it as one would take the
                // permission away for every row rather than for the ones the rule is about. It is
                // carried instead, and subtracted later by an axis that has a row to ask about.
                resolved.narrowed(direct.permission(), source);
            } else if (direct.allowed()) {
                resolved.granted(direct.permission(), source);
            } else {
                // Deny is recorded as a removal, and the builder applies removals last — which is
                // what "deny wins, across every level" means and why most-specific does not win.
                resolved.removed(direct.permission(), source);
            }
        }

        return resolved.build();
    }

    /**
     * Where one bundle entry lands for this target, or null where it does not land at all.
     *
     * <p>The type is the narrower of how far the role carries the permission and how far the
     * assignment reaches — which is what keeps {@code space:read} in {@code ROLE_USER} from opening
     * every workspace in the installation, and is the whole content of the permission
     * {@code space:administer} used to be.
     *
     * <p><strong>Narrowing to a scope that names an instance confers nothing.</strong> This is the
     * rule the whole cluster turns on, and it was wrong at first in a way that reads as a subtlety
     * and is not one. An installation-wide role bundling {@code space:write @SPACE} does <em>not</em>
     * mean "in whichever workspace is being asked about" — that would grant it in <strong>every</strong>
     * workspace, which is precisely what the scope column exists to prevent and exactly what
     * {@code space:administer} used to be. The assignment did not say <em>which</em> workspace, so
     * there is no workspace it confers in. Membership is what says which, and membership is an
     * assignment at {@code SPACE:{id}} whose bundle entry lands on the first branch below.
     *
     * <p><strong>Narrowing to a scope that names none is different, and lands.</strong>
     * {@code entry:write @SELF} inside {@code ROLE_USER} confers over the subject's own rows
     * wherever they are, because {@code SELF} needs no instance to be unambiguous: there is exactly
     * one answer to "whose", and the subject is it. Without this branch a person could not touch
     * their own rows at all.
     *
     * <p>Null is a real answer rather than a fault, and it is the common one: it means this entry
     * says nothing about this target.
     */
    private ScopeReference conferredScope(
            BundledPermission    entry,
            ScopeReference       assignedAt,
            List<ScopeReference> chain) {

        ScopeKind conferredType = entry.conferredAt(assignedAt.type());

        if (conferredType.equals(assignedAt.type())) {
            return assignedAt;
        }

        if (conferredType.namesAnInstance()) {
            return null;
        }

        return chain.stream()
                .filter(link -> link.type().equals(conferredType))
                .findFirst()
                .orElse(null);
    }

    /**
     * The narrowest link of the chain — where an agent cap is recorded as having applied.
     *
     * <p>Cosmetic, and only ever read by the control room: it is the scope the reader was asking
     * about, so "capped by master @SPACE:kyiv" reads as an answer to the question they asked rather
     * than as a fact about the installation.
     */
    private ScopeReference narrowestOf(List<ScopeReference> chain) {
        return chain.get(chain.size() - 1);
    }
}
