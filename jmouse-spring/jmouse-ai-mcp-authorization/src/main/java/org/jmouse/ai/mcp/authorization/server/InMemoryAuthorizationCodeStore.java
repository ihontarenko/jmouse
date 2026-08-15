package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.mcp.authorization.AuthorizationCodeStore;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where an approved authorization waits for the client to come and spend it, when a product has not said
 * where else to put it.
 *
 * <p>⚠️ <strong>Consuming, not reading.</strong> {@link #redeem} removes before it answers, so a code
 * cannot be spent twice even if two requests arrive at once — {@link ConcurrentHashMap#remove(Object)} is
 * what makes single use a fact rather than an intention. An expired code is removed and then refused, so
 * a late arrival cannot be retried into working.
 *
 * <p>⚠️ <strong>In memory, and the window is what makes that defensible.</strong> A code lives about a
 * minute and is spent by a client sitting there waiting for it, so all a restart loses is a flow somebody
 * is watching and will immediately retry. The credential it redeems for outlives everything here.
 * <strong>An installation running more than one instance has to replace this</strong>, and the failure
 * would read as "that authorization code is unknown" on roughly half of attempts — which is exactly why
 * {@link AuthorizationCodeStore} is an interface.
 */
public class InMemoryAuthorizationCodeStore implements AuthorizationCodeStore {

    private static final int          CODE_BYTES = 32;
    private static final SecureRandom RANDOM     = new SecureRandom();

    private final Map<String, PendingAuthorization> pending = new ConcurrentHashMap<>();

    @Override
    public String issue(PendingAuthorization authorization) {
        forgetExpired();

        String code = randomCode();
        pending.put(code, authorization);

        return code;
    }

    @Override
    public Optional<PendingAuthorization> redeem(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(pending.remove(code))
                .filter(authorization -> !authorization.hasExpired(Instant.now()));
    }

    private void forgetExpired() {
        Instant now = Instant.now();
        pending.values().removeIf(authorization -> authorization.hasExpired(now));
    }

    private static String randomCode() {
        byte[] bytes = new byte[CODE_BYTES];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
