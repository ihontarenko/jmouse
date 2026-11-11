package org.jmouse.cad.circuit;

import java.util.Optional;

/**
 * What a document says it does, to be checked by running it: what holds, why — its source —, on which bench, measured
 * how, held to what.
 *
 * @param source why it holds — a datasheet's line, a calculation —, or empty
 * @param bench  the bench it runs on when it is not the document's own, or null
 */
public record Expectation(String title, String source, Bench bench, Measure measure, Criterion criterion) {

    public Expectation {
        source = source == null ? "" : source;
    }

    public Optional<Bench> ownBench() {
        return Optional.ofNullable(bench);
    }
}
