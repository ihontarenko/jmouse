package org.jmouse.el.lexer;

import java.util.List;

/**
 * Defines a tokenizer that splits input text into a sequence of {@link Token.Type} instances.
 *
 * <p>Implementations of this interface process a given text and generate a structured list of tokens.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Tokenizer<T, S extends CharSequence> {

    /**
     * Tokenizes the given text into a list of {@link Token.Type} instances.
     *
     * @param text the input character sequence to tokenize
     * @return a list of extracted tokens
     */
    List<T> tokenize(S text);
}
