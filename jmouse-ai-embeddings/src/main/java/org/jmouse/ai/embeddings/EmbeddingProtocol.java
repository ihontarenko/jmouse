package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.Map;

/**
 * Open wire-format extension point used by {@link HttpEmbeddingModel}.
 * Implementations are immutable and safe to share between concurrent calls.
 * JSON-shaped values belong here; callers continue to use {@link EmbeddingModel}.
 */
public interface EmbeddingProtocol {

    /** Stable registry code, independent of the vendor's model name. */
    String code();

    boolean supports(String providerName);

    default boolean supportsInputType(EmbeddingModel.InputType inputType) {
        return inputType == EmbeddingModel.InputType.UNSPECIFIED;
    }

    /** Called only after provider support has been established. */
    boolean requiresApiKey(String providerName);

    /** Provider authentication headers; common JSON headers and redirect policy belong to the transport. */
    default Map<String, String> headers(ProviderSettings settings) {
        return EmbeddingTransport.bearerHeaders(settings);
    }

    /** Builds JSON-compatible request values without I/O or mutating caller settings. */
    Object encode(ProviderSettings settings, EmbeddingModel.Request request);

    /**
     * Restores original input order and reported usage; absent usage remains null.
     * Malformed provider values must throw INVALID_RESPONSE without including their contents.
     * The HTTP model also validates returned count, dimensions and finite coordinates.
     */
    EmbeddingModel.Response decode(Object response, ProviderSettings settings, EmbeddingModel.Request request);
}
