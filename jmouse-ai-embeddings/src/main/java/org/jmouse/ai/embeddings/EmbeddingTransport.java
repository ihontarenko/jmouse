package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderSettings;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/** Injectable bounded JSON exchange; transport implementations must not follow credential-bearing redirects. */
public interface EmbeddingTransport {

    Object post(URI endpoint, ProviderSettings settings, Object body, CallLimits limits);

    /**
     * Header-aware exchange. Existing transports retain their original contract and default Bearer behavior;
     * a transport must explicitly implement custom authentication, rather than silently dropping headers.
     */
    default Object post(URI endpoint, ProviderSettings settings, Object body, Map<String, String> headers, CallLimits limits) {
        if (!bearerHeaders(settings).equals(headers)) {
            throw new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null);
        }
        return post(endpoint, settings, body, limits);
    }

    static Map<String, String> bearerHeaders(ProviderSettings settings) {
        if (!settings.hasApiKey()) {
            return Map.of();
        }
        return Map.of("Authorization", "Bearer " + settings.apiKey());
    }

    static URI requireEndpoint(String address) {
        if (address == null || address.isBlank()) {
            throw new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null);
        }
        try {
            URI endpoint = URI.create(address);
            if (!Set.of("http", "https").contains(endpoint.getScheme()) || endpoint.getHost() == null
                    || endpoint.getUserInfo() != null || endpoint.getRawQuery() != null || endpoint.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            return endpoint;
        } catch (IllegalArgumentException failure) {
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
