package org.jmouse.access.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.jpa.entity.AccessRole;
import org.jmouse.access.jpa.entity.AccessRoleAssignment;
import org.jmouse.access.jpa.entity.AccessRolePermission;
import org.jmouse.access.jpa.entity.AccessSubjectPermission;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Changing authorization, against the engine's own tables.
 *
 * <h2>⚠️ Nothing here audits anything, and that is the design</h2>
 *
 * <p>Every write returns a {@link Change} instead. The product's screen records that somebody made it,
 * in the product's own audit vocabulary and against the product's own retention — which is exactly the
 * split that was being defended when {@code GrantStore} was made read-only. What was over-applied was
 * the conclusion that the <em>row</em> must therefore be the product's too.
 *
 * <p>{@code changed} is false on a no-op, so a screen does not record an event for a button somebody
 * pressed twice.
 *
 * <h2>⚠️ It cannot enumerate subjects</h2>
 *
 * <p>{@link #roles()} lists a bounded vocabulary. There is deliberately no <em>"everybody who holds
 * this role"</em> and no <em>"every subject"</em>: a store that could list every account would be a
 * store the engine could walk, and whoever opens an administration screen already knows which account
 * they are looking at.
 */
public class JpaAccessAdministration implements AccessAdministration {

    private final EntityManager       entityManager;
    private final DeclaredRoleBundles declaredBundles;
    private final Supplier<String>    identifiers;

    public JpaAccessAdministration(EntityManager entityManager, DeclaredRoleBundles declaredBundles) {
        this(entityManager, declaredBundles, () -> UUID.randomUUID().toString());
    }

    /**
     * @param identifiers how new rows are named. ⚠️ A seam because a product may already have an
     *                    identifier scheme its rows follow, and a library quietly imposing UUIDs on one
     *                    that does not is a schema decision made by accident.
     */
    public JpaAccessAdministration(EntityManager entityManager, DeclaredRoleBundles declaredBundles,
                                   Supplier<String> identifiers) {

        this.entityManager   = entityManager;
        this.declaredBundles = declaredBundles;
        this.identifiers     = identifiers;
    }

    // ── Roles ─────────────────────────────────────────────────────────────────

    @Override
    public List<RoleView> roles() {
        return entityManager.createQuery(
                        "SELECT role FROM AccessRole role ORDER BY role.roleName ASC", AccessRole.class)
                .getResultList().stream()
                .map(JpaAccessAdministration::describe)
                .toList();
    }

    @Override
    public RoleView defineRole(String roleName, ScopeKind assignableAt) {
        findRole(roleName).ifPresent(existing -> {
            throw new IllegalStateException(
                    "There is already a role called '" + roleName + "'. A role name is how every "
                    + "assignment refers to it, so two would be two different answers to one question.");
        });

        AccessRole role = new AccessRole(identifiers.get(), roleName, assignableAt.name());
        entityManager.persist(role);

        return describe(role);
    }

    /**
     * ⚠️ Refuses a role whose bundle a policy document owns.
     *
     * <p>A screen that offered to edit what a reviewed file states would let the two disagree — and the
     * document wins at load, so the edit would appear to work and then quietly not. Refusing names the
     * only honest next move: edit the document.
     */
    @Override
    public RoleView setBundle(String roleName, List<BundleEntry> bundle) {
        AccessRole role = requireRole(roleName);

        if (!declaredBundles.bundleOf(roleName).isEmpty()) {
            throw new IllegalStateException(
                    "'" + roleName + "' has its bundle declared in a policy document, so it cannot be "
                    + "edited here. What a file states is what loads, and a screen that appeared to "
                    + "change it would be showing an edit that never took effect. Edit the document.");
        }

        Set<AccessRolePermission> entries = new LinkedHashSet<>();

        for (BundleEntry entry : bundle) {
            entries.add(new AccessRolePermission(role.getId(), entry.permission(), entry.scopeType()));
        }

        role.replaceBundle(entries);

        return describe(role);
    }

    @Override
    public void retireRole(String roleName) {
        AccessRole role = requireRole(roleName);

        // The assignments go first and explicitly. A cascade would be tidier and would hide the one
        // fact worth knowing about retiring a role: it takes something away from everybody holding it.
        entityManager.createQuery(
                        "DELETE FROM AccessRoleAssignment assignment WHERE assignment.roleId = :roleId")
                .setParameter("roleId", role.getId())
                .executeUpdate();

        entityManager.remove(role);
    }

    // ── Who holds what ────────────────────────────────────────────────────────

    @Override
    public Change assign(String subjectId, String roleName, ScopeReference at, String source, String by) {
        AccessRole role = requireRole(roleName);

        if (assignment(subjectId, role.getId(), at).isPresent()) {
            return Change.nothing(subjectId, roleName, at);
        }

        entityManager.persist(new AccessRoleAssignment(
                identifiers.get(), subjectId, role.getId(), at.type().name(), at.id(), source, by));

        return Change.made(subjectId, roleName, at);
    }

    @Override
    public Change unassign(String subjectId, String roleName, ScopeReference at) {
        AccessRole role = requireRole(roleName);

        return assignment(subjectId, role.getId(), at)
                .map(assignment -> {
                    entityManager.remove(assignment);
                    return Change.made(subjectId, roleName, at);
                })
                .orElseGet(() -> Change.nothing(subjectId, roleName, at));
    }

    @Override
    public int unassignAllAt(String subjectId, ScopeReference at) {
        return entityManager.createQuery("""
                        DELETE FROM AccessRoleAssignment assignment
                         WHERE assignment.subjectId = :subjectId
                           AND assignment.scopeType = :kind
                           AND assignment.scopeId   = :instance
                        """)
                .setParameter("subjectId", subjectId)
                .setParameter("kind", at.type().name())
                .setParameter("instance", at.id())
                .executeUpdate();
    }

    @Override
    public Change grant(String subjectId, String permission, ScopeReference at, Effect effect,
                        String reason, String by) {

        Optional<AccessSubjectPermission> existing = override(subjectId, permission, at);

        if (existing.isPresent() && existing.get().getEffect().equalsIgnoreCase(effect.name())) {
            return Change.nothing(subjectId, permission, at);
        }

        // ⚠️ Replaced rather than updated, so that flipping an ALLOW to a DENY reads as one decision
        // rather than as an edit to somebody else's. The unique key would refuse the second row anyway.
        existing.ifPresent(entityManager::remove);
        entityManager.flush();

        entityManager.persist(new AccessSubjectPermission(
                identifiers.get(), subjectId, permission, effect.name(),
                at.type().name(), at.id(), reason, by));

        return Change.made(subjectId, permission, at);
    }

    @Override
    public Change ungrant(String subjectId, String permission, ScopeReference at) {
        return override(subjectId, permission, at)
                .map(row -> {
                    entityManager.remove(row);
                    return Change.made(subjectId, permission, at);
                })
                .orElseGet(() -> Change.nothing(subjectId, permission, at));
    }

    @Override
    public int revokeAllFor(String subjectId) {
        int assignments = entityManager.createQuery(
                        "DELETE FROM AccessRoleAssignment assignment WHERE assignment.subjectId = :subjectId")
                .setParameter("subjectId", subjectId)
                .executeUpdate();

        int overrides = entityManager.createQuery(
                        "DELETE FROM AccessSubjectPermission override WHERE override.subjectId = :subjectId")
                .setParameter("subjectId", subjectId)
                .executeUpdate();

        return assignments + overrides;
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private Optional<AccessRoleAssignment> assignment(String subjectId, String roleId, ScopeReference at) {
        return entityManager.createQuery("""
                        SELECT assignment FROM AccessRoleAssignment assignment
                         WHERE assignment.subjectId = :subjectId
                           AND assignment.roleId    = :roleId
                           AND assignment.scopeType = :kind
                           AND assignment.scopeId   = :instance
                        """, AccessRoleAssignment.class)
                .setParameter("subjectId", subjectId)
                .setParameter("roleId", roleId)
                .setParameter("kind", at.type().name())
                .setParameter("instance", at.id())
                .getResultStream()
                .findFirst();
    }

    private Optional<AccessSubjectPermission> override(String subjectId, String permission, ScopeReference at) {
        return entityManager.createQuery("""
                        SELECT override FROM AccessSubjectPermission override
                         WHERE override.subjectId  = :subjectId
                           AND override.permission = :permission
                           AND override.scopeType  = :kind
                           AND override.scopeId    = :instance
                        """, AccessSubjectPermission.class)
                .setParameter("subjectId", subjectId)
                .setParameter("permission", permission)
                .setParameter("kind", at.type().name())
                .setParameter("instance", at.id())
                .getResultStream()
                .findFirst();
    }

    private Optional<AccessRole> findRole(String roleName) {
        return entityManager.createQuery(
                        "SELECT role FROM AccessRole role WHERE role.roleName = :name", AccessRole.class)
                .setParameter("name", roleName)
                .getResultStream()
                .findFirst();
    }

    private AccessRole requireRole(String roleName) {
        return findRole(roleName).orElseThrow(() -> new IllegalStateException(
                "There is no role called '" + roleName + "'."));
    }

    private static RoleView describe(AccessRole role) {
        return new RoleView(
                role.getId(),
                role.getRoleName(),
                role.getAssignableAt(),
                role.getBundle().stream()
                        .map(entry -> new BundleEntry(entry.getPermission(), entry.getScopeType()))
                        .toList());
    }
}
