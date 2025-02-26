package org.jmouse.template.lexer.recognizer;

import org.jmouse.util.Ordered;

import java.util.Optional;

/**
 * Represents a generic recognizer that attempts to identify a result from a given subject.
 *
 * <p>Implementations of this interface provide recognition logic that either returns a result
 * wrapped in an {@link Optional} or an empty result if recognition fails.</p>
 *
 * <pre>{@code
 * Recognizer<String, String> recognizer = subject -> subject.startsWith("Hello")
 *         ? Optional.of("Greeting recognized") : Optional.empty();
 *
 * Optional<String> result = recognizer.recognize("Hello, world!");
 * result.ifPresent(System.out::println);
 * }</pre>
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
