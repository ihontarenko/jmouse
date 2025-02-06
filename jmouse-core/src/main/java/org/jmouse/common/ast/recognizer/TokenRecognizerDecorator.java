package org.jmouse.common.ast.recognizer;

import org.jmouse.common.ast.PriorityComparator;
import org.jmouse.common.ast.token.Token;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class TokenRecognizerDecorator extends AbstractRecognizer<Token, String> {

    private static final int PRIORITY = 2500;

    private final List<Recognizer<Token, String>> recognizers = new ArrayList<>();

    public TokenRecognizerDecorator(final int priority) {
        super(priority);
    }

    public TokenRecognizerDecorator() {
        this(PRIORITY);
    }

    public void addRecognizer(Recognizer<Token, String> recognizer) {
        this.recognizers.add(recognizer);
    }

    @Override
    public Optional<Token> recognize(String piece) {
        this.recognizers.sort(new PriorityComparator<>());

        Optional<Token>                     token    = Optional.empty();
        Iterator<Recognizer<Token, String>> iterator = recognizers.iterator();

        while (iterator.hasNext() && token.isEmpty()) {
            token = iterator.next().recognize(piece);
        }

        return token;
    }

}
