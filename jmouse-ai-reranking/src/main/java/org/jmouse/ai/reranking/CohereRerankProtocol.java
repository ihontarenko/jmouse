package org.jmouse.ai.reranking;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The Cohere rerank shape, which several providers share: Cohere {@code POST /v2/rerank}, Jina
 * {@code POST /v1/rerank}, and gateways that expose one of them. The request is {@code model},
 * {@code query}, {@code documents} and an optional {@code top_n}; the answer is {@code results} of
 * {@code index} and {@code relevance_score}.
 *
 * <p>Usage differs between them and is read where each reports it — Jina's {@code usage.total_tokens},
 * Cohere's {@code meta.tokens.input_tokens}. Cohere's {@code meta.billed_units.search_units} is a count
 * of searches, not tokens, so it is never passed off as usage; with neither, usage stays unknown.
 */
public final class CohereRerankProtocol extends AbstractRerankProtocol {

    public static final String CODE = "COHERE";

    private static final String MODEL           = "model";
    private static final String QUERY           = "query";
    private static final String DOCUMENTS       = "documents";
    private static final String TOP_N           = "top_n";
    private static final String RESULTS         = "results";
    private static final String INDEX           = "index";
    private static final String RELEVANCE_SCORE = "relevance_score";
    private static final String USAGE           = "usage";
    private static final String TOTAL_TOKENS    = "total_tokens";
    private static final String META            = "meta";
    private static final String TOKENS          = "tokens";
    private static final String INPUT_TOKENS    = "input_tokens";

    public CohereRerankProtocol() {
        super(CODE, Set.of("cohere", "jina", "gateway"), Set.of("cohere", "jina"));
    }

    @Override
    public Object encode(ProviderSettings settings, RerankModel.Request request) {
        var body = new LinkedHashMap<String, Object>();
        body.put(MODEL, settings.model());
        body.put(QUERY, request.query());
        body.put(DOCUMENTS, request.documents());
        if (request.topResults() != null) {
            body.put(TOP_N, request.topResults());
        }
        return body;
    }

    @Override
    public RerankModel.Response decode(Object response, ProviderSettings settings, RerankModel.Request request) {
        Map<?, ?> object = RerankResponseReader.object(response);
        var ranked = RerankResponseReader.rows(RerankResponseReader.array(object.get(RESULTS)), INDEX, RELEVANCE_SCORE);
        Long tokens = RerankResponseReader.optionalInteger(object, USAGE, TOTAL_TOKENS);
        if (tokens == null) {
            tokens = RerankResponseReader.optionalInteger(object, META, TOKENS, INPUT_TOKENS);
        }
        return new RerankModel.Response(ranked, tokens);
    }
}
