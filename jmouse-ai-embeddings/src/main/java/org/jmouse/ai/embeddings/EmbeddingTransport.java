package org.jmouse.ai.embeddings;

import java.net.URI;
import org.jmouse.ai.provider.ProviderSettings;

/** Injectable bounded JSON exchange; transport implementations must not follow credential-bearing redirects. */
public interface EmbeddingTransport {
    Object post(URI endpoint, ProviderSettings settings, Object body, CallLimits limits);

    static URI requireEndpoint(String address) {
        try {
            URI endpoint = URI.create(address);
            if (!java.util.Set.of("http", "https").contains(endpoint.getScheme()) || endpoint.getHost() == null
                    || endpoint.getUserInfo() != null || endpoint.getRawQuery() != null || endpoint.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            return endpoint;
        } catch (RuntimeException failure) {
            throw new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null);
        }
    }

    static boolean sameOrigin(URI first, URI second) {
        return first.getScheme().equals(second.getScheme()) && first.getHost().equalsIgnoreCase(second.getHost())
                && port(first) == port(second);
    }

    private static int port(URI endpoint) {
        return endpoint.getPort() >= 0 ? endpoint.getPort() : endpoint.getScheme().equals("https") ? 443 : 80;
    }
}

