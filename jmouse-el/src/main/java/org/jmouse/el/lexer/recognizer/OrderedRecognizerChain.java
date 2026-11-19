package org.jmouse.el.lexer.recognizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jmouse.core.Sorter;

/** Ordered recognition shared by raw-token classification and source dialect rules. */
public class OrderedRecognizerChain<R, S> implements Recognizer<R, S> {
    private volatile List<Recognizer<R, S>> recognizers = List.of();

    public OrderedRecognizerChain() {
    }

    public OrderedRecognizerChain(List<? extends Recognizer<R, S>> recognizers) {
        List<Recognizer<R, S>> ordered = new ArrayList<>(recognizers);
        Sorter.sort(ordered);
        this.recognizers = List.copyOf(ordered);
    }

    public synchronized void addRecognizer(Recognizer<R, S> recognizer) {
        List<Recognizer<R, S>> ordered = new ArrayList<>(recognizers);
        ordered.add(Objects.requireNonNull(recognizer, "recognizer"));
        Sorter.sort(ordered);
        recognizers = List.copyOf(ordered);
    }

    @Override
    public Optional<R> recognize(S subject) {
        return first(recognizers, subject);
    }

    /** Captures the current order and registrations; subsequent registration cannot affect this view. */
    public Recognizer<R, S> snapshot() {
        List<Recognizer<R, S>> snapshot = recognizers;
        int priority = order();
        return new Recognizer<>() {
            @Override
            public Optional<R> recognize(S subject) {
                return first(snapshot, subject);
            }

            @Override
            public int order() {
                return priority;
            }
        };
    }

    @Override
    public int order() {
        return 0;
    }

    private static <R, S> Optional<R> first(List<Recognizer<R, S>> recognizers, S subject) {
        for (Recognizer<R, S> recognizer : recognizers) {
            Optional<R> recognized = Objects.requireNonNull(recognizer.recognize(subject), "recognition result");
            if (recognized.isPresent()) {
                return recognized;
            }
        }
        return Optional.empty();
    }
}
