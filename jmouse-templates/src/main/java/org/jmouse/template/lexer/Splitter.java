package org.jmouse.template.lexer;

/**
 * Defines a generic interface for splitting text into tokens or other structures.
 *
 * <p>Implementations of this interface process a given text segment and return a structured result.</p>
 *
 * <pre>{@code
 * Splitter<List<RawToken>> splitter = new ExpressionSplitter();
 * List<RawToken> tokens = splitter.split("variable + 42", 0, "variable + 42".length());
 * }</pre>
 *
 * @param <R> the type of result produced by the splitter
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Splitter<R> {

    /**
     * Splits the given text into a structured format.
     *
     * @param text   the input character sequence
     * @param offset the starting position for processing
     * @param length the number of characters to process
     * @return the processed result of type {@code R}
     */
    R split(CharSequence text, int offset, int length);
}
