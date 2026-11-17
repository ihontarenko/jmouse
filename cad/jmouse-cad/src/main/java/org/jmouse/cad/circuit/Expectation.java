package org.jmouse.cad.circuit;

import org.jspecify.annotations.Nullable;
import java.util.Optional;

/**
 * What a document says it does, to be checked by running it: what holds, why — its source —, on which bench, measured
 * how, held to what.
 *
 * @param title     what it says holds, for a reader
 * @param source    why it holds — a datasheet's line, a calculation —, or empty
 * @param bench     the bench it runs on when it is not the document's own, or null
 * @param measure   what is measured
 * @param criterion what the measure is held to
 */
public record Expectation(String title, String source, @Nullable Bench bench, Measure measure, Criterion criterion) {

    public Expectation {
        source = source == null ? "" : source;
    }

    public Optional<Bench> ownBench() {
        return Optional.ofNullable(bench);
    }
}
