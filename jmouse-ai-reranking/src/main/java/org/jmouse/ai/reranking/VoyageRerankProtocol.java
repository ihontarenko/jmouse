package org.jmouse.ai.reranking;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Voyage AI {@code POST /v1/rerank} (models rerank-2.5, rerank-2.5-lite, rerank-2…).
 *
 * <p>Truncation is switched off: an input over the model's limit is refused by the provider rather than
 * silently shortened, so a score always describes the whole document the caller sent.
 */
public final class VoyageRerankProtocol extends AbstractRerankProtocol {

    public static final String CODE = "VOYAGE";

    private static final String QUERY            = "query";
    private static final String DOCUMENTS        = "documents";
    private static final String MODEL            = "model";
    private static final String TOP_K            = "top_k";
    private static final String RETURN_DOCUMENTS = "return_documents";
    private static final String TRUNCATION       = "truncation";
    private static final String DATA             = "data";
    private static final String INDEX            = "index";
    private static final String RELEVANCE_SCORE  = "relevance_score";
    private static final String USAGE            = "usage";
    private static final String TOTAL_TOKENS     = "total_tokens";

    public VoyageRerankProtocol() {
        super(CODE, Set.of("voyage"), Set.of("voyage"));
    }

    @Override
    public Object encode(ProviderSettings settings, RerankModel.Request request) {
        var body = new LinkedHashMap<String, Object>();
        body.put(QUERY, request.query());
        body.put(DOCUMENTS, request.documents());
        body.put(MODEL, settings.model());
        if (request.topResults() != null) {
            body.put(TOP_K, request.topResults());
        }
        body.put(RETURN_DOCUMENTS, false);
        body.put(TRUNCATION, false);
        return body;
    }

    @Override
    public RerankModel.Response decode(Object response, ProviderSettings settings, RerankModel.Request request) {
        var object = RerankResponseReader.object(response);
        var ranked = RerankResponseReader.rows(RerankResponseReader.array(object.get(DATA)), INDEX, RELEVANCE_SCORE);
        return new RerankModel.Response(ranked, RerankResponseReader.optionalInteger(object, USAGE, TOTAL_TOKENS));
    }
}
