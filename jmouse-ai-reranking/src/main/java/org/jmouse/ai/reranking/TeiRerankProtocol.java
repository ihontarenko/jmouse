package org.jmouse.ai.reranking;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Hugging Face Text Embeddings Inference {@code POST /rerank}, serving a local cross-encoder. The request
 * is {@code query}, {@code texts} and flags; the answer is a bare array of {@code index} and
 * {@code score}.
 *
 * <p>TEI has no top-N parameter, so a request for the best few is cut here, after ordering. It reports
 * no usage, so usage stays unknown. Truncation is off, like Voyage's: an over-long input is refused,
 * never shortened behind the caller's back.
 */
public final class TeiRerankProtocol extends AbstractRerankProtocol {

    public static final String CODE = "TEI";

    private static final String QUERY       = "query";
    private static final String TEXTS       = "texts";
    private static final String RAW_SCORES  = "raw_scores";
    private static final String RETURN_TEXT = "return_text";
    private static final String TRUNCATE    = "truncate";
    private static final String INDEX       = "index";
    private static final String SCORE       = "score";

    public TeiRerankProtocol() {
        super(CODE, Set.of("tei"), Set.of());
    }

    @Override
    public Object encode(ProviderSettings settings, RerankModel.Request request) {
        var body = new LinkedHashMap<String, Object>();
        body.put(QUERY, request.query());
        body.put(TEXTS, request.documents());
        body.put(RAW_SCORES, false);
        body.put(RETURN_TEXT, false);
        body.put(TRUNCATE, false);
        return body;
    }

    @Override
    public RerankModel.Response decode(Object response, ProviderSettings settings, RerankModel.Request request) {
        List<RerankModel.Ranked> ranked = RerankResponseReader.rows(RerankResponseReader.array(response), INDEX, SCORE);
        if (request.topResults() != null && ranked.size() > request.topResults()) {
            ranked = ranked.stream()
                    .sorted(Comparator.comparingDouble(RerankModel.Ranked::score).reversed())
                    .limit(request.topResults())
                    .toList();
        }
        return new RerankModel.Response(ranked, null);
    }
}
