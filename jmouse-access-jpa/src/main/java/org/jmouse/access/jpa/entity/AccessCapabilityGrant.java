package org.jmouse.access.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * What a <strong>place</strong> has been granted, by whom, and until when — the entitlement axis.
 *
 * <h2>⚠️ A place, never a person</h2>
 *
 * <p>A capability is held by an account, a workspace, a tenant. If per-person metering is ever wanted
 * it arrives as a <em>scope</em> rather than a second addressing scheme — the same decision that let
 * the old {@code MEMBERSHIP} subject be dropped without anything noticing.
 *
 * <h2>⚠️ A null allowance means two opposite things</h2>
 *
 * <p><strong>No ceiling</strong> where the capability is metered, and <strong>not metered</strong>
 * where it is not — and the period is what separates them. Only the resolver may decide which; a
 * screen reading this row directly will get it wrong, which is why nothing outside this module does.
 *
 * <h2>⚠️ Expired rows are kept and must never be filtered in a query</h2>
 *
 * <p><em>"Ran until 12 September"</em> and <em>"never had it"</em> are different answers with different
 * next moves. A predicate in the query collapses them into the same silence, which is the one thing
 * this axis exists to avoid.
 */
@Entity
@Table(name = "access_capability_grants")
public class AccessCapabilityGrant {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "scope_type", length = 64, nullable = false)
    private String scopeType;

    @Column(name = "scope_id", length = 36, nullable = false)
    private String scopeId;

    @Column(name = "capability", length = 64, nullable = false)
    private String capability;

    @Column(name = "grant_kind", length = 8, nullable = false)
    private String grantKind;

    /** The product's own provenance vocabulary — a plan, a purchase, an internal allocation. */
    @Column(name = "source", length = 32, nullable = false)
    private String source;

    /**
     * ⚠️ <strong>Which</strong> bundle issued this, where one did.
     *
     * <p>Without it, <em>"what is this account on"</em> has no answer at all — and a column holding it
     * beside these rows would be a second copy of what they already say, written by the same method.
     */
    @Column(name = "source_reference", length = 64)
    private String sourceReference;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "allowance")
    private Long allowance;

    @Column(name = "allowance_period", length = 8)
    private String allowancePeriod;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "granted_by", length = 36)
    private String grantedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccessCapabilityGrant() {
    }

    /**
     * A grant nobody has decided the terms of yet.
     *
     * <p>Only the four facts that <em>address</em> it are constructor arguments, because they are the
     * four that may never change afterwards: re-pointing a grant at another place or another capability
     * would be issuing a different grant while keeping the first one's history. Everything else is
     * {@link #restate}d, which is what re-issuing one is.
     */
    public AccessCapabilityGrant(String id, String scopeType, String scopeId, String capability,
                                 String grantKind, String source) {

        this.id         = id;
        this.scopeType  = scopeType;
        this.scopeId    = scopeId;
        this.capability = capability;
        this.grantKind  = grantKind;
        this.source     = source;
        this.validFrom  = Instant.now();
        this.createdAt  = Instant.now();
        this.updatedAt  = Instant.now();
    }

    /**
     * Restates the terms — every field a re-issue may change, and no field it may not.
     *
     * <p>⚠️ One method rather than a setter each, because the terms only make sense together: an
     * allowance without a period and a validity window without an end are different grants, and a
     * caller setting three of the six is a caller who has half-issued something.
     */
    public void restate(Instant validFrom, Instant validUntil, Long allowance, String allowancePeriod,
                        String sourceReference, String reason, String grantedBy) {

        this.validFrom       = validFrom == null ? Instant.now() : validFrom;
        this.validUntil      = validUntil;
        this.allowance       = allowance;
        this.allowancePeriod = allowancePeriod;
        this.sourceReference = sourceReference;
        this.reason          = reason;
        this.grantedBy       = grantedBy;
        this.updatedAt       = Instant.now();
    }

    public String getId()              { return id; }
    public String getScopeType()       { return scopeType; }
    public String getScopeId()         { return scopeId; }
    public String getCapability()      { return capability; }
    public String getGrantKind()       { return grantKind; }
    public String getSource()          { return source; }
    public String getSourceReference() { return sourceReference; }
    public Instant getValidFrom()      { return validFrom; }
    public Instant getValidUntil()     { return validUntil; }
    public Long getAllowance()         { return allowance; }
    public String getAllowancePeriod() { return allowancePeriod; }
    public String getReason()          { return reason; }
    public String getGrantedBy()       { return grantedBy; }
    public Instant getCreatedAt()      { return createdAt; }
    public Instant getUpdatedAt()      { return updatedAt; }

    public boolean allows() {
        return "ALLOW".equalsIgnoreCase(grantKind);
    }
}
