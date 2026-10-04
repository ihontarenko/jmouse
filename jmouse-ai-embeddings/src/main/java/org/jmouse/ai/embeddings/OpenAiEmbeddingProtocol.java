package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.Map;
import java.util.Set;

/** OpenAI indexed float embeddings; compatible providers can be bound without modifying this adapter. */
public final class OpenAiEmbeddingProtocol extends AbstractEmbeddingProtocol {

    public static final String CODE = "OPENAI";

    private static final String MODEL = "model";
    private static final String INPUT = "input";
    private static final String ENCODING_FORMAT = "encoding_format";
    private static final String FLOAT = "float";
    private static final String PROMPT_TOKENS = "prompt_tokens";

    public OpenAiEmbeddingProtocol() {
        this(CODE, Set.of("openai", "ollama"), Set.of("openai"));
    }

    public OpenAiEmbeddingProtocol(String code, Set<String> providers, Set<String> authenticatedProviders) {
        super(code, providers, authenticatedProviders);
    }

    @Override
    public Object encode(ProviderSettings settings, EmbeddingModel.Request request) {
        return Map.of(MODEL, settings.model(), INPUT, request.inputs(), ENCODING_FORMAT, FLOAT);
    }

    @Override
    public EmbeddingModel.Response decode(Object response, ProviderSettings settings, EmbeddingModel.Request request) {
        return indexedResponse(response, settings, request, true, PROMPT_TOKENS);
    }
}
