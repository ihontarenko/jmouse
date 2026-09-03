package org.jmouse.script.el.host;

/**
 * A script reached for something outside the catalogue at evaluation time.
 *
 * <p>⚠️ <strong>Seeing one is a bug, not a script error.</strong> Every name a script writes is checked
 * when the file is loaded, so a refusal here means a tree was evaluated that never went through the
 * binder — a hand-assembled context, a test, or a caller that skipped a step. The message says which
 * name, because the answer is always to look at how the script was loaded rather than at the script.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptAccessException extends RuntimeException {

    /**
     * Constructs the refusal.
     *
     * @param message what was reached for, and what was available instead
     */
    public ScriptAccessException(String message) {
        super(message);
    }
}
