package org.jmouse.ai.mcp.authorization.server;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityManager;
import org.jmouse.ai.jpa.OwnTransaction;
import org.jmouse.ai.jpa.entity.AiClientRegistration;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * What a client called itself, in {@code ai_client_registrations}.
 *
 * <h2>⚠️ The bug this exists to end</h2>
 *
 * <p>The default registry is a {@code ConcurrentHashMap}, on the reasoning that a display name is a
 * claim nobody verified and losing one costs a client nothing. That is true about the client and wrong
 * about everything downstream: the name is <strong>copied onto the connection row</strong> at approval,
 * and in at least one product it <strong>becomes the agent's name</strong> — the thing a person then
 * grants permissions to and switches on and off.
 *
 * <p>So a restart between registration and approval baked <em>An unnamed client</em> into a durable
 * record, permanently. In development that happens every few minutes; and two clients that both landed
 * there became indistinguishable, which in a product that matches an agent by name is not cosmetic.
 *
 * <p>⚠️ <strong>Durable, not permanent, and the window slides.</strong> A registration is swept once it
 * expires, because a row nobody has used in a month is about a client that is gone. <em>Used</em> is the
 * load-bearing word: the client this is about caches its identifier and never registers again, so an
 * expiry counted from the registration would delete the row of the client using it most, and the symptom
 * would be a live connection renaming itself to <em>An unnamed client</em> — the exact bug above, arriving
 * a month later by a different route. So {@link #nameOf} extends the row it finds.
 *
 * <p>The sweep itself runs on registration rather than on a timer — this module owns no scheduler, and
 * the one moment a new row is written is the one moment the table is certainly being touched anyway.
 *
 * <p>⚠️ <strong>It still authorises nothing.</strong> The identifier confers no access: the credential
 * is protected by a loopback-only redirect, proof of possession, and a person approving a screen naming
 * both. This is a name and a clock.
 */
public final class JpaClientNameRegistry implements ClientNameRegistry {

    private static final String       IDENTIFIER_PREFIX = "mcp-client-";
    private static final int          IDENTIFIER_BYTES  = 24;
    private static final SecureRandom RANDOM            = new SecureRandom();

    private final EntityManagerFactory entityManagerFactory;
    private final Duration             lifetime;

    public JpaClientNameRegistry(EntityManagerFactory entityManagerFactory, Duration lifetime) {
        this.entityManagerFactory = entityManagerFactory;
        this.lifetime             = lifetime;
    }

    @Override
    public String register(String clientName) {
        String  clientId = IDENTIFIER_PREFIX + randomIdentifier();
        Instant now      = Instant.now();

        OwnTransaction.run(entityManagerFactory, entityManager -> {
            forgetExpired(entityManager, now);

            entityManager.persist(new AiClientRegistration(
                    clientId, ClientNameRegistry.describe(clientName), now, now.plus(lifetime)));
        });

        return clientId;
    }

    /**
     * ⚠️ Answers a name for an unknown identifier rather than refusing, and renews the one it finds.
     *
     * <p>Refusing <em>here</em> would be this method pretending the identifier means something it does
     * not; {@link #recognises} is where that question is asked and answered honestly. Being unnamed is a
     * worse label, never a closed door.
     */
    @Override
    public String nameOf(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return UNNAMED;
        }

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            Instant              now   = Instant.now();
            AiClientRegistration found = live(entityManager, clientId, now);

            if (found == null) {
                return UNNAMED;
            }

            found.renewUntil(now.plus(lifetime));

            return found.clientName();
        });
    }

    /** ⚠️ Read-only on purpose: knowing whether a row exists must not be what keeps it alive. */
    @Override
    public boolean recognises(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return false;
        }

        return OwnTransaction.call(entityManagerFactory,
                entityManager -> live(entityManager, clientId, Instant.now()) != null);
    }

    private static AiClientRegistration live(EntityManager entityManager, String clientId, Instant now) {
        AiClientRegistration found = entityManager.find(AiClientRegistration.class, clientId);

        return found == null || found.hasExpired(now) ? null : found;
    }

    private static void forgetExpired(EntityManager entityManager, Instant now) {
        entityManager
                .createQuery("DELETE FROM AiClientRegistration WHERE expiresAt < :now")
                .setParameter("now", now)
                .executeUpdate();
    }

    private static String randomIdentifier() {
        byte[] bytes = new byte[IDENTIFIER_BYTES];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
