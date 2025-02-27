package org.jmouse.template.lexer.recognizer;

import org.jmouse.template.lexer.Token;
import org.jmouse.util.Sorter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class CompositeRecognizer implements Recognizer<Token.Type, String> {

    private final List<Recognizer<Token.Type, String>> recognizers = new ArrayList<>();

    public void addRecognizer(Recognizer<Token.Type, String> recognizer) {
        recognizers.add(recognizer);
    }

    @Override
    public Optional<Token.Type> recognize(String subject) {
        Sorter.sort(this.recognizers);

        Optional<Token.Type>                     token    = Optional.empty();
        Iterator<Recognizer<Token.Type, String>> iterator = recognizers.iterator();

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