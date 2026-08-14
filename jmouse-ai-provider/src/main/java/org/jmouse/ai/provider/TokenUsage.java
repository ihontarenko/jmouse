package org.jmouse.ai.provider;

/**
 * What one turn cost.
 *
 * <p>Carried on every response rather than logged and discarded, because a tool-calling loop sends the
 * whole conversation again on every round — the input count grows with each turn, and a loop that ran
 * away is visible in this number long before it is visible in an invoice.
 *
 * @param inputTokens  what was sent, including the entire conversation so far
 * @param outputTokens what came back
 */
public record TokenUsage(int inputTokens, int outputTokens) {

    /** What a provider that said nothing about it is recorded as. */
    public static TokenUsage none() {
        return new TokenUsage(0, 0);
    }

    public int totalTokens() {
        return inputTokens + outputTokens;
    }

    /** The running total across a conversation's rounds. */
    public TokenUsage plus(TokenUsage other) {
        return new TokenUsage(inputTokens + other.inputTokens(), outputTokens + other.outputTokens());
    }
}
