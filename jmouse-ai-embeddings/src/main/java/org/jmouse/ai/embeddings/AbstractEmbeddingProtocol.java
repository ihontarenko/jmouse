package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Shared immutable protocol identity, supported providers and credential requirements. */
public abstract class AbstractEmbeddingProtocol implements EmbeddingProtocol {

    private final String      code;
    private final Set<String> authenticatedProviders;
    private final Set<String> providers;

    protected AbstractEmbeddingProtocol(String code, Set<String> providers, Set<String> authenticatedProviders) {
        this.code = Objects.requireNonNull(code, "Protocol code is required.");
        this.authenticatedProviders = Set.copyOf(authenticatedProviders);
        this.providers = Set.copyOf(providers);

        if (code.isBlank() || providers.isEmpty() || providers.stream().anyMatch(String::isBlank)
                || !providers.containsAll(authenticatedProviders)) {
            throw new IllegalArgumentException("A protocol requires a code, supported providers and a valid credential subset.");
        }
    }

    @Override
    public final String code() {
        return code;
    }

    @Override
    public final boolean supports(String providerName) {
        return providerName != null && providers.contains(providerName);
    }

    @Override
    public final boolean requiresApiKey(String providerName) {
        return providerName != null && authenticatedProviders.contains(providerName);
    }

    /** Reusable indexed decoding for compatible formats, including protocols installed outside this module. */
    protected final EmbeddingModel.Response indexedResponse(Object response, ProviderSettings settings,
            EmbeddingModel.Request request, boolean requireReportedModel, String usageField) {
        Map<?, ?> object = EmbeddingResponseReader.object(response);
        EmbeddingResponseReader.requireModel(object, settings.model(), requireReportedModel);

        return new EmbeddingModel.Response(EmbeddingResponseReader.indexedVectors(object, request),
                EmbeddingResponseReader.optionalUsage(object, usageField));
    }
}
