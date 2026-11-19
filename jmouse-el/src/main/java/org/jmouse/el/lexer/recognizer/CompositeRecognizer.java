package org.jmouse.el.lexer.recognizer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Token;

/** The existing raw-token classifier specialization of ordered recognition. */
public class CompositeRecognizer extends OrderedRecognizerChain<Token.Type, RawToken> {
}
