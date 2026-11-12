package org.jmouse.ai.reranking;

import org.jmouse.http.client.HttpEndpoint;
import org.jmouse.ai.provider.ProviderSettings;

import java.net.URI;
import java.util.Map;

/** Injectable bounded JSON exchange; implementations must not follow credential-bearing redirects. */
@FunctionalInterface
public interface RerankTransport {

    Object post(URI endpoint, Object body, Map<String, String> headers, RerankLimits limits);

    static Map<String, String> bearerHeaders(ProviderSettings settings) {
        if (!settings.hasApiKey()) {
            return Map.of();
        }
        return Map.of("Authorization", HttpEndpoint.bearer(settings.apiKey()));
    }

    /** The provider's address, where it is one a service is called at ({@link HttpEndpoint}); refused as configuration otherwise. */
    static URI requireEndpoint(String address) {
        return HttpEndpoint.of(address).orElseThrow(() -> new RerankException(RerankException.Reason.CONFIGURATION, null));
    }
}
