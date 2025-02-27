package org.jmouse.template.lexer;

import java.util.List;

/**
 * Defines a tokenizer that splits input text into a sequence of {@link Token} instances.
 *
 * <p>Implementations of this interface process a given text and generate a structured list of tokens.</p>
 *
 * <pre>{@code
 * Tokenizer tokenizer = new MyTokenizerImplementation();
 * List<Token> tokens = tokenizer.tokenize("var x = 42;");
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Tokenizer<T, S extends CharSequence> {

    /**
     * Tokenizes the given text into a list of {@link Token} instances.
     *
     * @param text the input character sequence to tokenize
     * @return a list of extracted tokens
     */
    List<T> tokenize(S text);
}
