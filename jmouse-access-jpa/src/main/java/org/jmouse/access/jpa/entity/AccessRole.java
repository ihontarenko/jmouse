package org.jmouse.access.jpa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A named bundle of permissions — <strong>and nothing else</strong>.
 *
 * <h2>⚠️ Why this class is here rather than in a product</h2>
 *
 * <p>Because the table is. Stage 2 moved the schema into this library and left the {@code @Entity}
 * classes on the other side of the seam, which produced the worst of both arrangements: a product
 * mapping classes onto a schema it does not own, kept honest by {@code ddl-auto: validate} — a hope
 * rather than a contract. The library may change a column in a release, and the only thing that
 * notices is somebody's failed startup.
 *
 * <p>The rule the whole family follows: <em>whoever owns the table owns the mapping.</em>
 *
 * <h2>What a role is not</h2>
 *
 * <p>It is not a thing to gate on. Nothing in an adopting product should ever ask <em>"does this
 * subject hold ROLE_ADMIN"</em> — a role is a route to permissions and the engine resolves through
 * it. A product that checks role names has authorization in two places, and they disagree the first
 * time somebody edits a bundle.
 */
@Entity
@Table(name = "access_roles")
public class AccessRole {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "role_name", length = 64, nullable = false)
    private String roleName;

    /**
     * The <strong>widest</strong> place this role may be handed out at, as a scope name.
     *
     * <p>A plain string rather than an enum, because the vocabulary belongs to the product: one
     * installation's floors are workspaces and accounts, another's are boards inside projects, and a
     * third has none at all. {@code ScopeCatalog} is what turns it back into a kind.
     */
    @Column(name = "assignable_at", length = 64, nullable = false)
    private String assignableAt;

    /**
     * What the role carries, and how far each entry reaches.
     *
     * <p>⚠️ {@code CascadeType.ALL} with {@code orphanRemoval} is right <em>here</em> and would be
     * wrong on an assignment: a bundle entry has no life outside its role, so removing it from the
     * set is deleting it. An assignment, by contrast, is about a subject the library does not own.
     *
     * <p>⚠️ <strong>The join column is read-only, and it has to be.</strong> {@code role_id} is half
     * of {@link AccessRolePermission}'s own composite key, so leaving this writable maps one column
     * twice and Hibernate refuses to build the session factory at all — naming the entity rather than
     * this line. The entry carries its own role identifier, set when it is constructed, so nothing is
     * lost by letting the key be the one thing that writes it.
     */
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "role_id", nullable = false, insertable = false, updatable = false)
    private Set<AccessRolePermission> bundle = new LinkedHashSet<>();

    protected AccessRole() {
    }

    public AccessRole(String id, String roleName, String assignableAt) {
        this.id           = id;
        this.roleName     = roleName;
        this.assignableAt = assignableAt;
    }

    public String getId() {
        return id;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getAssignableAt() {
        return assignableAt;
    }

    public void setAssignableAt(String assignableAt) {
        this.assignableAt = assignableAt;
    }

    public Set<AccessRolePermission> getBundle() {
        return bundle;
    }

    /**
     * Replaces what this role bundles.
     *
     * <p>⚠️ Mutates the existing collection rather than assigning a new one. Hibernate tracks the
     * instance it handed out; replacing the reference is how a save silently becomes a delete of
     * everything and an insert of everything, which for a role's bundle is a different history in the
     * audit log for the same outcome.
     *
     * <p>⚠️ <strong>An entry that survives keeps the instance already in the session.</strong> The
     * caller builds a fresh {@link AccessRolePermission} for every entry it wants, and for an entry
     * that is already there that new object carries a composite key Hibernate is already holding —
     * which is a {@code NonUniqueObjectException} on flush, not a silent problem but a 500 on the
     * ordinary case of adding one permission to a role that has three. So the survivors are matched
     * by key and left alone, and only the genuine additions and removals move.
     */
    public void replaceBundle(Set<AccessRolePermission> entries) {
        Map<AccessRolePermission, AccessRolePermission> wanted = new LinkedHashMap<>();

        entries.forEach(entry -> wanted.put(entry, entry));

        bundle.removeIf(held -> !wanted.containsKey(held));

        // A survivor keeps its instance and takes the new reach AND the new condition — identity is
        // the (role, permission) key, so an entry whose scope or condition changed is this entry
        // edited rather than a different one. ⚠️ Both, and forgetting the second is silent: the entry
        // survives the reconciliation, the row keeps yesterday's predicate, and the only symptom is a
        // rule that goes on applying after somebody removed it.
        bundle.forEach(held -> {
            AccessRolePermission asked = wanted.get(held);

            held.carryAt(asked.getScopeType());
            held.applyOnly(asked.getConditionSource());
        });

        entries.stream()
                .filter(entry -> !bundle.contains(entry))
                .forEach(bundle::add);
    }
}
