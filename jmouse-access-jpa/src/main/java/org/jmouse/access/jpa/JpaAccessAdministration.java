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
import java.util.Objects;
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

    private final EntityManager    entityManager;
    private final Supplier<String> identifiers;

    public JpaAccessAdministration(EntityManager entityManager) {
        this(entityManager, () -> UUID.randomUUID().toString());
    }

    /**
     * ⚠️ <strong>No {@code DeclaredRoleBundles} any more.</strong> Writing knew about declared bundles
     * for one reason — to refuse editing one — and that refusal is gone; see {@link #setBundle}. What
     * a document declares is still a reading concern, and reading is
     * {@link JpaGrantStore}'s and {@link JpaAccessDisclosure}'s.
     *
     * @param identifiers how new rows are named. ⚠️ A seam because a product may already have an
     *                    identifier scheme its rows follow, and a library quietly imposing UUIDs on one
     *                    that does not is a schema decision made by accident.
     */
    public JpaAccessAdministration(EntityManager entityManager, Supplier<String> identifiers) {
        this.entityManager = entityManager;
        this.identifiers   = identifiers;
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
     * ⚠️ <strong>This no longer refuses a role whose bundle a document declares, and the reason is
     * that a document is now something that <em>becomes</em> rows.</strong>
     *
     * <p>The refusal was right while a file was a second grant store read beside the tables: an edit
     * here would have been unioned with the file, so a screen appeared to change what a reviewed
     * document states and quietly did not. But the first thing to write a declared bundle into a table
     * is the seeder, which is the file becoming the row — refusing it refuses the only act that
     * removes the disagreement.
     *
     * <p>Where a product still reads a document beside its tables, the guard belongs on the screen
     * rather than here, and with the two things a library cannot give it: the refusal in that
     * product's audit trail, and a message naming the file to edit instead.
     */
    @Override
    public RoleView setBundle(String roleName, List<BundleEntry> bundle) {
        AccessRole role = requireRole(roleName);

        Set<AccessRolePermission> entries = new LinkedHashSet<>();

        for (BundleEntry entry : bundle) {
            entries.add(new AccessRolePermission(
                    role.getId(), entry.permission(), entry.scopeType(), entry.conditionSource()));
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

    /**
     * ⚠️ An existing row with a <em>different</em> condition is a change, not a no-op.
     *
     * <p>The unique key is (subject, role, place), so "already assigned" used to be the whole
     * question. It is not once a condition is on the row: leaving the old predicate in place would
     * make removing a condition impossible and narrowing one silently ineffective, and the caller
     * would be told nothing changed — which would be true of the row and false of what they asked for.
     */
    @Override
    public Change assign(String subjectId, String roleName, ScopeReference at, String source, String by,
                         String conditionSource) {

        AccessRole                     role     = requireRole(roleName);
        Optional<AccessRoleAssignment> existing = assignment(subjectId, role.getId(), at);

        if (existing.isPresent()) {
            if (Objects.equals(existing.get().getConditionSource(), conditionSource)) {
                return Change.nothing(subjectId, roleName, at);
            }

            // Replaced rather than updated, for the same reason a grant is: the unique key would
            // refuse the second row anyway, and one decision reads better than an edit to somebody
            // else's.
            entityManager.remove(existing.get());
            entityManager.flush();
        }

        entityManager.persist(new AccessRoleAssignment(
                identifiers.get(), subjectId, role.getId(), at.type().name(), at.id(), source, by,
                conditionSource));

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
                        String reason, String by, String conditionSource) {

        refuseAnEverybodyAllow(subjectId, permission, effect);

        Optional<AccessSubjectPermission> existing = override(subjectId, permission, at);

        // ⚠️ The condition is part of "the same grant". Compared here rather than left out, because a
        // caller removing a condition would otherwise be told nothing changed while the predicate
        // stayed on the row and went on narrowing.
        //
        // ⚠️ AND SO IS THE REASON, for the same argument one step further along. It is not provenance
        // decorating the row: it is the sentence the refusal reads out, so a caller who rewrote it and
        // was told "nothing changed" goes on refusing people in the words they just replaced. That is
        // exactly what happened — a grant seeded once kept saying "Seeded from the policy files" no
        // matter what anybody typed into the policy editor afterwards, because the only two things
        // compared here were the effect and the condition.
        if (existing.isPresent()
            && existing.get().getEffect().equalsIgnoreCase(effect.name())
            && Objects.equals(existing.get().getConditionSource(), conditionSource)
            && Objects.equals(existing.get().getReason(), reason)) {

            return Change.nothing(subjectId, permission, at);
        }

        // ⚠️ Replaced rather than updated, so that flipping an ALLOW to a DENY reads as one decision
        // rather than as an edit to somebody else's. The unique key would refuse the second row anyway.
        //
        // ⚠️ Which means a REASON-ONLY edit resets `created_at`, so the Who view will say the grant was
        // made when somebody fixed a typo in its sentence. Known and left alone: the entity has no
        // setters on purpose, and growing one for this is a change to a table four products share.
        // Worth doing properly when somebody is already in here.
        existing.ifPresent(entityManager::remove);
        entityManager.flush();

        entityManager.persist(new AccessSubjectPermission(
                identifiers.get(), subjectId, permission, effect.name(),
                at.type().name(), at.id(), reason, by, conditionSource));

        return Change.made(subjectId, permission, at);
    }

    /**
     * ⚠️ The everybody-row may only take away.
     *
     * <p>The database says so too, and this says it first — a {@code CHECK} violation arrives as a
     * constraint name and a stack trace, which is the wrong sentence to show somebody who has just
     * written what they thought was an ordinary grant. The rule itself is not about tidiness: a
     * universal allow appears on no screen that lists what one person holds, so nobody would find it
     * by looking at any of the accounts it affected.
     */
    private static void refuseAnEverybodyAllow(String subjectId, String permission, Effect effect) {
        if (AccessSubjectPermission.EVERYBODY.equals(subjectId) && effect != Effect.DENY) {
            throw new IllegalArgumentException(
                    "'" + permission + "' cannot be ALLOWED to every subject at once. The '*' subject "
                    + "exists to take something away from everybody — a universal allow would be "
                    + "invisible to every screen that lists what one account holds. Grant it through a "
                    + "role instead, which is what a role is.");
        }
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
