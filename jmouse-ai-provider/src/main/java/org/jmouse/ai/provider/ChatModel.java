package org.jmouse.ai.provider;

/**
 * A language model, as the thing calling it holds it.
 *
 * <p>Two methods, and the inversion in the first sentence is the whole design. The implementation this
 * was learned from had a port a <em>gateway service</em> used internally — {@code headers(…)},
 * {@code buildRequestBody(…)}, {@code parseResponse(…)} — which made the gateway the only possible
 * caller and every one of its consumers a client of an HTTP service. Turned around, the port is what a
 * caller holds, and a conversation loop that holds one <strong>cannot tell whether the model is across
 * the process boundary or across the internet.</strong> Local Anthropic in development, a shared
 * gateway in production, and the loop is the same code.
 *
 * <p>The request-shaping methods still exist; they are {@link HttpChatModel}'s, one layer down, where
 * only implementations see them.
 */
public interface ChatModel {

    /**
     * What this model calls itself.
     *
     * <p>Matched against {@link ProviderSettings#providerName()} so that settings meant for one
     * provider cannot be handed to another — which otherwise sends the right key to the wrong endpoint
     * and produces an authentication failure that reads as a bad credential.
     */
    String providerName();

    /**
     * One turn.
     *
     * @throws ProviderException when the provider answered with an error, did not answer at all, or was
     *                           not reachable at the address configured — see {@link ProviderException}
     *                           for why those are one type
     */
    ChatResponse converse(ChatRequest request);
}
