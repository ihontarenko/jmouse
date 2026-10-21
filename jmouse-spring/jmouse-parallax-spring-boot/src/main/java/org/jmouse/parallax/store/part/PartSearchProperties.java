package org.jmouse.parallax.store.part;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * How parts are searched for beyond the catalogue.
 *
 * <pre>{@code
 * jmouse.parallax.parts.limit: 24
 * jmouse.parallax.parts.nexar.client-id: ${NEXAR_CLIENT_ID:}
 * jmouse.parallax.parts.nexar.client-secret: ${NEXAR_CLIENT_SECRET:}
 * }</pre>
 *
 * @param limit how many candidates each provider answers with at most
 * @param nexar Nexar's supply search (Octopart's data); without a client it is listed as not configured
 */
@ConfigurationProperties("jmouse.parallax.parts")
public record PartSearchProperties(int limit, Nexar nexar) {

    public PartSearchProperties {
        limit = limit <= 0 ? 24 : limit;
        nexar = nexar == null ? new Nexar(null, null, null, null, 0, null) : nexar;
    }

    /**
     * ⚠️ Nexar counts every part a supply query returns against the application's monthly allowance, so {@code limit}
     * is kept small.
     *
     * @param clientId     the Nexar application's client id; blank leaves the provider not configured
     * @param clientSecret its client secret
     * @param tokenUrl     where a token is asked for
     * @param apiUrl       the GraphQL endpoint
     * @param limit        how many parts one search asks Nexar for, at most 100
     * @param timeout      how long a search waits for Nexar
     */
    public record Nexar(String clientId, String clientSecret, String tokenUrl, String apiUrl, int limit,
                        Duration timeout) {

        public Nexar {
            clientId = clientId == null ? "" : clientId.strip();
            clientSecret = clientSecret == null ? "" : clientSecret.strip();
            tokenUrl = tokenUrl == null || tokenUrl.isBlank() ? "https://identity.nexar.com/connect/token" : tokenUrl.strip();
            apiUrl = apiUrl == null || apiUrl.isBlank() ? "https://api.nexar.com/graphql" : apiUrl.strip();
            limit = limit <= 0 ? 10 : Math.min(limit, 100);
            timeout = timeout == null ? Duration.ofSeconds(8) : timeout;
        }

        public boolean configured() {
            return !clientId.isEmpty() && !clientSecret.isEmpty();
        }
    }
}
