package org.jmouse.access.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One permission handed to, or taken from, one subject <em>directly</em>.
 *
 * <h2>⚠️ `effect` is why this is not simply a list of grants</h2>
 *
 * <p>A {@code DENY} wins over every {@code ALLOW}, from anywhere, at every level. That is what makes
 * a row here the way an administrator takes something away that a role gives, without editing the
 * role that gives it to everybody else — and it is the same sentence the capability axis uses, so an
 * installation learns one rule.
 *
 * <p>{@code reason} is not decoration. A denial nobody can explain is a support ticket that outlives
 * whoever wrote it, and it is the one field a screen showing a refusal actually needs.
 */
@Entity
@Table(name = "access_subject_permissions")
public class AccessSubjectPermission {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    /** ⚠️ A plain identifier — see {@link AccessRoleAssignment} for why there is no association. */
    @Column(name = "subject_id", length = 36, nullable = false)
    private String subjectId;

    @Column(name = "permission", length = 128, nullable = false)
    private String permission;

    @Column(name = "effect", length = 8, nullable = false)
    private String effect;

    @Column(name = "scope_type", length = 64, nullable = false)
    private String scopeType;

    @Column(name = "scope_id", length = 36, nullable = false)
    private String scopeId;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "granted_by", length = 36)
    private String grantedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccessSubjectPermission() {
    }

    public AccessSubjectPermission(String id, String subjectId, String permission, String effect,
                                   String scopeType, String scopeId, String reason, String grantedBy) {
        this.id         = id;
        this.subjectId  = subjectId;
        this.permission = permission;
        this.effect     = effect;
        this.scopeType  = scopeType;
        this.scopeId    = scopeId;
        this.reason     = reason;
        this.grantedBy  = grantedBy;
        this.createdAt  = Instant.now();
    }

    public String getId()         { return id; }
    public String getSubjectId()  { return subjectId; }
    public String getPermission() { return permission; }
    public String getEffect()     { return effect; }
    public String getScopeType()  { return scopeType; }
    public String getScopeId()    { return scopeId; }
    public String getReason()     { return reason; }
    public String getGrantedBy()  { return grantedBy; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean allows() {
        return "ALLOW".equalsIgnoreCase(effect);
    }
}
