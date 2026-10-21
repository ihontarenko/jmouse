package org.jmouse.parallax.circuit;

import org.jmouse.formula.Complex;
import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The representations of a lumped circuit, each an ordinary dataset: a circuit is computed like any other object.
 *
 * <p>What is computed is a {@link CircuitEngine}'s: each batch analysis takes the setting {@code engine} — this
 * library's own, or SPICE where the host supplies it — and the representation here is only what is asked and how it is
 * shown. The phasor diagram is the library's alone: SPICE has no such analysis.</p>
 */
public final class CircuitComputations {

    /** The unit of a digital node's column: 1, 0, −1 for unknown, −2 released — a viewer draws it as a timing lane. */
    public static final String LEVEL_UNIT = "level";

    /** The setting that chooses the engine of a batch analysis. */
    public static final String ENGINE = "engine";

    private CircuitComputations() {
    }

    private static SettingSpecification engineSetting() {
        return SettingSpecification.choice(ENGINE, CircuitEngineKind.PARALLAX,
                "What computes it: this library's own engine, or SPICE where the server has a simulator");
    }

    /** The engine a computation's settings choose, among those the host offers. */
    private static CircuitEngine engineOf(CircuitEngines engines, ComputationSettings settings) {
        return engines.engine(settings.choice(ENGINE, CircuitEngineKind.class));
    }

    private static CircuitEngine.FrequencyRange frequencyRange(ComputationSettings settings) {
        return new CircuitEngine.FrequencyRange(settings.number("from"), settings.number("to"), settings.integer("points"));
    }

    /**
     * Every node voltage, every own-current branch and every diode current from rest over the circuit's time span —
     * or, with {@code levels}, the digital nodes' logic levels from the same run: 1, 0, −1 for unknown, −2 released.
     */
    public static class TransientResponse implements Computation {

        /** The divisions across the bench's oscilloscope screen: its time base times this is what it shows. */
        private static final int SCREEN_DIVISIONS = 10;

        private final CircuitEngines engines;

        public TransientResponse() {
            this(CircuitEngines.parallaxOnly());
        }

        public TransientResponse(CircuitEngines engines) {
            this.engines = engines;
        }

        public RepresentationKind kind() {
            return RepresentationKind.TRANSIENT_RESPONSE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof Circuit;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("samples", 1000, 50, 100_000, "Time steps"),
                    SettingSpecification.flag("parts", false, "Every part's current too, for a sheet that shows them"),
                    SettingSpecification.flag("levels", false, "The digital nodes' logic levels instead of the analog columns"),
                    SettingSpecification.vector("window", List.of(), 2, 0, 1e6,
                            "From and to, seconds: only this stretch, sampled finely — a zoom into an edge; none is the whole run"),
                    SettingSpecification.flag("screen", false,
                            "What the bench's oscilloscope opens on: the probed voltages over the last screen of its time base"),
                    engineSetting());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            long size = ((Circuit) object.definition()).elements().size();
            return settings.integer("samples") * size * size;
        }

