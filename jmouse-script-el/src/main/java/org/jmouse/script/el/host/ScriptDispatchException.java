package org.jmouse.script.el.host;

/**
 * A script failed while it was running, rather than while it was being loaded.
 *
 * <h2>⚠️ It says which handler, and which event</h2>
 *
 * <p>Everything a load can catch is caught at load, with a file, a line and a name. What is left is the
 * class of failure only running can find — a facade that threw, a property that was not there, a value
 * of the wrong shape — and those arrive from inside somebody else's code with a message about that
 * code. <em>"NullPointerException"</em> tells whoever is holding the script nothing at all.</p>
 *
 * <p>So dispatch names the two things the host knows and the stack trace does not: the event that fired
 * and the handler that was running when it broke. The original is the {@linkplain #getCause() cause},
 * untouched.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptDispatchException extends RuntimeException {

    private final String document;
    private final String event;

    /**
     * Constructs the failure.
     *
     * @param document what the script is called
     * @param event    the event that was being dispatched, or the function that was being called
     * @param where    which handler was running
     * @param cause    what actually went wrong
     */
    public ScriptDispatchException(String document, String event, String where, Throwable cause) {
        super("'%s' failed while running %s for '%s': %s".formatted(
                      document == null ? "a script" : document, where, event,
                      cause == null ? "unknown" : cause.getMessage()),
              cause);
        this.document = document;
        this.event = event;
    }

    /**
     * Returns what the script is called.
     *
     * @return the document name
     */
    public String document() {
        return document;
    }

    /**
     * Returns the event that was firing.
     *
     * @return the event name
     */
    public String event() {
        return event;
    }
}
