package org.jmouse.ai;

/**
 * Turns a refusal into the sentence a model reads.
 *
 * <p>Here rather than in a transport because <strong>every</strong> transport needs the identical
 * text and each only wraps it in its own envelope — a Model Context Protocol result marked
 * {@code isError}, a conversation's tool-result block marked {@code is_error}. Two implementations of
 * one paragraph is how one of them ends up saying less than the other.
 *
 * <p>The principle is worth stating where it can be seen: <strong>a refusal is a result the model
 * reads and retries against, never a transport error that reads as "something went wrong".</strong> A
 * client told the latter reports exactly that to the user and stops; a model told <em>why</em> corrects
 * itself and calls again — which is the entire difference between a tool that works and one that
 * appears broken.
 *
 * <p>The reason is appended as a stable, machine-readable tag. It costs one short bracket in the
 * transcript and buys two things: an operator reading a conversation can bucket refusals without
 * parsing prose, and a model gets a token it can match on rather than a sentence that varies. It goes
 * at the end so the human-readable half is what is read first.
 */
public final class RefusalRendering {

    private RefusalRendering() {
    }

    /** The text a transport puts in its error envelope. */
    public static String render(ToolRefusedException refusal) {
        return refusal.getMessage() + " [refused: " + refusal.reason().name() + "]";
    }

    /**
     * The same, for work that reached the domain and stopped inside it.
     *
     * <p>Deliberately vaguer, and deliberately says the opposite of what every refusal says: a refusal
     * promises nothing was changed, and a failure cannot. Telling a model that nothing happened when
     * something may have is how a retry becomes a duplicate.
     */
    public static String renderFailure(String actionName, RuntimeException failure) {
        return "'" + actionName + "' was attempted and did not finish: "
             + (failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage())
             + ". Part of it may have been carried out — check what is there before trying again. "
             + "[failed]";
    }
}
