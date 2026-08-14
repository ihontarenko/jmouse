package org.jmouse.access.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.jpa.entity.AccessRole;
import org.jmouse.access.jpa.entity.AccessRoleAssignment;
import org.jmouse.access.jpa.entity.AccessSubjectPermission;
import org.jmouse.access.spi.BundledPermission;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Who holds what, read straight off the engine's tables.
 *
 * <h2>⚠️ Two queries and a map, never one query per row</h2>
 *
 * <p>Roles are loaded once into a name-and-bundle map and the assignments are joined against it in
 * memory. The obvious shape — {@code entityManager.find(AccessRole.class, …)} per assignment — is an
 * N+1 over the largest table in the schema, on the one screen somebody opens when they are already
 * having a bad day.
 */
public class JpaAccessDisclosure implements AccessDisclosure {

    private final EntityManager    entityManager;
    private final ScopeCatalog     scopes;
    private final StoredConditions conditions;

    public JpaAccessDisclosure(EntityManager entityManager, ScopeCatalog scopes) {
        this(entityManager, scopes, StoredConditions.none());
    }

    public JpaAccessDisclosure(EntityManager entityManager, ScopeCatalog scopes,
                               StoredConditions conditions) {

        this.entityManager = entityManager;
        this.scopes        = scopes;
        this.conditions    = conditions;
    }

    @Override
    public List<RoleHolding> roleHoldings() {
        Map<String, Described> roles = describedRoles();

        return entityManager.createQuery(
                        "SELECT assignment FROM AccessRoleAssignment assignment", AccessRoleAssignment.class)
                .getResultList().stream()
                .map(assignment -> holding(assignment, roles.get(assignment.getRoleId())))
                .toList();
    }

    @Override
    public List<DirectHolding> directHoldings() {
        return entityManager.createQuery(
                        "SELECT override FROM AccessSubjectPermission override", AccessSubjectPermission.class)
                .getResultList().stream()
                .map(this::holding)
                .toList();
    }

    @Override
    public List<DirectHolding> directHoldingsOf(String permission) {
        return entityManager.createQuery("""
                        SELECT override FROM AccessSubjectPermission override
                         WHERE override.permission = :permission
                        """, AccessSubjectPermission.class)
                .setParameter("permission", permission)
                .getResultList().stream()
                .map(this::holding)
                .toList();
    }

    @Override
    public long holdersOf(String roleName, ScopeReference at) {
        return entityManager.createQuery("""
                        SELECT COUNT(assignment)
                          FROM AccessRoleAssignment assignment, AccessRole role
                         WHERE assignment.roleId    = role.id
                           AND role.roleName        = :roleName
                           AND assignment.scopeType = :kind
                           AND assignment.scopeId   = :instance
                        """, Long.class)
                .setParameter("roleName", roleName)
                .setParameter("kind", at.type().name())
                .setParameter("instance", at.id())
                .getSingleResult();
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    /** Every role by identifier, with its stored bundle already unioned with the declared one. */
    private Map<String, Described> describedRoles() {
        Map<String, Described> byId = new HashMap<>();

        for (AccessRole role : entityManager
                .createQuery("SELECT role FROM AccessRole role", AccessRole.class)
                .getResultList()) {

            List<BundledPermission> bundle = new ArrayList<>();

            role.getBundle().stream()
                    .map(entry -> new BundledPermission(
                            entry.getPermission(), kind(entry.getScopeType()),
                            conditions.of(entry.getConditionSource())))
                    .forEach(bundle::add);

            byId.put(role.getId(), new Described(role.getRoleName(), List.copyOf(bundle)));
        }

        return byId;
    }

    /**
     * ⚠️ A role that has gone is named by its identifier and carries nothing, rather than dropping the
     * assignment. An orphan is a fact worth seeing on the screen built to explain authorization —
     * silently omitting it is how somebody spends an afternoon looking for a row that is right there.
     */
    private RoleHolding holding(AccessRoleAssignment assignment, Described role) {
        return new RoleHolding(
                assignment.getSubjectId(),
                role == null ? assignment.getRoleId() : role.roleName(),
                placeOf(assignment.getScopeType(), assignment.getScopeId()),
                assignment.getGrantedBy(),
                localise(assignment.getCreatedAt()),
                assignment.getConditionSource(),
                role == null ? List.of() : role.bundle());
    }

    private DirectHolding holding(AccessSubjectPermission override) {
        return new DirectHolding(
                override.getSubjectId(),
                override.getPermission(),
                override.allows(),
                placeOf(override.getScopeType(), override.getScopeId()),
                override.getGrantedBy(),
                override.getReason(),
                localise(override.getCreatedAt()),
                override.getConditionSource());
    }

    private ScopeReference placeOf(String kind, String instance) {
        return ScopeReference.of(kind(kind), instance);
    }

    /**
     * ⚠️ An unregistered scope name is a programming error rather than a case to handle: the row was
     * written by this installation, against a vocabulary it registers.
     */
    private ScopeKind kind(String name) {
        return scopes.byName(name).orElseThrow(() -> new IllegalStateException(
                "A stored grant names the scope '" + name + "', which this installation does not "
                + "register."));
    }

    private static LocalDateTime localise(Instant moment) {
        return moment == null ? null : moment.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private record Described(String roleName, List<BundledPermission> bundle) {
    }
}
