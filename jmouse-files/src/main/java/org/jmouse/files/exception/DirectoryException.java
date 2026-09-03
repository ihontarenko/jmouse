package org.jmouse.files.exception;

/**
 * 🌳 Something about the directory tree could not be done.
 *
 * <p>A name that will not fit, a move that would put a directory inside itself, a depth beyond what
 * the installation allows, a root asked to be renamed. Every one of them is a refusal rather than a
 * failure — the tree is left exactly as it was.</p>
 */
public class DirectoryException extends RuntimeException {

    /**
     * 🏗️ Report a refusal.
     *
     * @param message what was wrong
     */
    public DirectoryException(String message) {
        super(message);
    }

    /**
     * 🏗️ Report a refusal caused by something underneath.
     *
     * <p>For the one case where the reason is not the tree's own: a configuration document that will
     * not bind into the record its kind declares. The cause carries the parser's sentence, which is the
     * half that says <em>which field</em>.</p>
     *
     * @param message what was wrong
     * @param cause   what said so
     */
    public DirectoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
