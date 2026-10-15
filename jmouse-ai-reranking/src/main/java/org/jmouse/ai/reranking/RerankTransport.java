package org.jmouse.ai.reranking;

import org.jmouse.ai.provider.ProviderSettings;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/** Injectable bounded JSON exchange; implementations must not follow credential-bearing redirects. */
@FunctionalInterface
public interface RerankTransport {

    Object post(URI endpoint, Object body, Map<String, String> headers, RerankLimits limits);

    static Map<String, String> bearerHeaders(ProviderSettings settings) {
        if (!settings.hasApiKey()) {
            return Map.of();
        }
        return Map.of("Authorization", "Bearer " + settings.apiKey());
    }

    static URI requireEndpoint(String address) {
        if (address == null || address.isBlank()) {
            throw new RerankException(RerankException.Reason.CONFIGURATION, null);
        }
        try {
            URI endpoint = URI.create(address);
            if (!Set.of("http", "https").contains(endpoint.getScheme()) || endpoint.getHost() == null
                    || endpoint.getUserInfo() != null || endpoint.getRawQuery() != null || endpoint.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            return endpoint;
        } catch (IllegalArgumentException failure) {
            throw new RerankException(RerankException.Reason.CONFIGURATION, null);
        }
    }
}
