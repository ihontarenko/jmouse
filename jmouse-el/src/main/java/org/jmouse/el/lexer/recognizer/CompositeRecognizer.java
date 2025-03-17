package org.jmouse.el.lexer.recognizer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.util.Sorter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class CompositeRecognizer implements Recognizer<Token.Type, RawToken> {

    private final List<Recognizer<Token.Type, RawToken>> recognizers = new ArrayList<>();

    public void addRecognizer(Recognizer<Token.Type, RawToken> recognizer) {
        recognizers.add(recognizer);
    }

    @Override
    public Optional<Token.Type> recognize(RawToken subject) {
        Sorter.sort(this.recognizers);

        Optional<Token.Type>                       token    = Optional.empty();
        Iterator<Recognizer<Token.Type, RawToken>> iterator = recognizers.iterator();

        while (iterator.hasNext() && token.isEmpty()) {
            token = iterator.next().recognize(subject);
        }

        return token;
    }

    @Override
    public int getOrder() {
        return 0;
    }

}