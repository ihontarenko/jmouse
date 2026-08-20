package org.jmouse.files.exception;

/**
 * 📏 The file is real and readable, and reading it whole is more than the caller asked to be handed.
 *
 * <h3>⚠️ A refusal, because the alternative is a truncation nobody notices</h3>
 *
 * <p>Whoever asks for a file's bytes has somewhere to put them — an agent's context window, a preview
 * buffer, a response body. Handing back the first part of an image is not a smaller answer, it is a
 * <em>wrong</em> one: it decodes to a corrupt picture, or to nothing, and the failure surfaces somewhere
 * with no idea a limit was ever involved.</p>
 *
 * <p>So the limit is stated in the message, with the real size beside it, and the caller decides what to
 * do about it.</p>
 */
public class ManagedFileTooLargeException extends RuntimeException {

    /**
     * 🏗️ Refuse to read a file whole.
     *
     * @param displayName  what the file is called
     * @param sizeBytes    how big it actually is
     * @param maximumBytes how much the caller was prepared to take
     */
    public ManagedFileTooLargeException(String displayName, long sizeBytes, long maximumBytes) {
        super("'%s' is %d bytes and the limit for reading one whole is %d. Nothing was read — a part of a file is not a smaller answer, it is a wrong one."
                      .formatted(displayName, sizeBytes, maximumBytes));
    }
}
