package org.jmouse.ai.conversation;

/**
 * Why the loop stopped.
 *
 * <p>Carried rather than inferred, for the same reason {@code jmouse-ai} carries a verdict: two of
 * these three mean <strong>the work is not finished</strong>, and a caller that rendered all three the
 * same way would show a half-done job as an answer.
 *
 * <p>⚠️ The sentences here are the one exception to this module holding no English. They are not a
 * prompt and not a product's vocabulary — they say what the mechanism did, and a budget that ran out
 * without saying so is the exact failure this enum exists to prevent.
 */
public enum ConversationEnding {

    /** The model said what it had to say. */
    MODEL_FINISHED("The assistant finished."),

    /**
     * It was still working and ran out of rounds.
     *
     * <p>The tool results from the last round are in the conversation and were never sent, so running
     * again with a fresh budget carries on from exactly here rather than starting over.
     */
    ROUNDS_SPENT("The assistant was still working after the maximum number of rounds and was stopped. "
               + "What it had done is in the conversation; running it again continues from there."),

    /** Same, for the limit that actually bounds the cost. */
    TOKENS_SPENT("The conversation reached its token budget and was stopped. What had been done is in "
               + "the conversation; running it again continues from there.");

    private final String sentence;

    ConversationEnding(String sentence) {
        this.sentence = sentence;
    }

    /** What to tell somebody, when what to tell them is not the model's own answer. */
    public String describe() {
        return sentence;
    }

    /** Whether the model got to the end of what it was doing. */
    public boolean finished() {
        return this == MODEL_FINISHED;
    }
}
