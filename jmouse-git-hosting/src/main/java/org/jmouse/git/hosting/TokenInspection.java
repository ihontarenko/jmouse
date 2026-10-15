package org.jmouse.git.hosting;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/** What a hosting says about an access token: accepted, with what it may do, or rejected. */
public sealed interface TokenInspection permits TokenInspection.Accepted, TokenInspection.Rejected {

    /** Whether the hosting accepted the token. */
    boolean valid();

    /**
     * The hosting accepted the token.
     *
     * @param account        the account the token acts as, or {@code null} when the token may not read its own
     *                       account (a GitLab token without {@code read_user})
     * @param scopes         the scopes the hosting reports for it; empty when it reports none
     * @param scopesReported whether the hosting reports scopes for this kind of token at all — GitHub does for classic
     *                       tokens and not for fine-grained ones, GitLab does for personal, project and group access
     *                       tokens and not for OAuth tokens. When {@code false}, an empty {@code scopes} means
     *                       "unknown", not "none"
     * @param expiresAt      when the token expires, or {@code null} when it does not or the hosting does not say
     */
    record Accepted(String account, Set<String> scopes, boolean scopesReported, Instant expiresAt) implements TokenInspection {

        public Accepted {
            scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
        }

        @Override
        public boolean valid() {
            return true;
        }

        /** The account, when the token may read it. */
        public Optional<String> accountName() {
            return Optional.ofNullable(account);
        }

        /** Whether the hosting reported this scope for the token. */
        public boolean hasScope(String scope) {
            return scopes.contains(scope);
        }
    }

    /** The hosting rejected the token: unknown, revoked or expired. */
    record Rejected() implements TokenInspection {

        @Override
        public boolean valid() {
            return false;
        }
    }
}
