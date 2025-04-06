package org.jmouse.el.core.lexer;

import java.util.List;

/**
 * Defines a tokenizer that splits input text into a sequence of {@link Token.Type} instances.
 *
 * <p>Implementations of this interface process a given text and generate a structured list of tokens.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Tokenizer<S extends CharSequence, T> {

    List<T> tokenize(S text);

}
