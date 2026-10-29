package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.circuit.Bench;
import org.jmouse.cad.simulation.EngineName;
import org.jmouse.units.Temperatures;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The bench a circuit runs on: the figures it tells every part — its temperature, whether it is realistic, the corner of
 * its spread every part stands at, how much of the physics a run computes —, and what it does to some of them: a
 * tolerance of a part's own, an offset on a source, a part that has failed.
 *
 * @param temperature kelvin
 * @param corner      −1, 0 or +1
 * @param tolerances  a part's own spread and drift, by its reference
 * @param offsets     a source's offset, by its reference: volts on a voltage source, amperes on a current source
 * @param failures    the parts that have failed, by reference, and how, as their components say they fail
 */
public record BenchConditions(double temperature, int corner, Map<String, Tolerance> tolerances, Map<String, Double> offsets,
                              boolean realistic, Map<String, Failure> failures, Detail detail) {

    /** Where a bench stands when it names no temperature: the 25 °C datasheets give their typical values at. */
    public static final double NOMINAL_TEMPERATURE = Temperatures.kelvin(25);

    public BenchConditions {
        tolerances = Map.copyOf(tolerances);
        offsets = Map.copyOf(offsets);
        failures = Map.copyOf(failures);
    }

    /**
     * How much of the physics a run computes, and how finely: each part's equations read it as {@code detail} (0, 1, 2)
     * and decide what it changes, and the engine solves to the precision it names. Every check runs at {@link #STANDARD}.
     */
    public enum Detail {
        /** Fast and rough: the effects a document reserves for detail left out, a looser error per step. */
        DRAFT(0),
        /** What every check is held to: the documents' full physics at SPICE's own tolerances. */
        STANDARD(1),
        /** Finer: an error per step ten times tighter, and whatever a document adds at this level. */
        PRECISE(2);

        private final int figure;

        Detail(int figure) {
            this.figure = figure;
        }

        /** What a part's equations read as {@code detail}. */
        public int figure() {
            return figure;
        }
    }

    /**
     * A part's own spread and drift, given it by the bench.
     *
     * @param share       how far it may stand from its value, as a share: 0.05 for ±5 %
     * @param coefficient how far it drifts per kelvin, as a share: 100e-6 for 100 ppm/K
     */
    public record Tolerance(double share, double coefficient) {
    }

    /**
     * How a part fails, as its component writes it: {@code open} — nothing conducts any more —, or {@code short collector
     * emitter} — those pins tied together, the rest left open; a short naming no pins ties every pin together.
     *
     * @param pins the pins a short ties, by the component's names; empty for every pin, and for an open part
     */
    public record Failure(Kind kind, List<String> pins) {

        /** A part that names no way to fail fails open. */
        public static final Failure OPEN = new Failure(Kind.OPEN, List.of());

        public Failure {
            pins = pins == null ? List.of() : List.copyOf(pins);
            if (kind == Kind.OPEN && !pins.isEmpty()) {
                throw new IllegalArgumentException("An open part conducts nowhere: 'failure open' names no pins.");
            }
        }

        /** The two ways a part fails, as a component writes them. */
        public enum Kind {
            OPEN, SHORT;

            /** As a component writes it. */
            public String keyword() {
                return name().toLowerCase(Locale.ROOT);
            }

            public static Kind of(String keyword) {
                for (Kind kind : values()) {
                    if (kind.keyword().equals(keyword)) {
                        return kind;
                    }
                }
                throw new IllegalArgumentException("A part fails 'open' or 'short', not '" + keyword + "'.");
            }
        }

        /** A failure as a component's words say it: {@code open}, {@code short}, {@code short drain source}. */
        public static Failure of(List<String> words) {
            return new Failure(Kind.of(words.getFirst()), words.subList(1, words.size()));
        }

        public boolean shorts() {
            return kind == Kind.SHORT;
        }
    }

    /** The bench nothing is said of: ideal, at the nominal temperature, every part at the middle of its spread. */
    public static BenchConditions nominal() {
        return new BenchConditions(NOMINAL_TEMPERATURE, 0, Map.of(), Map.of(), false, Map.of(), Detail.STANDARD);
    }

    /** The bench a document writes — its temperature, whether it is realistic —, the rest nominal. */
    public static BenchConditions of(Bench bench) {
        double temperature = bench.temperature() == null ? NOMINAL_TEMPERATURE : bench.temperature();
        return new BenchConditions(temperature, 0, Map.of(), Map.of(), bench.realistic(), Map.of(), Detail.STANDARD);
    }

    /** These conditions with one more part failed. */
    public BenchConditions withFailure(String part, Failure failure) {
        Map<String, Failure> all = new LinkedHashMap<>(failures);
        all.put(part, failure);
        return new BenchConditions(temperature, corner, tolerances, offsets, realistic, all, detail);
    }

    /** Each figure the bench tells every part, by the name a document reads it with. */
    public Map<String, Double> figures() {
        Map<String, Double> figures = new LinkedHashMap<>();
        figures.put(EngineName.TEMPERATURE.written(), temperature);
        figures.put(EngineName.REALISTIC.written(), realistic ? 1.0 : 0.0);
        figures.put(EngineName.TOLERANCE_CORNER.written(), (double) corner);
        figures.put(EngineName.DETAIL.written(), (double) detail.figure());
        return figures;
    }
}
