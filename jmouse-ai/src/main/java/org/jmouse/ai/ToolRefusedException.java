package org.jmouse.ai;

/**
 * A refusal a model is meant to read and act on.
 *
 * <p>Every message carried by one of these is written for a caller that will try again: it says what
 * was wrong <em>and what would have been accepted</em>. A refusal that only says "no" costs a second
 * wrong call, and a model that receives three of them in a row starts inventing.
 *
 * <p><strong>A refusal is not a failure.</strong> A refusal is a decision, taken before anything was
 * attempted; a failure is work that reached the domain and stopped inside it, possibly having done
 * part of what it was asked. They are recorded through different methods of
 * {@link org.jmouse.ai.spi.InvocationTrace} on purpose, because only one of the two is ever a reason
 * to go and look at the data.
 *
 * <p>Unchecked, because every path that can raise one already ends at a transport that has to render
 * it — see {@link RefusalRendering} — and a checked exception would only be caught and rethrown at
 * every layer between.
 */
public class ToolRefusedException extends RuntimeException {

    private final RefusalReason reason;

    public ToolRefusedException(RefusalReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    /** What kind of refusal this is, for counting. The message is what is read. */
    public RefusalReason reason() {
        return reason;
    }
}
