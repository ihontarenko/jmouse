package org.jmouse.ai.reranking;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.List;

/**
 * Reorders candidate documents by their relevance to one query, independent of where the documents came
 * from, how they are stored and what the caller does with the order.
 */
public interface RerankModel {

    String code();

    boolean supports(String providerName);

    Response rerank(ProviderSettings settings, Request request, RerankLimits limits);

    /**
     * One query and the documents to order. {@code topResults} asks for only the best few; null asks for
     * every document back, ordered.
     */
    record Request(String query, List<String> documents, Integer topResults) {

        public Request(String query, List<String> documents) {
            this(query, documents, null);
        }

        public Request {
            documents = List.copyOf(documents);
            if (query == null || query.isEmpty() || documents.isEmpty()
                    || documents.stream().anyMatch(document -> document == null || document.isEmpty())) {
                throw new IllegalArgumentException("A nonempty query and nonempty documents are required.");
            }
            if (topResults != null && (topResults < 1 || topResults > documents.size())) {
                throw new IllegalArgumentException("Top results must be between one and the number of documents.");
            }
        }

        /** How many ranked documents a correct response carries. */
        public int expectedResults() {
            return topResults == null ? documents.size() : topResults;
        }
    }

    /**
     * One document's place: its index in the request and the score the provider gave it. Scores are
     * comparable only within one response, and only to order — their scale is the provider's own.
     */
    record Ranked(int index, double score) {
        public Ranked {
            if (index < 0 || !Double.isFinite(score)) {
                throw new IllegalArgumentException("A ranked document needs a valid index and a finite score.");
            }
        }
    }

    /** Best first. Usage null means the provider did not report it; zero never substitutes for unknown. */
    record Response(List<Ranked> ranked, Long inputTokens) {
        public Response {
            ranked = List.copyOf(ranked);
            if (inputTokens != null && inputTokens < 0) {
                throw new IllegalArgumentException("Reported input usage cannot be negative.");
            }
        }
    }
}
