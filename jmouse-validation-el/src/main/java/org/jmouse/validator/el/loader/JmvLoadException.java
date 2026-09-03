package org.jmouse.validator.el.loader;

/**
 * A set of {@code .jmv} documents that cannot be loaded, said in a way somebody can act on. 🚫
 *
 * <p>⚠️ Every message names the <strong>file</strong>. A load walks several locations and may read
 * dozens of documents; "could not be compiled" without a path is a sentence that sends a reader to
 * open all of them.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JmvLoadException extends RuntimeException {

    public JmvLoadException(String message) {
        super(message);
    }

    /**
     * @param message what is wrong, naming the file
     * @param cause   ⚠️ kept, not folded away. The message says what is wrong with a document; the
     *                cause is what says where in the reader or the compiler that was decided, and it
     *                is the only half a library author can use.
     */
    public JmvLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