        /**
         * The engine's transient; with {@code screen}, the run's last screen of the circuit's time base, read as the
         * circuit's probes read it.
         */
        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            Circuit circuit = (Circuit) object.definition();
            boolean levels = settings.flag("levels");
            boolean screen = settings.flag("screen") && !levels;
            double[] window = settings.vectorOr("window", null);
            if (screen && window == null) {
                window = lastScreen(circuit, circuit.time().maximum());
            }
            CircuitEngine.TransientRequest request = new CircuitEngine.TransientRequest(settings.integer("samples"),
                    settings.flag("parts"), levels, window);
            Dataset run = engineOf(engines, settings).transientResponse(object, request, progress);
            return screen ? probedVoltages(circuit, (Dataset.TimeSeries) run) : run;
        }

        /**
         * The last screen of the circuit's time base, from and to — what the bench's oscilloscope shows once the run
         * has gone on —; null for the whole run when the circuit names no time base or the run is shorter than a screen.
         */
        private static double[] lastScreen(Circuit circuit, double duration) {
            Double perDivision = circuit.timePerDivision();
            if (perDivision == null || perDivision * SCREEN_DIVISIONS >= duration) {
                return null;
            }
            return new double[]{duration - perDivision * SCREEN_DIVISIONS, duration};
        }

        /**
         * The voltages the circuit's probes read, each named as its channel reads it — `out`, `a - b` against a
         * reference —, in the probes' order; every node when it probes none. A switch node probed among them is one
         * trace of the screen, not the solid band every node of a switcher drawn over its whole run made.
         */
        private static Dataset.TimeSeries probedVoltages(Circuit circuit, Dataset.TimeSeries run) {
            List<String> columns = new ArrayList<>();
            List<float[]> values = new ArrayList<>();
            for (ObjectDefinition.BenchProbe probe : circuit.probes()) {
                if (!probe.measure().equals("voltage") || probe.target() == null) {
                    continue;
                }
                float[] voltages = voltageOf(run, probe.target());
                boolean against = probe.reference() != null && !probe.reference().equals(Circuit.GROUND);
                if (against) {
                    float[] reference = voltageOf(run, probe.reference());
                    float[] difference = new float[voltages.length];
                    for (int sample = 0; sample < difference.length; sample++) {
                        difference[sample] = voltages[sample] - reference[sample];
                    }
                    voltages = difference;
                }
                columns.add(against ? probe.target() + " - " + probe.reference() : probe.target());
                values.add(voltages);
            }
            if (columns.isEmpty()) {
                for (int column = 0; column < run.columns().size(); column++) {
                    if (ColumnNames.VOLTS.equals(run.units().get(column))) {
                        columns.add(run.columns().get(column));
                        values.add(run.values().get(column));
                    }
                }
            }
            return new Dataset.TimeSeries("t", run.axisValues(), columns, values,
                    Collections.nCopies(columns.size(), ColumnNames.VOLTS));
        }

        /** A node's voltage in a run: its column, or zeros for ground. */
        private static float[] voltageOf(Dataset.TimeSeries run, String node) {
            if (node.equals(Circuit.GROUND)) {
                return new float[run.axisValues().length];
            }
            for (int column = 0; column < run.columns().size(); column++) {
                if (run.columns().get(column).equals(node) && ColumnNames.VOLTS.equals(run.units().get(column))) {
                    return run.values().get(column);
                }
            }
            throw new IllegalArgumentException("A probe reads '" + node + "', which is no node of the circuit.");
        }
    }

    /**
     * The noise every node carries, V/√Hz over frequency: each device's noise sources at the operating point driven
     * through the small-signal circuit one at a time, their powers added. SPICE's {@code .noise}, for every node at once.
     */
    public static class NoiseResponse implements Computation {

        private final CircuitEngines engines;

        public NoiseResponse() {
            this(CircuitEngines.parallaxOnly());
        }

        public NoiseResponse(CircuitEngines engines) {
            this.engines = engines;
        }

        public RepresentationKind kind() {
            return RepresentationKind.NOISE_RESPONSE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof Circuit;
        }

        public List<SettingSpecification> settings() {
            return List.of(
                    SettingSpecification.number("from", 10, 1e-3, 1e9, "Lowest frequency, Hz"),
                    SettingSpecification.number("to", 1e6, 1e-3, 1e9, "Highest frequency, Hz"),
                    SettingSpecification.integer("points", 100, 10, 2000, "Frequencies"),
                    engineSetting());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            long size = ((Circuit) object.definition()).elements().size();
            return settings.integer("points") * size * size * size * size;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            return engineOf(engines, settings).noiseResponse(object, frequencyRange(settings), progress);
        }
    }

    /** Bode curves: the gain in decibels and the phase in degrees of every node, over log-spaced frequencies. */
    public static class FrequencyResponse implements Computation {

        private final CircuitEngines engines;

        public FrequencyResponse() {
            this(CircuitEngines.parallaxOnly());
        }

        public FrequencyResponse(CircuitEngines engines) {
            this.engines = engines;
        }

        public RepresentationKind kind() {
            return RepresentationKind.FREQUENCY_RESPONSE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof Circuit;
        }

        public List<SettingSpecification> settings() {
            return List.of(
                    SettingSpecification.number("from", 1, 1e-3, 1e9, "Lowest frequency, Hz"),
                    SettingSpecification.number("to", 1e6, 1e-3, 1e9, "Highest frequency, Hz"),
                    SettingSpecification.integer("points", 200, 10, 5000, "Frequencies"),
                    engineSetting());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            long size = ((Circuit) object.definition()).elements().size();
            return settings.integer("points") * size * size * size;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            return engineOf(engines, settings).frequencyResponse(object, frequencyRange(settings), progress);
        }
    }

    /**
     * Every two-terminal part's voltage and current at one frequency, as phasors: the steady state a sine of that frequency
     * settles into, the circuit linearized at its operating point as the frequency response is. Each part is a feature
     * named by its id; its facts are the voltage across it (its first terminal against its second) and the current into
     * its first terminal, each as a real and an imaginary part, every source excited at its own value with phase 0 —
     * so the angles between them are the circuit's, whatever the source's amplitude. What a sheet turns into a dial by
     * every part: how far its current lags its voltage, and how large each is against the others. This library's engine
     * alone computes it: SPICE has no analysis that reports a part's phasors.
     */
    public static class PhasorDiagram implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.PHASOR_DIAGRAM;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof Circuit;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.number("frequency", 1000, 1e-3, 1e9, "Frequency, Hz"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            long size = ((Circuit) object.definition()).elements().size();
            return size * size * size;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            double frequency = settings.number("frequency");
            if (!(frequency > 0)) {
                throw new IllegalArgumentException("A phasor diagram is taken at a frequency above zero.");
            }
            Netlist netlist = Netlist.of(object);
            CircuitSolver solver = new CircuitSolver(netlist);
            double[] operatingPoint = ParallaxEngine.smallSignalBias(solver, netlist);
            Complex[] solution = solver.phasors(frequency, operatingPoint);
            double omega = 2 * Math.PI * frequency;
            List<Dataset.Feature> features = new ArrayList<>();
            // Every part says the phasor across it and through it, from the admittance it stamped — a generator and a
            // model alike.
            for (Device device : solver.devices()) {
                if (device.part() == null) {
                    continue;
                }
                Complex voltage = device.phasorVoltage(solution);
                Complex current = device.phasorCurrent(solution, omega, operatingPoint);
                if (voltage == null) {
                    continue;
                }
                if (current == null || !voltage.isFinite() || !current.isFinite()) {
                    continue;
                }
                Map<String, Object> facts = new LinkedHashMap<>();
                facts.put("voltage_real", voltage.real());
                facts.put("voltage_imaginary", voltage.imaginary());
                facts.put("current_real", current.real());
                facts.put("current_imaginary", current.imaginary());
                facts.put("frequency", frequency);
                facts.put("source", device.source());
                features.add(new Dataset.Feature(new double[]{voltage.real(), voltage.imaginary()}, voltage.modulus(), device.part(), facts));
            }
            return new Dataset.FeaturePoints(features);
        }
    }

    /** The DC state: node voltages, every part's currents and the power every part takes. */
    public static class OperatingPoint implements Computation {

        private final CircuitEngines engines;

        public OperatingPoint() {
            this(CircuitEngines.parallaxOnly());
        }

        public OperatingPoint(CircuitEngines engines) {
            this.engines = engines;
        }

        public RepresentationKind kind() {
            return RepresentationKind.OPERATING_POINT;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof Circuit;
        }

        public List<SettingSpecification> settings() {
            return List.of(engineSetting());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            long size = ((Circuit) object.definition()).elements().size();
            return size * size * size;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            return engineOf(engines, settings).operatingPoint(object);
        }
    }

    /**
     * The operating point as one source's value is swept: transfer curves and current–voltage characteristics. Each
     * point starts Newton from the one before, which is how a sweep through a diode's knee stays on its curve.
     */
    public static class DirectCurrentSweep implements Computation {

        private final CircuitEngines engines;

        public DirectCurrentSweep() {
            this(CircuitEngines.parallaxOnly());
        }

        public DirectCurrentSweep(CircuitEngines engines) {
            this.engines = engines;
        }

        public RepresentationKind kind() {
            return RepresentationKind.DC_SWEEP;
        }

        public boolean accepts(ObjectDefinition definition) {
            // Any circuit: a parameter can always be swept, and a source is looked for in the circuit as built — a
            // generator or a supply is a component before it is expanded, and asking for a primitive here refused a
            // DC sweep to nearly every circuit of the catalogue.
            return definition instanceof Circuit;
        }

        public List<SettingSpecification> settings() {
            return List.of(
                    SettingSpecification.text("source", "", 32, "The source to sweep; empty for the first one"),
                    SettingSpecification.text("parameter", "", 32, "A parameter of the circuit to sweep instead — a wiper's position, a resistance"),
                    SettingSpecification.number("from", 0, -1e6, 1e6, "First value, V or A"),
                    SettingSpecification.number("to", 5, -1e6, 1e6, "Last value, V or A"),
                    SettingSpecification.integer("points", 101, 3, 5000, "Points"),
                    engineSetting());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            long size = ((Circuit) object.definition()).elements().size();
            return settings.integer("points") * size * size * size;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            CircuitEngine.SweepRequest request = new CircuitEngine.SweepRequest(settings.text("source"),
                    settings.text("parameter"), settings.number("from"), settings.number("to"), settings.integer("points"));
            return engineOf(engines, settings).directCurrentSweep(object, request, progress);
        }
    }
}
