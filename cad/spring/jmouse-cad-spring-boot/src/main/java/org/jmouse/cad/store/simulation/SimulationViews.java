package org.jmouse.cad.store.simulation;

import org.jmouse.cad.simulation.engine.LogicLevel;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * What a simulation takes and answers — the shapes {@code JMOUSE_CAD_API.md} §3.9 and §3.10 write out with examples.
 */
public final class SimulationViews {

    private SimulationViews() {
    }

    /**
     * A circuit to run.
     *
     * @param circuit    a stored circuit, by slug or {@code slug@revision}; or null when {@code source} is sent
     * @param source     an unsaved circuit's text — the bench's edits —; or null
     * @param owner      whose documents the circuit is found among besides the catalogue, as {@code TYPE:id}; or null
     * @param parameters other values for the circuit's sliders, or a part's setting as {@code part.setting}, each as
     *                   written: {@code "U_in": "12 V"}, {@code "S1.closed": "1"}
     * @param analysis   {@code transient} or {@code operating}; null for the circuit's own {@code analysis} line
     * @param duration   seconds to run a transient; null for the analysis line's
     * @param samples    how many samples a transient is read at
     * @param everyPart  record every part's current and power, not only what has a current of its own; null is no
     */
    public record SimulationRequest(String circuit, String source, String owner, Map<String, String> parameters,
                                    String analysis, Double duration, Integer samples, Boolean everyPart) {

        /** Whether every part is recorded: a field left out is not asked for. */
        public boolean recordsEveryPart() {
            return Boolean.TRUE.equals(everyPart);
        }
    }

    /** What a simulation answers: a run finished now, or a job to fetch it from later. */
    public sealed interface SimulationAnswer permits SimulationResult, Job {
    }

    /** A finished run; the fields of the other analysis are empty. */
    public sealed interface SimulationResult extends SimulationAnswer permits Transient, OperatingPoint {
    }

    /**
     * A transient: every sample's time, and by node or part what was read at it.
     *
     * @param lows  each node's lowest voltage between one sample and the next, what a peak-detecting screen draws
     * @param highs each node's highest
     */
    public record Transient(String analysis, String circuit, double[] time, Map<String, double[]> voltages,
                            Map<String, double[]> lows, Map<String, double[]> highs, Map<String, double[]> currents,
                            Map<String, double[]> powers, List<Map<String, LogicLevel>> levels) implements SimulationResult {
    }

    /** A circuit at rest: every node's voltage. */
    public record OperatingPoint(String analysis, String circuit, Map<String, Double> voltages) implements SimulationResult {
    }

    /** Where a queued run stands. */
    public enum JobState { QUEUED, RUNNING, DONE, FAILED, CANCELLED }

    /**
     * A run too large to answer at once.
     *
     * @param progress from 0 to 1
     * @param failure  why it failed, or null
     */
    public record Job(String id, JobState state, double progress, Instant queuedAt, Instant startedAt, Instant finishedAt,
                      String failure) implements SimulationAnswer {
    }

    /**
     * Every expectation a document writes, measured.
     *
     * @param measured what was measured, as the engine writes it — a value or a row —; null when it was refused
     */
    public record Verdict(String title, String source, String measured, String window, boolean met, String refusal) {
    }

    /**
     * One catalogue document's expectations, measured.
     *
     * @param refusal why its model could not be measured at all; null when it was
     */
    public record DocumentVerdicts(String slug, String title, String kind, List<Verdict> verdicts, String refusal) {
    }

    /** A document whose expectations are checked: stored, or its text. */
    public record ExpectationRequest(String reference, String source, String owner) {
    }
}
