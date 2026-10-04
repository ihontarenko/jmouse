package org.jmouse.ai.embeddings;

/** Safe operational failure: never embeds request text, credentials or a provider response body. */
public final class EmbeddingException extends RuntimeException {
    public enum Reason { CONFIGURATION, TIMEOUT, TRANSPORT, PROVIDER_REFUSED, INVALID_RESPONSE, RESPONSE_LIMIT }
    private final Reason reason;
    private final Integer status;
    public EmbeddingException(Reason reason, Integer status) {
        super("Embedding operation failed: " + reason + (status == null ? "" : " (HTTP " + status + ")"));
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

