package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.Map;
import java.util.Set;

/** Native Ollama /api/embed; ordered vectors and nullable prompt evaluation usage. */
public final class OllamaEmbeddingProtocol extends AbstractEmbeddingProtocol {

    public static final String CODE = "OLLAMA";

    private static final String PROMPT_EVAL_COUNT = "prompt_eval_count";
    private static final String MODEL             = "model";
    private static final String INPUT             = "input";
    private static final String EMBEDDINGS        = "embeddings";
    private static final String TRUNCATE          = "truncate";

    public OllamaEmbeddingProtocol() {
        super(CODE, Set.of("ollama"), Set.of());
    }

    @Override
    public Object encode(ProviderSettings settings, EmbeddingModel.Request request) {
        return Map.of(MODEL, settings.model(), INPUT, request.inputs(), TRUNCATE, false);
    }

    @Override
    public EmbeddingModel.Response decode(Object response, ProviderSettings settings, EmbeddingModel.Request request) {
        Map<?, ?> object = EmbeddingResponseReader.object(response);
        EmbeddingResponseReader.requireModel(object, settings.model(), true);

        return new EmbeddingModel.Response(EmbeddingResponseReader.vectors(object.get(EMBEDDINGS), request),
                EmbeddingResponseReader.optionalInteger(object, PROMPT_EVAL_COUNT));
    }
}
