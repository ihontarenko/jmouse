package org.jmouse.ai.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * What a client called itself when it registered, kept somewhere a restart cannot reach.
 *
 * <h2>⚠️ Why this is a table and the first version was a map</h2>
 *
 * <p>The reasoning for the map was that a display name is a claim nobody verified, so losing one costs a
 * client nothing — it shows as unnamed and connects anyway. That was true about the <em>client</em> and
 * wrong about everything downstream, because two things happen to the name after it is looked up:
 *
 * <ul>
 *   <li>it is <strong>copied onto the connection row</strong>, where it stays for the life of the
 *       credential; and
 *   <li>in at least one product it <strong>becomes the agent's name</strong>, which is what the person
 *       then grants permissions to and switches on and off.
 * </ul>
 *
 * <p>So a forgotten registration did not degrade a label — it baked <em>An unnamed client</em> into a
 * durable record, permanently, on nothing worse than a backend restart. In development that is every few
 * minutes.
 *
 * <p>⚠️ <strong>Still expires, and still confers nothing.</strong> Durable is not the same as permanent:
 * a registration nobody has used in a month is a row about a client that is gone. And the identifier here
 * still authorises nothing at all — the credential is protected by a loopback-only redirect, proof of
 * possession, and a person approving a screen. This row is a name and a clock.
 */
@Entity
@Table(name = AiClientRegistration.TABLE_NAME)
public class AiClientRegistration {

    public static final String TABLE_NAME = "ai_client_registrations";

    @Id
    @Column(name = "client_id", length = 128, nullable = false, updatable = false)
    private String clientId;

    /** ⚠️ A claim, shown as one, never an identity. */
    @Column(name = "client_name", length = 255, nullable = false)
    private String clientName;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected AiClientRegistration() {
    }

    public AiClientRegistration(String clientId, String clientName, Instant registeredAt, Instant expiresAt) {
        this.clientId     = clientId;
        this.clientName   = clientName;
        this.registeredAt = registeredAt;
        this.expiresAt    = expiresAt;
    }

    public String clientId() {
        return clientId;
    }

    public String clientName() {
        return clientName;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean hasExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    /**
     * Pushes the expiry out, because the registration is still in use.
     *
     * <p>⚠️ <strong>A sliding window, not a deadline.</strong> The client this row is about caches the
     * identifier it was issued and never registers again — so an expiry counted from
     * {@link #registeredAt} would sweep the row of the client using it most. The visible failure would be
     * a working connection renaming itself to <em>An unnamed client</em>, permanently, a month after
     * anybody last touched anything.
     */
    public void renewUntil(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
