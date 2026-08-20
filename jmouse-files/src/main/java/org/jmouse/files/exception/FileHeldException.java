package org.jmouse.files.exception;

/**
 * 🔒 Something in the product is holding this file, so it may not stop being reachable.
 *
 * <h3>⚠️ Not an authorization failure, and the distinction is the whole of it</h3>
 *
 * <p>The caller owns the file and holds every permission over it. Nothing is being withheld — the
 * answer is that the product is currently <em>using</em> it, which is a state they can change rather
 * than an authority they lack. A product mapping this to 403 would send whoever reads the refusal off
 * to look at their permissions; 409 sends them to look at whatever is displaying the file.</p>
 *
 * <p>The message carries the holder's own sentence, so the refusal names the real obstacle instead of
 * "something".</p>
 */
public class FileHeldException extends RuntimeException {

    /**
     * 🏗️ Refuse an act that would make a held file unreachable.
     *
     * @param displayName      what the file is called
     * @param attemptedAction  what was being attempted, as a past participle — {@code deleted}
     * @param heldReason       why it is held, in the holder's words
     */
    public FileHeldException(String displayName, String attemptedAction, String heldReason) {
        super("'%s' cannot be %s — %s".formatted(displayName, attemptedAction, heldReason));
    }
}
