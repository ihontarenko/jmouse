package org.jmouse.access.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * What a place has turned <strong>off</strong> — axis 4, and it is a preference rather than a grant.
 *
 * <h2>⚠️ Why it is not a grant, given the two now sit side by side</h2>
 *
 * <p>A grant says what a place <em>may</em> have and is issued by whoever decides that; a switch says
 * what it <em>wants</em> and is flipped from an ordinary settings screen. Forcing one into the other's
 * shape would hand a preference an allowance, a validity window and a provenance it has no use for —
 * and would put a settings-screen write behind an interface that is read-only precisely because
 * issuing is a governed act.
 *
 * <h2>⚠️ Default-on, and this table names only the exceptions</h2>
 *
 * <p>A table listing what is <em>on</em> would have to know the catalogue to be complete, and would
 * answer wrongly for every capability introduced after its rows were written — a feature shipped next
 * year would arrive switched off everywhere, silently, with nobody having decided that.
 */
@Entity
@Table(name = "access_capability_switches")
public class AccessCapabilitySwitch {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "scope_type", length = 64, nullable = false)
    private String scopeType;

    @Column(name = "scope_id", length = 36, nullable = false)
    private String scopeId;

    @Column(name = "capability", length = 64, nullable = false)
    private String capability;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** Locked on, so that nobody's personal tidying-up can take it away. */
    @Column(name = "forced", nullable = false)
    private boolean forced = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccessCapabilitySwitch() {
    }

    public AccessCapabilitySwitch(String id, String scopeType, String scopeId, String capability,
                                  boolean enabled, boolean forced) {

        this.id         = id;
        this.scopeType  = scopeType;
        this.scopeId    = scopeId;
        this.capability = capability;
        this.enabled    = enabled;
        this.forced     = forced;
        this.createdAt  = Instant.now();
        this.updatedAt  = Instant.now();
    }

    /** Flips it, and says whether anything actually moved — a switch set to where it already is. */
    public boolean flip(boolean enabled, boolean forced) {
        if (this.enabled == enabled && this.forced == forced) {
            return false;
        }

        this.enabled   = enabled;
        this.forced    = forced;
        this.updatedAt = Instant.now();

        return true;
    }

    public String getId()         { return id; }
    public String getScopeType()  { return scopeType; }
    public String getScopeId()    { return scopeId; }
    public String getCapability() { return capability; }
    public boolean isEnabled()    { return enabled; }
    public boolean isForced()     { return forced; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
