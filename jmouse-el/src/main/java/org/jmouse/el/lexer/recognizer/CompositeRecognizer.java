package org.jmouse.el.lexer.recognizer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.core.Sorter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Asks its recognizers in priority order, and the first one that recognizes a token answers.
 *
 * <h2>⚠️ Sorted when a recognizer is added, never while recognizing</h2>
 *
 * <p>One language instance is shared by every thread that parses through it, so {@link #recognize}
 * must only read. It used to sort the list in place on every token, and two threads tokenising at once
 * failed with a {@link java.util.ConcurrentModificationException}. Now {@link #addRecognizer} sorts a
 * copy and publishes it whole, so a reader sees either the old list or the new one, never a list being
 * rearranged. The order is the one the per-token sort produced — {@link Sorter}'s, over the same
 * recognizers.</p>
 */
public class CompositeRecognizer implements Recognizer<Token.Type, RawToken> {

    private volatile List<Recognizer<Token.Type, RawToken>> recognizers = List.of();

    public synchronized void addRecognizer(Recognizer<Token.Type, RawToken> recognizer) {
        List<Recognizer<Token.Type, RawToken>> extended = new ArrayList<>(recognizers);

        extended.add(recognizer);
        Sorter.sort(extended);

        recognizers = List.copyOf(extended);
    }

    @Override
    public Optional<Token.Type> recognize(RawToken subject) {
        for (Recognizer<Token.Type, RawToken> recognizer : recognizers) {
            Optional<Token.Type> token = recognizer.recognize(subject);

            if (token.isPresent()) {
                return token;
            }
        }

        return Optional.empty();
    }

    @Override
    public int order() {
        return 0;
    }

}
