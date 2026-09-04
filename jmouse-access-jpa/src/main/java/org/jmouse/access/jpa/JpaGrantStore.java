package org.jmouse.access.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.jpa.entity.AccessRole;
import org.jmouse.access.jpa.entity.AccessRoleAssignment;
import org.jmouse.access.jpa.entity.AccessSubjectPermission;
import org.jmouse.access.spi.BundledPermission;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.GrantAttribution;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.RoleGrant;

import java.time.Instant;
import java.time.LocalDateTime;
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
 *
 * <h2>⚠️ Two things a row carries that it once could not</h2>
 *
 * <p><strong>A condition.</strong> This class used to say, in so many words, that a stored grant
 * carries none and that <em>"conditions exist only where they can be read, which is the document"</em>.
 * That was true while a document was in the read path beside these tables. Where it is not, the
 * sentence inverts: a condition has nowhere else to live, and a policy that narrows a permission by a
 * rule would simply stop narrowing it. The column holds source; {@link StoredConditions} turns it into
 * something askable, and refuses to be missing.
 *
 * <p><strong>Everybody.</strong> {@code subject_id = '*'} is a denial aimed at every account at once,
 * matched by one extra predicate rather than expanded into a row per account — see
 * {@link org.jmouse.access.jpa.entity.AccessSubjectPermission#EVERYBODY}. It is deliberately the same
 * trick a policy document plays at read time, for the same reason: a store must not be able to
 * enumerate accounts.
 */
public class JpaGrantStore implements GrantStore {

    private final EntityManager    entityManager;
    private final ScopeCatalog     scopes;
    private final StoredConditions conditions;

    public JpaGrantStore(EntityManager entityManager, ScopeCatalog scopes) {
        this(entityManager, scopes, StoredConditions.none());
    }

    public JpaGrantStore(EntityManager entityManager, ScopeCatalog scopes,
                         StoredConditions conditions) {

        this.entityManager = entityManager;
        this.scopes        = scopes;
        this.conditions    = conditions;
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

        // ⚠️ `IN (:subjectId, '*')` rather than a second query and a merge. The everybody-block is a
        // row about this subject as much as their own rows are — it is only written once instead of
        // once per account — so it belongs in the same predicate, ordered and filtered with them.
        List<AccessSubjectPermission> overrides = entityManager.createQuery("""
                        SELECT override FROM AccessSubjectPermission override
                         WHERE override.subjectId IN (:subjectId, :everybody)
                           AND override.scopeType IN :kinds
                           AND override.scopeId   IN :instances
                        """, AccessSubjectPermission.class)
                .setParameter("subjectId", subjectId)
                .setParameter("everybody", AccessSubjectPermission.EVERYBODY)
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

    /**
     * ⚠️ Includes the everybody-block, and it has to.
     *
     * <p>This is what a control room asks to show <em>what does this account hold</em>, and a denial
     * written at everybody is part of that answer — it is what is taking something away. Left out, the
     * screen would list a permission the engine then refuses, and the reason would be a row the screen
     * knows about and did not mention.
     */
    @Override
    public List<DirectGrant> directHeldBy(String subjectId) {
        return entityManager.createQuery("""
                        SELECT override FROM AccessSubjectPermission override
                         WHERE override.subjectId IN (:subjectId, :everybody)
                        """, AccessSubjectPermission.class)
                .setParameter("subjectId", subjectId)
                .setParameter("everybody", AccessSubjectPermission.EVERYBODY)
                .getResultList().stream()
                .map(this::toDirectGrant)
                .toList();
    }

    /**
     * One assignment, with everything its role bundles — <strong>from the table, and only the table</strong>.
     *
     * <p>⚠️ This used to union the row's bundle with one a policy document declared, so that a file
     * could own what a role carries while the assignment stayed a row. That is gone: a bundle has one
     * home now, and it is here. What the union cost was a question with two answers — <em>what does
     * this role carry</em> — of which a screen could only show one.
     */
    private RoleGrant toRoleGrant(AccessRoleAssignment assignment) {
        AccessRole role = entityManager.find(AccessRole.class, assignment.getRoleId());

        List<BundledPermission> bundle = new ArrayList<>();

        if (role != null) {
            role.getBundle().stream()
                    .map(entry -> new BundledPermission(
                            entry.getPermission(),
                            kind(entry.getScopeType()),
                            conditions.of(entry.getConditionSource())))
                    .forEach(bundle::add);
        }

        String roleName = role == null ? assignment.getRoleId() : role.getRoleName();

        return new RoleGrant(
                roleName,
                placeOf(assignment.getScopeType(), assignment.getScopeId()),
                bundle,
                // ⚠️ The assignment's own condition narrows everything the role carries, and a bundle
                // entry's narrows that one entry further. Both apply, which is what `narrowedBy`
                // composes rather than replaces — a narrowing of a narrowing is still a narrowing.
                //
                // ⚠️ AND THE REASON IS PART OF THE ATTRIBUTION, exactly as it is for a direct grant
                // below. This read `stored(grantedBy, since)` for as long as there was no column to
                // read from — so every explanation the engine produced about a role-derived power said
                // who and when and never why, which is the half somebody auditing actually wants.
                GrantAttribution.stored(
                                assignment.getGrantedBy(),
                                assignment.getReason(),
                                writtenAt(assignment.getCreatedAt()))
                        .narrowedBy(conditions.of(assignment.getConditionSource())));
    }

    private DirectGrant toDirectGrant(AccessSubjectPermission override) {
        return new DirectGrant(
                override.getPermission(),
                override.allows(),
                placeOf(override.getScopeType(), override.getScopeId()),
                GrantAttribution.stored(
                                override.getGrantedBy(), override.getReason(), writtenAt(override.getCreatedAt()))
                        .narrowedBy(conditions.of(override.getConditionSource())));
    }

    /** When a row says it was written, in the engine's words — or nothing, where it does not say. */
    private static LocalDateTime writtenAt(Instant created) {
        return created == null ? null : created.atZone(ZoneId.systemDefault()).toLocalDateTime();
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
