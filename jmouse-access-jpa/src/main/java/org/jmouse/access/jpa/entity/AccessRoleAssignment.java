package org.jmouse.access.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Who holds a role, and where.
 *
 * <h2>⚠️ `subjectId` is a plain identifier, and there is no association behind it</h2>
 *
 * <p>A library table cannot foreign-key into a product's account table, and it should not want to:
 * the SPI has always said <em>subject</em>, and a service account or an agent is one. {@code user_id}
 * named a concept this engine does not have.
 *
 * <p>The cost is that deleting an account no longer cascades, and the gap is <strong>silent</strong>
 * if forgotten — orphan assignments simply sit here and a projection reports them. That is why
 * {@code AccessAdministration.revokeAllFor} exists and why it lives in this library rather than in
 * every adopting product: the constraint is the library's, so the answer to it is too.
 *
 * <h2>`grantedBy` has nothing behind it either, deliberately</h2>
 *
 * <p>A granter whose account is deleted must leave the grant <em>standing and legible</em>. "Granted
 * by somebody who has since left" is the honest record; a {@code SET NULL} would erase the one fact
 * an audit actually asks for.
 */
@Entity
@Table(name = "access_role_assignments")
public class AccessRoleAssignment {

    @jakarta.persistence.Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "subject_id", length = 36, nullable = false)
    private String subjectId;

    @Column(name = "role_id", length = 36, nullable = false)
    private String roleId;

    @Column(name = "scope_type", length = 64, nullable = false)
    private String scopeType;

    /** The place, or {@code *} for every place of this kind. A universal scope still writes a value. */
    @Column(name = "scope_id", length = 36, nullable = false)
    private String scopeId;

    /**
     * Where this assignment came from, as a plain name.
     *
     * <p>No enum and no constraint: a product may issue an assignment from a mechanism this library
     * has never heard of, and refusing the row would be the engine legislating about somebody else's
     * provenance vocabulary.
     */
    @Column(name = "source", length = 32, nullable = false)
    private String source;

    /** The expression this assignment only applies under, as source, or null. */
    @Column(name = "condition_source", length = 1024)
    private String conditionSource;

    @Column(name = "granted_by", length = 36)
    private String grantedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccessRoleAssignment() {
    }

    public AccessRoleAssignment(String id, String subjectId, String roleId, String scopeType,
                                String scopeId, String source, String grantedBy) {
        this(id, subjectId, roleId, scopeType, scopeId, source, grantedBy, null);
    }

    public AccessRoleAssignment(String id, String subjectId, String roleId, String scopeType,
                                String scopeId, String source, String grantedBy,
                                String conditionSource) {
        this.id              = id;
        this.subjectId       = subjectId;
        this.roleId          = roleId;
        this.scopeType       = scopeType;
        this.scopeId         = scopeId;
        this.source          = source;
        this.grantedBy       = grantedBy;
        this.conditionSource = conditionSource;
        this.createdAt       = Instant.now();
    }

    public String getId()         { return id; }
    public String getSubjectId()  { return subjectId; }
    public String getRoleId()     { return roleId; }
    public String getScopeType()  { return scopeType; }
    public String getScopeId()    { return scopeId; }
    public String getSource()     { return source; }
    public String getGrantedBy()  { return grantedBy; }
    public Instant getCreatedAt() { return createdAt; }

    public String getConditionSource() { return conditionSource; }
}
