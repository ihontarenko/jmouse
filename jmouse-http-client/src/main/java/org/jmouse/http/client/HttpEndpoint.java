package org.jmouse.http.client;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

/**
 * A service's address as a caller may be configured with one: HTTP or HTTPS, a host, and nothing else that would ride
 * along unseen — no credentials in it, no query, no fragment. What a caller does with one that is not is its own to say.
 */
public final class HttpEndpoint {

    private HttpEndpoint() {
    }

    /** The address, where it is one a service is called at; empty for a blank one, a malformed one, or one carrying more. */
    public static Optional<URI> of(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        try {
            URI endpoint = URI.create(address);
            boolean served = endpoint.getScheme() != null && HttpCall.SCHEMES.contains(endpoint.getScheme().toLowerCase(Locale.ROOT))
                    && endpoint.getHost() != null;
            boolean clean = served && endpoint.getUserInfo() == null && endpoint.getRawQuery() == null && endpoint.getFragment() == null;
            return clean ? Optional.of(endpoint) : Optional.empty();
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    /** Whether two addresses are of one origin: scheme, host and port — where a credential may go and not further. */
    public static boolean sameOrigin(URI first, URI second) {
        return first.getScheme().equalsIgnoreCase(second.getScheme()) && first.getHost().equalsIgnoreCase(second.getHost())
                && port(first) == port(second);
    }

    /** The {@code Authorization} header carrying a bearer token. */
    public static String bearer(String token) {
        return "Bearer " + token;
    }

    private static int port(URI endpoint) {
        return endpoint.getPort() >= 0 ? endpoint.getPort() : endpoint.getScheme().equalsIgnoreCase("https") ? 443 : 80;
    }
}
