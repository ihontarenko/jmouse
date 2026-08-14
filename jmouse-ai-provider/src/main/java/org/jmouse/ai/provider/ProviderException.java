package org.jmouse.ai.provider;

/**
 * A call to a language model that did not come back with an answer.
 *
 * <p>One type for all of it, deliberately. Three genuinely different things go wrong on this path — the
 * provider answered with an error, the provider did not answer at all, and the address was not an
 * address — and a caller can do exactly one thing about any of them: tell somebody, in a sentence they
 * can read. Three exception types would be three catch blocks that all did that.
 *
 * <p><strong>The message is the whole product of this class.</strong> It is written to end up in a
 * toast or a transcript, so it names the provider, says what happened, and where the provider offered
 * an explanation of its own it carries that rather than the wall of JSON it arrived in.
 */
public class ProviderException extends RuntimeException {

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
