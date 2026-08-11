package org.jmouse.access.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.jpa.entity.AccessRole;
import org.jmouse.access.jpa.entity.AccessRoleAssignment;
import org.jmouse.access.jpa.entity.AccessSubjectPermission;
import org.jmouse.access.spi.BundledPermission;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.RoleGrant;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The engine's grants, out of the engine's own tables.
 *
 * <h2>⚠️ This class used to live in a product, and that was the defect</h2>
 *
 * <p>Stage 2 moved the schema into this library and left the store — and the {@code @Entity} classes it
 * reads — on the other side of the seam. What that produced was a product mapping classes onto a schema
 * it does not own, kept honest by {@code ddl-auto: validate}, which is a hope rather than a contract.
 * Whoever owns the table owns the mapping, and the query.
 *
 * <h2>Plain JPA, deliberately</h2>
 *
 * <p>{@link EntityManager} and JPQL rather than Spring Data: this module depends on
 * {@code jakarta.persistence-api} and nothing else, so adopting the engine's storage does not also mean
 * adopting somebody's repository framework. The Spring wiring is one bean, in the starter.
 *
 * <h2>⚠️ The queries are approximate and are narrowed back here</h2>
 *
 * <p>A covering query matches scope kinds and identifiers as two independent lists, so a chain of four
 * costs one round trip — and the widened result that produces is filtered against the chain before it
 * leaves. An engine that had to know a store's queries were approximate would be an engine that knew
 * the store.
 */
public class JpaGrantStore implements GrantStore {

    private final EntityManager       entityManager;
    private final ScopeCatalog        scopes;
    private final DeclaredRoleBundles declaredBundles;

    public JpaGrantStore(EntityManager entityManager, ScopeCatalog scopes,
                         DeclaredRoleBundles declaredBundles) {

        this.entityManager   = entityManager;
        this.scopes          = scopes;
        this.declaredBundles = declaredBundles;
    }

    @Override
    public List<RoleGrant> rolesCovering(String subjectId, List<ScopeReference> chain) {
        if (chain.isEmpty()) {
            return List.of();
        }

        List<AccessRoleAssignment> assignments = entityManager.createQuery("""
                        SELECT assignment FROM AccessRoleAssignment assignment
                         WHERE assignment.subjectId = :subjectId
                           AND assignment.scopeType IN :kinds
                           AND assignment.scopeId   IN :instances
                        """, AccessRoleAssignment.class)
                .setParameter("subjectId", subjectId)
                .setParameter("kinds", kindsOf(chain))
                .setParameter("instances", instancesOf(chain))
                .getResultList();

        return assignments.stream()
                .filter(assignment -> chain.contains(placeOf(assignment.getScopeType(), assignment.getScopeId())))
                .map(this::toRoleGrant)
                .toList();
    }

    @Override
    public List<DirectGrant> directCovering(String subjectId, List<ScopeReference> chain) {
        if (chain.isEmpty()) {
            return List.of();
        }

        List<AccessSubjectPermission> overrides = entityManager.createQuery("""
                        SELECT override FROM AccessSubjectPermission override
                         WHERE override.subjectId = :subjectId
                           AND override.scopeType IN :kinds
                           AND override.scopeId   IN :instances
                        """, AccessSubjectPermission.class)
                .setParameter("subjectId", subjectId)
                .setParameter("kinds", kindsOf(chain))
                .setParameter("instances", instancesOf(chain))
                .getResultList();

        return overrides.stream()
                .filter(override -> chain.contains(placeOf(override.getScopeType(), override.getScopeId())))
                .map(this::toDirectGrant)
                .toList();
    }

    @Override
    public List<RoleGrant> rolesHeldBy(String subjectId) {
        return entityManager.createQuery("""
                        SELECT assignment FROM AccessRoleAssignment assignment
                         WHERE assignment.subjectId = :subjectId
                        """, AccessRoleAssignment.class)
                .setParameter("subjectId", subjectId)
                .getResultList().stream()
                .map(this::toRoleGrant)
                .toList();
    }

    @Override
    public List<DirectGrant> directHeldBy(String subjectId) {
        return entityManager.createQuery("""
                        SELECT override FROM AccessSubjectPermission override
                         WHERE override.subjectId = :subjectId
                        """, AccessSubjectPermission.class)
                .setParameter("subjectId", subjectId)
                .getResultList().stream()
                .map(this::toDirectGrant)
                .toList();
    }

    /**
     * One assignment, with everything its role bundles — from the table <strong>and</strong> from a
     * policy document.
     *
     * <p>⚠️ The union is what makes a declared bundle reach a stored assignment. The <em>assignment</em>
     * is still a row, so nothing about where it may be revoked from changes; what a file owns here is
     * the bundle.
     */
    private RoleGrant toRoleGrant(AccessRoleAssignment assignment) {
        AccessRole role = entityManager.find(AccessRole.class, assignment.getRoleId());

        List<BundledPermission> bundle = new ArrayList<>();

        if (role != null) {
            role.getBundle().stream()
                    .map(entry -> new BundledPermission(entry.getPermission(), kind(entry.getScopeType())))
                    .forEach(bundle::add);
        }

        String roleName = role == null ? assignment.getRoleId() : role.getRoleName();

        bundle.addAll(declaredBundles.bundleOf(roleName));

        return new RoleGrant(
                roleName,
                placeOf(assignment.getScopeType(), assignment.getScopeId()),
                assignment.getGrantedBy(),
                assignment.getCreatedAt() == null
                        ? null
                        : assignment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                bundle,
                null);
    }

    private DirectGrant toDirectGrant(AccessSubjectPermission override) {
        return new DirectGrant(
                override.getPermission(),
                override.allows(),
                placeOf(override.getScopeType(), override.getScopeId()),
                override.getGrantedBy(),
                override.getReason(),
                override.getCreatedAt() == null
                        ? null
                        : override.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                null,
                // ⚠️ A stored grant carries no condition, and there is no column for one. A predicate in
                // a row is a predicate nobody can find — conditions exist only where they can be READ,
                // which is the document.
                null);
    }

    private ScopeReference placeOf(String kind, String instance) {
        return ScopeReference.of(kind(kind), instance);
    }

    /**
     * ⚠️ An unregistered scope name is a programming error rather than a case to handle: the row was
     * written by this installation, against a vocabulary it registers, and the boot already refuses a
     * policy naming a scope the build does not have.
     */
    private org.jmouse.access.ScopeKind kind(String name) {
        return scopes.byName(name).orElseThrow(() -> new IllegalStateException(
                "A stored grant names the scope '" + name + "', which this installation does not "
                + "register. Known scopes: "
                + scopes.all().stream().map(org.jmouse.access.ScopeKind::name).collect(Collectors.joining(", "))
                + "."));
    }

    private static List<String> kindsOf(List<ScopeReference> chain) {
        return chain.stream().map(place -> place.type().name()).distinct().toList();
    }

    private static List<String> instancesOf(List<ScopeReference> chain) {
        return chain.stream().map(ScopeReference::id).distinct().toList();
    }
}
