package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;

/** Voyage indexed float embeddings with an explicit optional retrieval role and native dimensions. */
public final class VoyageEmbeddingProtocol extends AbstractEmbeddingProtocol {

    public static final String CODE = "VOYAGE";

    private static final String MODEL = "model";
    private static final String INPUT = "input";
    private static final String INPUT_TYPE = "input_type";
    private static final String TRUNCATION = "truncation";
    private static final String OUTPUT_DTYPE = "output_dtype";
    private static final String FLOAT = "float";
    private static final String TOTAL_TOKENS = "total_tokens";

    public VoyageEmbeddingProtocol() {
        super(CODE, Set.of("voyage"), Set.of("voyage"));
    }

    @Override
    public boolean supportsInputType(EmbeddingModel.InputType inputType) {
        return inputType != null;
    }

    @Override
    public Object encode(ProviderSettings settings, EmbeddingModel.Request request) {
        var body = new LinkedHashMap<String, Object>();
        body.put(MODEL, settings.model());
        body.put(INPUT, request.inputs());
        body.put(TRUNCATION, false);
        body.put(OUTPUT_DTYPE, FLOAT);
        if (request.inputType() != EmbeddingModel.InputType.UNSPECIFIED) {
            body.put(INPUT_TYPE, request.inputType().name().toLowerCase(Locale.ROOT));
        }
        return body;
    }

    @Override
    public EmbeddingModel.Response decode(Object response, ProviderSettings settings, EmbeddingModel.Request request) {
        return indexedResponse(response, settings, request, false, TOTAL_TOKENS);
    }
}
