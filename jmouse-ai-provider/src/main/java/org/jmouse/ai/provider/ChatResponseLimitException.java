package org.jmouse.ai.provider;

/** The response was cancelled before body decoding. The remote operation/usage may already have happened. */
public final class ChatResponseLimitException extends ProviderException {

    private final int maximumResponseBytes;

    public ChatResponseLimitException(int maximumResponseBytes) {
        super("CHAT response exceeded its admitted HTTP body byte allowance; remote usage is unknown.");
        this.maximumResponseBytes = maximumResponseBytes;
    }

    public int maximumResponseBytes() {
        return maximumResponseBytes;
    }
}
