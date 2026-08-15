package org.jmouse.ai.mcp.authorization.server;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default registry: a map that forgets, which is all a display name is worth.
 *
 * <p>Used when a product declares no {@link ClientNameRegistry} of its own. Losing one costs a client
 * nothing — it is shown as unnamed and connects anyway — so a table would make a pretence durable, and a
 * cache that a restart clears is an honest home for a claim nobody verified.
 *
 * <p>A product with somewhere better to put it, and a reason, declares its own bean.
 */
public class InMemoryClientNameRegistry implements ClientNameRegistry {

    private static final String       IDENTIFIER_PREFIX = "mcp-client-";
    private static final int          IDENTIFIER_BYTES  = 24;
    private static final Duration     LIFETIME          = Duration.ofDays(30);
    private static final SecureRandom RANDOM            = new SecureRandom();

    private final Map<String, NamedClient> clients = new ConcurrentHashMap<>();

    @Override
    public String register(String clientName) {
        forgetExpired();

        String clientId = IDENTIFIER_PREFIX + randomIdentifier();
        clients.put(clientId, new NamedClient(ClientNameRegistry.describe(clientName),
                                              Instant.now().plus(LIFETIME)));

        return clientId;
    }

    @Override
    public String nameOf(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return UNNAMED;
        }

        NamedClient client = clients.get(clientId);

        if (client == null || client.hasExpired(Instant.now())) {
            return UNNAMED;
        }

        return client.name();
    }

    private void forgetExpired() {
        Instant now = Instant.now();
        clients.values().removeIf(client -> client.hasExpired(now));
    }

    private static String randomIdentifier() {
        byte[] bytes = new byte[IDENTIFIER_BYTES];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record NamedClient(String name, Instant expiresAt) {

        boolean hasExpired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }
}
