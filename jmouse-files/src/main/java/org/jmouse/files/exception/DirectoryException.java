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
}
