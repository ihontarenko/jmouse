package org.jmouse.script.el.node;

/**
 * How a {@code return} leaves the body it was written in.
 *
 * <p>⚠️ <strong>Stackless on purpose.</strong> The expensive half of an exception is capturing a stack
 * trace, and this one is not a failure — it is ordinary control flow that a behaviour's {@code tick}
 * may take on every frame. Suppression and writable stack traces are both off, which leaves an
 * allocation of a few words and no walk of the call stack.</p>
 *
 * <p>It carries the value rather than parking it in the evaluation context, because a function calling
 * a function has two returns in flight and one slot cannot hold both.</p>
 *
 * <p>⚠️ Never let one escape a script. Whoever invokes a body — a handler dispatch, a function call —
 * catches it and reads {@link #value()}; a {@code ReturnSignal} arriving in host code is a missing
 * catch, not a script error.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ReturnSignal extends RuntimeException {

    private final transient Object value;

    /**
     * Constructs the signal a {@code return} raises.
     *
     * @param value what was returned, or {@code null} for a bare {@code return}
     */
    public ReturnSignal(Object value) {
        super(null, null, false, false);
        this.value = value;
    }

    /**
     * Returns what the script returned.
     *
     * @return the value, or {@code null} for a bare {@code return}
     */
    public Object value() {
        return value;
    }
}
