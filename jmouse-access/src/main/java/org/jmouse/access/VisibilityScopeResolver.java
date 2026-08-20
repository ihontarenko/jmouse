package org.jmouse.access;

import org.jmouse.access.spi.BundledPermission;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.RoleGrant;
import org.jmouse.access.spi.ResolutionCache;
import org.jmouse.access.spi.ScopeHierarchy;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Everywhere a subject holds one permission — the question a <em>listing</em> asks.
 *
 * <p>Deliberately not {@link EffectivePermissionsResolver}, and the difference is not a detail. That
 * one asks <em>"may this subject do this <strong>here</strong>"</em> and matches grants against the
 * chain covering one target. A listing asks the opposite: <em>"where does this subject hold this at
 * all"</em>, and the answer is a set of places rather than a yes. Folding the two into one bean would
 * have meant one of the two questions being answered by filtering the other's answer, which is how a
 * listing predicate comes to disagree with the check on the row it just listed.
 *
 * <p><strong>Deny wins, and it wins everywhere.</strong> The same sentence as everywhere else in this
 * subsystem, and most-specific still does not win: one personal deny takes the permission away
 * entirely, so the listing shows nothing rather than showing the parts the deny did not name. That is
 * the property {@code space:module:restrict} was invented to have, applied to reading.
 *
 * <p>⚠️ <strong>An <em>unconditional</em> deny, that is.</strong> A conditional one is a rule about a
 * row and this question has no row, so it is carried past rather than applied — exactly as
 * {@link EffectivePermissionsResolver} carries it and lets a later axis subtract it. The two have to
 * treat it the same way: reading it as unconditional here emptied every listing filtered by a
 * permission that any single row still passed the check for.
 */
public class VisibilityScopeResolver {

    private final GrantStore      grants;
    private final ResolutionCache cache;
    private final ScopeHierarchy  scopeHierarchy;

    /**
     * @param cache where to keep an answer for the length of one unit of work.
     *              {@link ResolutionCache#none()} is valid and merely slower
     */
    public VisibilityScopeResolver(GrantStore grants, ResolutionCache cache, ScopeHierarchy scopeHierarchy) {
        this.grants         = grants;
        this.cache          = cache == null ? ResolutionCache.none() : cache;
        this.scopeHierarchy = scopeHierarchy;
    }

    /**
     * Which rows this subject may see under this permission.
     *
     * <p>An agent's answer is capped against its master's, for the reason every other answer about an
     * agent is: it owns nothing and may reach nothing its master cannot. Applied per read rather than
     * frozen, so a master who leaves a workspace takes its agents out of that workspace's listings in
     * the same request.
     */
    public VisibilityScope of(Subject subject, String permission) {
        if (!subject.isAuthenticated()) {
            return VisibilityScope.nothing();
        }

        // Memoised here rather than by the caller. A caller that has to remember to cache is a
        // caller that forgets inside a loop, which is the N+1 a listing invites.
        return cache.visibility(
                subject.principalId(), permission, () -> resolveSubject(subject, permission));
    }

    private VisibilityScope resolveSubject(Subject subject, String permission) {
        String          ownedRowsBelongTo = subject.ownedRowsBelongTo();
        VisibilityScope own               = resolveFor(subject.principalId(), permission, ownedRowsBelongTo);

        if (!subject.isAgent()) {
            return own;
        }

        return narrower(own, resolveFor(subject.masterId(), permission, ownedRowsBelongTo));
    }

