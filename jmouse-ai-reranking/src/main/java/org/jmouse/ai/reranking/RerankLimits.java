package org.jmouse.ai.reranking;

import java.nio.charset.StandardCharsets;

/** Explicit per-call resource ceilings supplied by the adopter's managed policy. */
public record RerankLimits(int maximumDocuments, int maximumQueryCodePoints, int maximumDocumentCodePoints,
        int maximumTotalCodePoints, int timeoutMilliseconds, int maximumResponseBytes) {

    public RerankLimits {
        if (maximumDocuments < 1 || maximumDocuments > 1000
                || maximumQueryCodePoints < 1 || maximumQueryCodePoints > 65536
                || maximumDocumentCodePoints < 1 || maximumDocumentCodePoints > 1048576
                || maximumTotalCodePoints < 1 || maximumTotalCodePoints > 8388608
                || timeoutMilliseconds < 10 || timeoutMilliseconds > 120000
                || maximumResponseBytes < 1024 || maximumResponseBytes > 67108864) {
            throw new IllegalArgumentException("Explicit bounded rerank call limits are required.");
        }
    }

    public void validate(RerankModel.Request request) {
        if (request.documents().size() > maximumDocuments) {
            throw new IllegalArgumentException("Rerank document count exceeds the configured ceiling.");
        }
        long total = codePoints(request.query(), maximumQueryCodePoints);
        for (String document : request.documents()) {
            total += codePoints(document, maximumDocumentCodePoints);
        }
        if (total > maximumTotalCodePoints) {
            throw new IllegalArgumentException("Rerank input exceeds the configured Unicode ceiling.");
        }
    }

    private static int codePoints(String text, int ceiling) {
        if (!StandardCharsets.UTF_8.newEncoder().canEncode(text)) {
            throw new IllegalArgumentException("Rerank inputs must be valid Unicode.");
        }
        int count = text.codePointCount(0, text.length());
        if (count > ceiling) {
            throw new IllegalArgumentException("A rerank input exceeds its configured Unicode ceiling.");
        }
        return count;
    }
}
