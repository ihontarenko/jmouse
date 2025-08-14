package org.jmouse.el.lexer.recognizer;

import org.jmouse.core.Ordered;

import java.util.Optional;

/**
 * Represents a generic recognizer that attempts to identify a result from a given subject.
 *
 * @param <R> the type of the recognition result
 * @param <T> the type of the subject being recognized
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Recognizer<R, T> extends Ordered {

    /**
     * Attempts to recognize a result from the given subject.
     *
     * @param subject the input to be analyzed
     * @return an {@link Optional} containing the recognized result if successful, otherwise empty
     */
    Optional<R> recognize(T subject);
}