    private VisibilityScope resolveFor(String subjectId, String permission, String ownedRowsBelongTo) {
        Set<ScopeReference> places     = new LinkedHashSet<>();
        boolean             everywhere = false;
        boolean             held       = false;

        for (DirectGrant direct : grants.directHeldBy(subjectId)) {
            if (!direct.permission().equals(permission)) {
                continue;
            }

            if (!direct.allowed()) {
                // ⚠️ A CONDITIONAL DENIAL IS NOT A DENIAL YET — the same rule
                // EffectivePermissionsResolver keeps, and it has to be the same rule or the two
                // disagree. Its condition is about a row ("… when the entry's purpose is not ASSET"),
                // and there is no row here: a listing asks where the permission is held at all. Read
                // as unconditional it took the permission away everywhere, so one narrow rule about
                // one action emptied every listing filtered by that permission — while the check on
                // any single row still passed, which is the pair disagreeing in the worst direction.
                //
                // Skipped rather than applied, so the listing is the wider of the two and the check
                // that has a row to ask about does the subtracting. An unconditional denial still
                // wins outright, below.
                if (direct.isConditional()) {
                    continue;
                }

                return VisibilityScope.nothing();
            }

            held       = true;
            everywhere |= collect(direct.at().type(), direct.at().id(), places);
        }

        for (RoleGrant role : grants.rolesHeldBy(subjectId)) {
            for (BundledPermission entry : role.bundle()) {
                if (!entry.permission().equals(permission)) {
                    continue;
                }

                held       = true;
                everywhere |= collectConferred(entry, role, places);
            }
        }

        if (!held) {
            return VisibilityScope.nothing();
        }
        if (everywhere) {
            return VisibilityScope.everything(ownedRowsBelongTo);
        }

        // Every narrower place these wider ones contain, resolved here rather than by each consumer.
        // A check can ask upward from a row and compare; a filter has no row yet, so a grant at a
        // wider place has to become the narrower ones it covers before any query can use it.
        places.addAll(scopeHierarchy.within(places));

        return VisibilityScope.places(ownedRowsBelongTo, places);
    }

    /**
     * Where one bundle entry lands when nothing is being asked about in particular.
     *
     * <p>The same rule as the row check, and it has to be the same rule or a listing disagrees with
     * the check on a row it listed. The conferred type is the narrower of the entry's and the
     * assignment's; where that narrowing lands on a scope that <strong>names an instance</strong>,
     * the assignment did not say which one, so it confers <em>nothing</em>.
     *
     * <p>Reading it the other way — as "every instance" — is the mistake that makes a listing show
     * the whole installation to anybody holding a workspace permission in an installation-wide role.
     * Which workspaces a person can see is said by their assignments at those workspaces, and by
     * nothing else.
     */
    private boolean collectConferred(
            BundledPermission   entry,
            RoleGrant           role,
            Set<ScopeReference> places) {

        ScopeReference assignedAt = role.at();
        ScopeKind      conferred  = entry.conferredAt(assignedAt.type());

        if (conferred.equals(assignedAt.type())) {
            return collect(conferred, assignedAt.id(), places);
        }

        // Narrowed to a place the assignment did not name: it confers nowhere.
        return !conferred.namesAnInstance() && collect(conferred, null, places);
    }

    /**
     * Adds one grant to the answer, and says whether it reaches everything.
     *
     * <p>It names no scope, and now it does not know any either. The widest scope reaches everything
     * and own rows reach no place at all — both are properties of the model, so both are read off
     * {@link ScopeNature} rather than off a constant — and every other kind is a place, whatever this
     * product happens to call it. A place always arrives with the instance it names; the one caller
     * that could have passed none has already refused to.
     */
    private boolean collect(ScopeKind scope, String instance, Set<ScopeReference> places) {
        if (scope.nature() == ScopeNature.EVERYTHING) {
            return true;
        }
        if (!scope.namesAnInstance()) {
            return false;
        }

        places.add(ScopeReference.of(scope, instance));
        return false;
    }

    /** The agent cap: the places both reach, and nothing either of them does not. */
    private VisibilityScope narrower(VisibilityScope agent, VisibilityScope master) {
        if (agent.seesNothing() || master.seesNothing()) {
            return VisibilityScope.nothing();
        }
        if (master.seesEverything()) {
            return agent;
        }
        if (agent.seesEverything()) {
            return master;
        }

        Set<ScopeReference> shared = new LinkedHashSet<>(agent.places());
        shared.retainAll(master.places());

        return VisibilityScope.places(agent.ownerId(), shared);
    }
}
