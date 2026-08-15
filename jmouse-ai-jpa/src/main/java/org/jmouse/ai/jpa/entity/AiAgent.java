package org.jmouse.ai.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jmouse.ai.agent.AgentAuthority;

import java.time.Instant;

/**
 * A named thing that acts for somebody.
 *
 * <p>⚠️ <strong>{@link #ownerReference} is not a foreign key and never becomes one.</strong> A library
 * table cannot reference a product's accounts without knowing what an account is, so this is an opaque
 * string the module stores and compares and never interprets. What follows is that nothing cascades when
 * an account is deleted — the port's {@code discardAllOwnedBy} is the product's side of that bargain.
 *
 * <p>⚠️ <strong>No privileges here.</strong> An agent's ceiling is its owner's, resolved by whatever
 * engine already authorizes the product's endpoints. A permission column on this row would be a second
 * answer to a question that already has one.
 */
@Entity
@Table(name = AiAgent.TABLE_NAME)
public class AiAgent {

    public static final String TABLE_NAME = "ai_agents";

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    /** Whoever it acts for, in the product's own vocabulary. Opaque, and unreferenced by design. */
    @Column(name = "owner_reference", length = 64, nullable = false, updatable = false)
    private String ownerReference;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    /**
     * ⚠️ {@link EnumType#STRING}, and never the ordinal. A number here would mean reordering the enum
     * silently reinterprets every stored row — and the two values are precisely "acts with everything its
     * owner holds" and "acts with almost nothing", so the reinterpretation would be a privilege change
     * nobody wrote.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "authority", length = 16, nullable = false)
    private AgentAuthority authority;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Null until it has actually acted. ⚠️ A refusal is not activity and must not be stamped here. */
    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    protected AiAgent() {
    }

    public AiAgent(
            String         id,
            String         ownerReference,
            String         name,
            AgentAuthority authority,
            boolean        enabled,
            Instant        createdAt) {

        this.id             = id;
        this.ownerReference = ownerReference;
        this.name           = name;
        this.authority      = authority;
        this.enabled        = enabled;
        this.createdAt      = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getOwnerReference() {
        return ownerReference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AgentAuthority getAuthority() {
        return authority;
    }

    public void setAuthority(AgentAuthority authority) {
        this.authority = authority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
