package org.jmouse.ai.reranking;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.Map;

/**
 * Open wire-format extension point used by {@link HttpRerankModel}.
 * Implementations are immutable and safe to share between concurrent calls.
 * JSON-shaped values belong here; callers continue to use {@link RerankModel}.
 */
public interface RerankProtocol {

    /** Stable registry code, independent of the vendor's model name. */
    String code();

    boolean supports(String providerName);

    /** Called only after provider support has been established. */
    boolean requiresApiKey(String providerName);

    /** Provider authentication headers; common JSON headers and redirect policy belong to the transport. */
    default Map<String, String> headers(ProviderSettings settings) {
        return RerankTransport.bearerHeaders(settings);
    }

    /** Builds JSON-compatible request values without I/O or mutating caller settings. */
    Object encode(ProviderSettings settings, RerankModel.Request request);

    /**
     * Reads the provider's ranked results and reported usage; absent usage remains null. Malformed values
     * throw INVALID_RESPONSE without their contents. The HTTP model then checks the indices, the count and
     * the scores, and puts the results best first.
     */
    RerankModel.Response decode(Object response, ProviderSettings settings, RerankModel.Request request);
}
