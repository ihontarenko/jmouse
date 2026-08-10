package org.jmouse.access.policy;

import org.jmouse.access.ScopeReference;
import org.jmouse.access.policy.AccessPolicy.BoundAssignment;
import org.jmouse.access.policy.AccessPolicy.BoundSubject;
import org.jmouse.access.spi.BundledPermission;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.RoleGrant;

import java.util.List;

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

    @Override
    public List<DirectGrant> directHeldBy(String subjectId) {
        return subject(subjectId).grants();
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
