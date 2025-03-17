package org.jmouse.el.lexer;

/**
 * Defines a generic interface for splitting text into tokens or other structures.
 *
 * <p>Implementations of this interface process a given text segment and return a structured result.</p>
 *
 * @param <R> the type of result produced by the splitter
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Splitter<R, S extends CharSequence> {

    /**
     * Splits the given text into a structured format.
     *
     * @param text   the input character sequence
     * @param offset the starting offset for processing
     * @param length the number of characters to process
     * @return the processed result of type {@code R}
     */
    R split(S text, int offset, int length);
}
