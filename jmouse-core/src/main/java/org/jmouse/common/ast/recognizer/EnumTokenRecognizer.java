package org.jmouse.common.ast.recognizer;

import org.jmouse.common.ast.finder.EnumFinder;
import org.jmouse.common.ast.token.Token;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class EnumTokenRecognizer<T extends Enum<T> & Token> extends EnumFinder<T> implements Recognizer<Token, String> {

    private final int priority;
    private final T[] tokens;

    public EnumTokenRecognizer(T[] tokens, int priority) {
        this.priority = priority;
        this.tokens = tokens;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public Optional<Token> recognize(String piece) {
        return ofNullable(find(piece, tokens).orElse(null));
    }

}
