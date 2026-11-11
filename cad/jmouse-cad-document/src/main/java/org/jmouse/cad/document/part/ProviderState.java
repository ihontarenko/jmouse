package org.jmouse.cad.document.part;

/**
 * Whether a provider can answer, and in words why not.
 *
 * @param status what it can do now
 * @param detail what a reader is told: how many parts it holds, what is missing, what went wrong
 */
public record ProviderState(Status status, String detail) {

    public enum Status {
        /** It answers. */
        READY,
        /** It is reading what it searches; ask again shortly. */
        INDEXING,
        /** It needs configuration it has not been given — a key, a folder. */
        NOT_CONFIGURED,
        /** It was configured and could not answer. */
        FAILED
    }

    public static ProviderState ready(String detail) {
        return new ProviderState(Status.READY, detail);
    }

    public static ProviderState indexing(String detail) {
        return new ProviderState(Status.INDEXING, detail);
    }

    public static ProviderState notConfigured(String detail) {
        return new ProviderState(Status.NOT_CONFIGURED, detail);
    }

    public static ProviderState failed(String detail) {
        return new ProviderState(Status.FAILED, detail);
    }

    public boolean ready() {
        return status == Status.READY;
    }
}
