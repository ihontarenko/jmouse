package org.jmouse.access.policy;

import org.jmouse.access.ScopeReference;
import org.jmouse.access.policy.AccessPolicy.BoundAssignment;
import org.jmouse.access.policy.AccessPolicy.BoundSubject;
import org.jmouse.access.spi.BundledPermission;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.RoleGrant;

import java.util.List;
import java.util.stream.Stream;

/**
 * A bound policy, answered as the engine's {@link GrantStore}.
 *
 * <p>The last step, and the shortest: four methods that read a map. Everything expensive already
 * happened at load — names resolved, wildcards expanded, scopes checked — so serving a decision from a
 * policy costs a lookup and a filter, and the engine cannot tell it apart from a database.
 *
 * <p>Which is the point of the whole feature. Nothing above this class knows a file was involved.
 */
public final class PolicyGrantStore implements GrantStore {

    private final AccessPolicy policy;

    public PolicyGrantStore(AccessPolicy policy) {
        this.policy = policy;
    }

    @Override
    public List<RoleGrant> rolesCovering(String subjectId, List<ScopeReference> chain) {
        return rolesHeldBy(subjectId).stream()
                .filter(role -> chain.contains(role.at()))
                .toList();
    }

    @Override
    public List<DirectGrant> directCovering(String subjectId, List<ScopeReference> chain) {
        return directHeldBy(subjectId).stream()
                .filter(grant -> chain.contains(grant.at()))
                .toList();
    }

    @Override
    public List<RoleGrant> rolesHeldBy(String subjectId) {
        return subject(subjectId).roles().stream()
                .map(this::toRoleGrant)
                .toList();
    }

    /**
     * What this account holds directly, and what the installation withholds from everybody.
     *
     * <p>⚠️ <strong>{@code subject *} is read here rather than expanded at load.</strong> Expanding it
     * would mean knowing every account, and a store that could enumerate them is a store the engine
     * could walk — which is the one thing {@link GrantStore} is shaped to prevent. So the wildcard
     * stays a block, and every answer carries it.
     *
     * <p>It is only ever a denial — {@link PolicyBinder} refuses anything else in that block — so
     * concatenating it can subtract and can never add. Which is also why the order of the two lists
     * does not matter: deny wins wherever it was written.
     */
    @Override
    public List<DirectGrant> directHeldBy(String subjectId) {
        List<DirectGrant> withheldFromEverybody = subject(PolicyBinder.EVERYBODY).grants();

        if (withheldFromEverybody.isEmpty() || PolicyBinder.EVERYBODY.equals(subjectId)) {
            return subject(subjectId).grants();
        }

        return Stream.concat(subject(subjectId).grants().stream(), withheldFromEverybody.stream())
                .toList();
    }

    /**
     * One assignment, with the bundle its role carries.
     *
     * <p>The bundle is looked up rather than stored per assignment, so a role held in nine places is
     * one bundle and nine references to it.
     *
     * <p>{@code grantedBy} and {@code since} are the policy's name and its load time, never null, and
     * the {@code origin} names the file and the line. The control room renders provenance verbatim; a
     * grant whose origin is nothing reads as a defect to whoever is trying to explain a permission,
     * and one that points at a table nobody can find is worse.
     */
    private RoleGrant toRoleGrant(BoundAssignment assignment) {
        List<BundledPermission> bundle = policy.roles().getOrDefault(assignment.roleName(), List.of());

        return new RoleGrant(
                assignment.roleName(),
                assignment.at(),
                policy.provenance(),
                policy.loadedAt(),
                bundle,
                assignment.origin());
    }

    private BoundSubject subject(String subjectId) {
        return policy.subjects().getOrDefault(subjectId, BoundSubject.empty());
    }
}
