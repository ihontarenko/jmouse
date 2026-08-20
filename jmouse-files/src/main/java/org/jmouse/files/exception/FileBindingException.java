package org.jmouse.files.exception;

/**
 * 🔗 A binding that could not be made, or could not be read as one.
 *
 * <p>Raised before anything is written, so a refused binding leaves no half-filed row behind.</p>
 */
public class FileBindingException extends RuntimeException {

    /**
     * 🏗️ Report a binding refusal.
     *
     * @param message what was wrong with it
     */
    public FileBindingException(String message) {
        super(message);
    }
}
