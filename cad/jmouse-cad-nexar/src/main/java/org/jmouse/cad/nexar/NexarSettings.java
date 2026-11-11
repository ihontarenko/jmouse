package org.jmouse.cad.nexar;

import java.time.Duration;

/**
 * How Nexar is reached: a Nexar application's client, and where its token and its GraphQL endpoint are.
 *
 * <p>⚠️ Nexar counts every part a supply query returns against the application's monthly allowance, so {@code limit}
 * is kept small.</p>
 *
 * @param clientId     the Nexar application's client id; blank leaves the provider not configured
 * @param clientSecret its client secret
 * @param tokenUrl     where a token is asked for; blank for Nexar's own
 * @param apiUrl       the GraphQL endpoint; blank for Nexar's own
 * @param limit        how many parts one search asks Nexar for, at most 100; 0 for 10
 * @param timeout      how long a search waits for Nexar; null for 8 seconds
 */
public record NexarSettings(String clientId, String clientSecret, String tokenUrl, String apiUrl, int limit,
                            Duration timeout) {

    /** Where Nexar issues a token for an application's client. */
    public static final String TOKEN_URL = "https://identity.nexar.com/connect/token";

    /** Nexar's GraphQL endpoint. */
    public static final String API_URL = "https://api.nexar.com/graphql";

    /** The most parts Nexar answers one supply search with. */
    private static final int MOST = 100;

    private static final int USUAL = 10;

    private static final Duration WAIT = Duration.ofSeconds(8);

    public NexarSettings {
        clientId = clientId == null ? "" : clientId.strip();
        clientSecret = clientSecret == null ? "" : clientSecret.strip();
        tokenUrl = tokenUrl == null || tokenUrl.isBlank() ? TOKEN_URL : tokenUrl.strip();
        apiUrl = apiUrl == null || apiUrl.isBlank() ? API_URL : apiUrl.strip();
        limit = limit <= 0 ? USUAL : Math.min(limit, MOST);
        timeout = timeout == null ? WAIT : timeout;
    }

    public boolean configured() {
        return !clientId.isEmpty() && !clientSecret.isEmpty();
    }
}
