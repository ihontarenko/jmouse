package org.jmouse.parallax.event;

/** A consistency-check run finished and its counts are stored. */
public record EvaluationCompleted(String runId, String actorId, int passed, int warned, int failed) {
}
