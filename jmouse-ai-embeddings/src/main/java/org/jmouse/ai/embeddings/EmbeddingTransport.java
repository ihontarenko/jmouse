package org.jmouse.ai.embeddings;

import org.jmouse.http.client.HttpEndpoint;
import org.jmouse.ai.provider.ProviderSettings;

import java.net.URI;
import java.util.Map;

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
        return Map.of("Authorization", HttpEndpoint.bearer(settings.apiKey()));
    }

    /** The provider's address, where it is one a service is called at ({@link HttpEndpoint}); refused as configuration otherwise. */
    static URI requireEndpoint(String address) {
        return HttpEndpoint.of(address).orElseThrow(() -> new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null));
    }


}
