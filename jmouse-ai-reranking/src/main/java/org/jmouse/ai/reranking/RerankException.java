package org.jmouse.ai.reranking;

/** Safe operational failure: never embeds the query, documents, credentials or a provider response body. */
public final class RerankException extends RuntimeException {

    public enum Reason {
        CONFIGURATION,
        TIMEOUT,
        TRANSPORT,
        PROVIDER_REFUSED,
        INVALID_RESPONSE,
        RESPONSE_LIMIT
    }

    private final Reason  reason;
    private final Integer status;

    public RerankException(Reason reason, Integer status) {
        super("Rerank operation failed: " + reason + (status == null ? "" : " (HTTP " + status + ")"));
        this.reason = reason;
        this.status = status;
    }

    public Reason reason() {
        return reason;
    }

    public Integer status() {
        return status;
    }

    public boolean retryable() {
        return reason == Reason.TIMEOUT || reason == Reason.TRANSPORT || (status != null && (status == 429 || status >= 500));
    }
}
