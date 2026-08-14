package org.jmouse.access.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * One permission inside a role's bundle, and how far the role carries it.
 *
 * <h2>⚠️ The scope here is a KIND, never an instance</h2>
 *
 * <p>{@code @SPACE space:write} inside a role means <em>"as far as a place of that kind"</em>; which
 * place is decided by where the role is <em>assigned</em>. The same line naming an instance would
 * grant it there to everybody holding the role — so there is no column to hold one, and the language
 * refuses to express it. What a subject ends up holding is the <strong>narrower</strong> of this and
 * the assignment's, which is {@link #conferredAt}.
 *
 * <h2>The permission is the identity</h2>
 *
 * <p>By name, with no catalogue table behind it. The surrogate id this used to carry existed only so
 * that a third copy of the permission list could hand one out; the row is readable now without a
 * join, and a bundle entry is exactly what it says.
 */
@Entity
@Table(name = "access_role_permissions")
public class AccessRolePermission {

    @EmbeddedId
    private Key key;

    @Column(name = "scope_type", length = 64, nullable = false)
    private String scopeType;

    /**
     * ⚠️ The expression this entry only applies under, as <strong>source</strong>, or null.
     *
     * <p>Source rather than anything compiled, and the name says so on purpose. A compiled predicate
     * cannot be written back out, diffed, reviewed or shown in a code pane — and every one of those is
     * something this project does with a condition. Compiling is the reader's job and happens once per
     * distinct expression, not once per row and never once per request.
     */
    @Column(name = "condition_source", length = 1024)
    private String conditionSource;

    protected AccessRolePermission() {
    }

    public AccessRolePermission(String roleId, String permission, String scopeType) {
        this(roleId, permission, scopeType, null);
    }

    public AccessRolePermission(String roleId, String permission, String scopeType, String conditionSource) {
        this.key             = new Key(roleId, permission);
        this.scopeType       = scopeType;
        this.conditionSource = conditionSource;
    }

    public String getRoleId() {
        return key.roleId();
    }

    public String getPermission() {
        return key.permission();
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getConditionSource() {
        return conditionSource;
    }

    /** How far this entry reaches. Changing it is an edit to the entry, never a different entry. */
    public void carryAt(String scopeType) {
        this.scopeType = scopeType;
    }

    /**
     * What this entry applies under, or null for always.
     *
     * <p>An edit for the same reason {@link #carryAt} is: {@code (role, permission)} is the identity,
     * so changing when it applies changes this entry rather than making another one.
     */
    public void applyOnly(String conditionSource) {
        this.conditionSource = conditionSource;
    }

    /**
     * ⚠️ <strong>Identity is the key alone, and it has to be.</strong>
     *
     * <p>{@code scope_type} is not part of it: a role carries a permission once, so two entries with
     * the same {@code (role, permission)} are the same entry with a different reach — not two.
     * Without this, a {@code Set} of these compares by object identity, and reconciling a bundle
     * cannot tell an entry that survived from one that is new. Hibernate then meets two objects
     * holding one composite key and refuses the flush.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AccessRolePermission entry)) {
            return false;
        }

        return Objects.equals(key, entry.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    /** A role carries a permission once, at one scope. */
    @Embeddable
    public record Key(
            @Column(name = "role_id", length = 36, nullable = false) String roleId,
            @Column(name = "permission", length = 128, nullable = false) String permission
    ) implements Serializable {

        public Key {
            Objects.requireNonNull(roleId, "a bundle entry belongs to a role");
            Objects.requireNonNull(permission, "a bundle entry is about a permission");
        }
    }
}
