package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.Map;
import java.util.Set;

/** Native TEI /embed; response is an ordered matrix and reports no usage. */
public final class TeiEmbeddingProtocol extends AbstractEmbeddingProtocol {

    public static final String CODE = "TEI";

    private static final String INPUTS = "inputs";
    private static final String TRUNCATE = "truncate";

    public TeiEmbeddingProtocol() {
        super(CODE, Set.of("tei"), Set.of());
    }

    @Override
    public Object encode(ProviderSettings settings, EmbeddingModel.Request request) {
        return Map.of(INPUTS, request.inputs(), TRUNCATE, false);
    }

    @Override
    public EmbeddingModel.Response decode(Object response, ProviderSettings settings, EmbeddingModel.Request request) {
        return new EmbeddingModel.Response(EmbeddingResponseReader.vectors(response, request), null);
    }
}
