package org.jmouse.ai.conversation;

import org.jmouse.ai.provider.TokenUsage;

/**
 * How far a conversation is allowed to run before somebody looks at it.
 *
 * <p><strong>Two numbers, because rounds are only half of what runs away.</strong> A round cap is the
 * obvious one and it bounds the wrong quantity: six rounds of a conversation that has grown to eighty
 * thousand tokens costs more than sixty rounds of a short one, and the whole conversation is resent
 * every round, so the cost of a round grows with every round taken. The token count already arrives on
 * every response and the implementation this was learned from was not adding it up.
 *
 * <p>A budget is a value rather than a constant in a service, because <strong>what counts as too far
 * depends on the conversation</strong>: an assistant answering a question and one working through a
 * hundred-item import want different answers, and neither should need a release to say so.
 *
 * @param maximumRounds the most times the model may be asked; one round is one question and its tools
 * @param maximumTokens the most a whole conversation may spend, input and output together
 */
public record ConversationBudget(int maximumRounds, int maximumTokens) {

    /**
     * Enough for real work, obviously not enough for a loop.
     *
     * <p>Six rounds is what the implementation this was learned from settled on and is a reasonable
     * ceiling on a model that is making progress. The token figure is deliberately the one that fires
     * first on a conversation that is growing rather than progressing.
     */
    public static ConversationBudget defaults() {
        return new ConversationBudget(6, 120_000);
    }

    public ConversationBudget withMaximumRounds(int maximumRounds) {
        return new ConversationBudget(maximumRounds, maximumTokens);
    }

    public ConversationBudget withMaximumTokens(int maximumTokens) {
        return new ConversationBudget(maximumRounds, maximumTokens);
    }

    /**
     * Which limit has been reached, or null while there is room for another round.
     *
     * <p>Answers <em>which</em> rather than <em>whether</em> on purpose: a conversation that stopped is
     * about to tell somebody why, and "it stopped" is the one answer nobody can act on.
     */
    public ConversationEnding reachedAfter(int rounds, TokenUsage spent) {
        if (rounds >= maximumRounds) {
            return ConversationEnding.ROUNDS_SPENT;
        }

        if (spent.totalTokens() >= maximumTokens) {
            return ConversationEnding.TOKENS_SPENT;
        }

        return null;
    }
}
