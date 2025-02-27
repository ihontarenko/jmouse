package org.jmouse.template.lexer.recognizer;

import org.jmouse.template.lexer.Token;
import org.jmouse.util.Sorter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class CompositeRecognizer implements Recognizer<Token, String> {

    private final List<Recognizer<Token, String>> recognizers = new ArrayList<>();

    public void addRecognizer(Recognizer<Token, String> recognizer) {
        recognizers.add(recognizer);
    }

    @Override
    public Optional<Token> recognize(String subject) {
        Sorter.sort(this.recognizers);

        Optional<Token>                     token    = Optional.empty();
        Iterator<Recognizer<Token, String>> iterator = recognizers.iterator();

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