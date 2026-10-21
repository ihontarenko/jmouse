package org.jmouse.parallax.spice;

import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.circuit.CircuitEngine;
import org.jmouse.parallax.circuit.CircuitEngineKind;
import org.jmouse.parallax.circuit.CircuitTopology;
import org.jmouse.parallax.circuit.ColumnNames;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition.BenchConditions;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.node.SpiceArgument;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.node.SpiceElement;
import org.jmouse.spice.node.SpiceStatement;
import org.jmouse.spice.printer.SpicePrinter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SPICE as a {@link CircuitEngine}: the circuit written as a deck ({@link SpiceExporter}), a {@code .control} block that
 * runs one analysis and prints it, the printout read back ({@link SpicePrintout}) into the datasets the library's own
 * engine answers with — a node's voltage by the node, a source's current by the source, a node's gain, phase and noise
 * as {@code out gain}, {@code out phase}, {@code out noise} —, so the two can be set side by side.
 *
 * <p>The run is at the bench's temperature, the models' nominal temperature the bench's nominal one, as the library's
 * engine computes them. What SPICE cannot say is never guessed: a deck that leaves a part out is another circuit, and a
 * computation is refused rather than answered for it ({@link #run}, which a caller that shows the notes may use); an
 * analysis SPICE has no counterpart for — the logic levels, every model pin's current — is refused naming it.</p>
 */
public final class SpiceEngine implements CircuitEngine {

    private static final String BRANCH = "#branch";
    /** The axes ngspice prints: a transient's, a frequency analysis', a DC sweep's of a voltage or a current source. */
    private static final List<String> TIME = List.of("time");
    private static final List<String> FREQUENCY = List.of("frequency");
    private static final List<String> SWEEPS = List.of("v-sweep", "i-sweep");
    /** ngspice's own name for a noise analysis' spectrum: the noise at the output, V/√Hz. */
    private static final String OUTPUT_NOISE = "onoise_spectrum";
    /** Each {@code noise} command makes two plots, the spectrum and the integrated noise: the n-th spectrum is 2n − 1. */
    private static final String NOISE_PLOT = "noise";
    /** A node a part keeps inside itself is named after the part ({@code D1.junction}); SPICE has no name for it. */
    private static final Pattern INNER_NODE = Pattern.compile(".*\\..*");

    private final SpiceSimulator simulator;

    public SpiceEngine(SpiceSimulator simulator) {
        this.simulator = simulator;
    }

    /**
     * One analysis run by SPICE, with what it ran and what the deck could not carry.
     *
     * @param deck    the deck that ran, its control block included
     * @param notes   what the deck could not carry, by line
     * @param leftOut the parts the deck does not carry at all: with any, the answer is another circuit's
     */
    public record SpiceRun(Dataset dataset, String deck, List<String> notes, Set<String> leftOut) {
        public SpiceRun {
            notes = List.copyOf(notes);
            leftOut = Set.copyOf(leftOut);
        }

        /** The same run, saying one thing more about how its answer stands to the library's engine's. */
        SpiceRun noting(String note) {
            List<String> more = new java.util.ArrayList<>(notes);
            more.add(note);
            return new SpiceRun(dataset, deck, more, leftOut);
        }
    }

    /** What a SPICE transient says about its start, beside the library's engine's run from rest. */
    static final String TRANSIENT_START = "SPICE starts the run from its operating point at time 0, the library's engine "
            + "from rest: a supply written as a subcircuit stands at its voltage from the first sample, and a capacitor no "
            + "source charges at 0 starts where the operating point puts it";

    @Override
    public CircuitEngineKind kind() {
        return CircuitEngineKind.SPICE;
    }

    @Override
    public Dataset operatingPoint(CompiledObject circuit) {
        return carried(circuit, () -> operatingPointRun(circuit));
    }

    @Override
    public Dataset transientResponse(CompiledObject circuit, TransientRequest request, ComputationProgress progress) {
        return carried(circuit, () -> transientRun(circuit, request));
    }

    @Override
    public Dataset frequencyResponse(CompiledObject circuit, FrequencyRange range, ComputationProgress progress) {
        return carried(circuit, () -> frequencyResponseRun(circuit, range));
    }

    @Override
    public Dataset noiseResponse(CompiledObject circuit, FrequencyRange range, ComputationProgress progress) {
        return carried(circuit, () -> noiseResponseRun(circuit, range));
    }

    @Override
    public Dataset directCurrentSweep(CompiledObject circuit, SweepRequest request, ComputationProgress progress) {
        return carried(circuit, () -> directCurrentSweepRun(circuit, request));
    }

    // ─── the analyses, each a run ──────────────────────────────────────────────────────────────────

    /** {@code op}: every node's voltage and every source's current at rest. */
    public SpiceRun operatingPointRun(CompiledObject circuit) {
        return run(circuit, false, prepared -> List.of(marker("op"), "op", "print all"), (printout, prepared) -> {
            Map<String, Double> values = new LinkedHashMap<>();
            Map<String, String> units = new LinkedHashMap<>();
            printout.values("op").forEach((name, value) -> prepared.column(name).ifPresent(column -> {
                values.put(column.name(), value);
                units.put(column.name(), column.unit());
            }));
            Map<String, String> notes = new LinkedHashMap<>();
            notes.put("units", "a node in volts; a source's current in amperes, into its first terminal");
            return new Dataset.Measurements(values, notes, units);
        });
    }

    /**
     * {@code tran}: the run over the circuit's time span, or the window asked for, printed on an even grid of the samples
     * asked. It starts from SPICE's operating point at time 0, where the library's engine starts from rest: a supply
     * written as a subcircuit (a dual supply) stands at its voltage from the first sample, and a capacitor no source
     * charges at 0 (an integrator's) starts where the operating point puts it. {@code uic}, SPICE's own start from rest,
     * is not used: without the operating point's gmin and source stepping its first point does not converge on the
     * catalogue's op-amp model (checked against ngspice 47: "timestep too small; initial timepoint").
     */
    public SpiceRun transientRun(CompiledObject circuit, TransientRequest request) {
        if (request.levels()) {
            throw new IllegalArgumentException("SPICE has no logic levels: a digital node's level is the library's "
                    + "engine's alone.");
        }
        if (request.everyPart()) {
            throw new IllegalArgumentException("SPICE prints the sources' currents, not every part's and every pin's: "
                    + "ask the library's engine for those.");
        }
        Circuit definition = (Circuit) circuit.definition();
        double[] window = request.window();
        double start = window == null ? 0 : window[0];
        double stop = window == null ? definition.time().maximum() : window[1];
        if (!(stop > start)) {
            throw new IllegalArgumentException("A transient runs over a span: its end comes after its start.");
        }
        double step = (stop - start) / request.samples();
        return run(circuit, false, prepared -> List.of(marker("tran"), "tran " + number(step) + " " + number(stop)
                + " " + number(start), "linearize", "print all"), (printout, prepared) -> {
            SpicePrintout.Table table = printout.table("tran");
            return series("t", table, table.axisColumn(TIME), prepared, Dataset.TimeSeries.AxisScale.LINEAR);
        }).noting(TRANSIENT_START);
    }

    /**
     * {@code ac}: every node's gain and phase, every source excited at 1 with phase 0 — as the library's engine excites
     * them —, over the same span at as many points a decade as the points asked make.
     */
    public SpiceRun frequencyResponseRun(CompiledObject circuit, FrequencyRange range) {
        return run(circuit, true, prepared -> {
            prepared.requireExcitable();
            return List.of(marker("ac"), "ac dec " + pointsPerDecade(range) + " " + number(range.from()) + " "
                    + number(range.to()), "print all");
        }, (printout, prepared) -> {
            SpicePrintout.Table table = printout.table("ac");
            float[] axis = table.real(table.axisColumn(FREQUENCY));
            List<String> columns = new ArrayList<>();
            List<float[]> values = new ArrayList<>();
            List<String> units = new ArrayList<>();
            for (String node : prepared.nodes()) {
                int column = prepared.columnOf(table, node);
                if (column < 0 || !table.complex(column)) {
                    continue;
                }
                float[] gains = new float[table.size()];
                float[] phases = new float[table.size()];
                for (int row = 0; row < table.size(); row++) {
                    double[] phasor = table.at(row, column);
                    double modulus = Math.hypot(phasor[0], phasor[1]);
                    gains[row] = (float) (20 * Math.log10(Math.max(modulus, 1e-12)));
                    phases[row] = (float) Math.toDegrees(Math.atan2(phasor[1], phasor[0]));
                }
                columns.add(node + ColumnNames.GAIN_SUFFIX);
                values.add(gains);
                units.add(ColumnNames.DECIBELS);
                columns.add(node + ColumnNames.PHASE_SUFFIX);
                values.add(phases);
                units.add(ColumnNames.DEGREES);
            }
            return new Dataset.TimeSeries("f", axis, columns, values, units, Dataset.TimeSeries.AxisScale.LOGARITHMIC);
        });
    }

    /**
     * {@code noise}, once a node: SPICE's noise analysis reports one output, so every node is its own run, each referred
     * to the circuit's first independent source — which only names where an input noise would be referred, and changes
     * nothing at the output.
     */
    public SpiceRun noiseResponseRun(CompiledObject circuit, FrequencyRange range) {
        // The input it is referred to needs an AC value, though the noise at an output does not depend on it.
        return run(circuit, true, prepared -> {
            String input = prepared.independentSource().orElseThrow(() -> new IllegalArgumentException(
                    "SPICE's noise analysis is referred to an independent source, and this circuit has none."));
            List<String> commands = new ArrayList<>();
            List<String> nodes = prepared.printableNodes();
            for (int index = 0; index < nodes.size(); index++) {
                commands.add("noise v(" + nodes.get(index) + ") " + input + " dec " + pointsPerDecade(range) + " "
                        + number(range.from()) + " " + number(range.to()));
                commands.add("setplot " + NOISE_PLOT + (2 * index + 1));
                commands.add(marker("noise " + index));
                commands.add("print " + OUTPUT_NOISE);
            }
            return commands;
        }, (printout, prepared) -> {
            List<String> nodes = prepared.printableNodes();
            List<String> columns = new ArrayList<>();
            List<float[]> values = new ArrayList<>();
            float[] axis = null;
            for (int index = 0; index < nodes.size(); index++) {
                SpicePrintout.Table table = printout.table("noise " + index);
                axis = axis == null ? table.real(table.axisColumn(FREQUENCY)) : axis;
                columns.add(nodes.get(index) + ColumnNames.NOISE_SUFFIX);
                values.add(table.real(table.names().indexOf(OUTPUT_NOISE)));
            }
            return new Dataset.TimeSeries("f", axis == null ? new float[0] : axis, columns, values,
                    Collections.nCopies(columns.size(), ColumnNames.NOISE_DENSITY), Dataset.TimeSeries.AxisScale.LOGARITHMIC);
        });
    }

    /**
     * {@code dc}: a source swept from one value to the other; a parameter is swept as SPICE sweeps one, set at each value
     * ({@code alterparam}) and the circuit's operating point found again.
     */
    public SpiceRun directCurrentSweepRun(CompiledObject circuit, SweepRequest request) {
        if (request.sweepsParameter()) {
            return parameterSweepRun(circuit, request);
        }
        if (request.from() == request.to()) {
            throw new IllegalArgumentException("SPICE sweeps a source in steps: the sweep needs two different ends.");
        }
        return run(circuit, false, prepared -> {
            String source = prepared.sweptSource(request.source());
            double step = (request.to() - request.from()) / (request.points() - 1);
            return List.of(marker("dc"), "dc " + source + " " + number(request.from()) + " " + number(request.to())
                    + " " + number(step), "print all");
        }, (printout, prepared) -> {
            SpicePrintout.Table table = printout.table("dc");
            String axis = request.source().isEmpty() ? prepared.sources().getFirst() : request.source();
            return series(axis, table, table.axisColumn(SWEEPS), prepared, Dataset.TimeSeries.AxisScale.LINEAR);
        });
    }

    private SpiceRun parameterSweepRun(CompiledObject circuit, SweepRequest request) {
        String parameter = request.parameter();
        if (circuit.parameters().stream().noneMatch(definition -> definition.name().equals(parameter))) {
            throw new IllegalArgumentException("The circuit has no parameter '" + parameter + "' to sweep.");
        }
        return run(circuit, false, prepared -> {
            List<String> commands = new ArrayList<>();
            for (int point = 0; point < request.points(); point++) {
                commands.add("alterparam " + parameter + " = " + number(request.value(point)));
                commands.add("reset");
                commands.add("op");
                commands.add(marker("point " + point));
                commands.add("print all");
            }
            return commands;
        }, (printout, prepared) -> {
            float[] axis = new float[request.points()];
            Map<String, float[]> columns = new LinkedHashMap<>();
            Map<String, String> units = new LinkedHashMap<>();
            for (int point = 0; point < request.points(); point++) {
                axis[point] = (float) request.value(point);
                int index = point;
                printout.values("point " + point).forEach((name, value) -> prepared.column(name).ifPresent(column -> {
                    columns.computeIfAbsent(column.name(), ignored -> new float[request.points()])[index] = value.floatValue();
                    units.put(column.name(), column.unit());
                }));
            }
            return new Dataset.TimeSeries(parameter, axis, List.copyOf(columns.keySet()), List.copyOf(columns.values()),
                    List.copyOf(units.values()));
        });
    }

    // ─── a run ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * Writes the circuit as a deck, adds the control block that runs the analysis, runs it and reads the printout.
     *
     * @param excited whether every source is excited at 1 for a small-signal analysis
     */
    private SpiceRun run(CompiledObject circuit, boolean excited,
                         Function<Prepared, List<String>> commands,
                         BiFunction<SpicePrintout, Prepared, Dataset> read) {
        Prepared prepared = prepare(circuit, excited);
        String deck = controlled(SpicePrinter.print(prepared.deck()), commands.apply(prepared), temperatureOf(circuit));
        Dataset dataset = read.apply(SpicePrintout.of(simulator.run(deck)), prepared);
        return new SpiceRun(dataset, deck, prepared.written().notes().stream().map(SpiceNote::toString).toList(),
                prepared.written().leftOut());
    }

    /** A run's dataset, refused when the deck left a part out: its answer would be another circuit's, said as this one's. */
    /**
     * A run's dataset, as the computations ask for it: what SPICE could not do — a circuit that does not write as a deck,
     * a run ngspice gave up on — is a refusal in its own words, as the library's engine refuses a circuit it cannot solve,
     * never an error of the server.
     */
    private Dataset carried(CompiledObject circuit, Supplier<SpiceRun> run) {
        try {
            // Refused before ngspice runs: a deck without a part answers for another circuit, and ngspice failing on
            // it would hide why.
            SpiceExporter.Written written = prepare(circuit, false).written();
            if (!written.leftOut().isEmpty()) {
                throw new IllegalArgumentException(leftOutRefusal(written.leftOut(),
                        written.notes().stream().map(SpiceNote::toString).toList()));
            }
            return carried(run.get());
        } catch (SpiceParseException unwritten) {
            throw new IllegalArgumentException("The circuit does not write as a SPICE deck: " + unwritten.getDetail(), unwritten);
        } catch (SpiceRunException failed) {
            throw new IllegalArgumentException("ngspice could not compute it: " + failed.getMessage(), failed);
        }
    }

    private static String leftOutRefusal(Set<String> leftOut, List<String> notes) {
        return "SPICE cannot carry " + String.join(", ", leftOut) + ", so its answer would be another circuit's: "
                + String.join("; ", notes);
    }

    private static Dataset carried(SpiceRun run) {
        if (!run.leftOut().isEmpty()) {
            throw new IllegalArgumentException(leftOutRefusal(run.leftOut(), run.notes()));
        }
        return run.dataset();
    }

    private Prepared prepare(CompiledObject circuit, boolean excited) {
        DefinedObject object = new DefinedObject("circuit", "Circuit", "", null, "circuits", List.of(), false, null,
                circuit.definition(), circuit.parameters(), List.of(), List.of(), List.of(), List.of());
        String source = ExperienceDefinitions.write(object);
        SpiceExporter.Written written = new SpiceExporter(circuit.library(), circuit.parameterValues()).write(source);
        SpiceDeck deck = excited ? excited(written.deck()) : written.deck();
        return new Prepared(deck, written, CircuitTopology.of(circuit));
    }

    /** The deck with every independent source excited at 1, phase 0: {@code AC 1} after what each already says. */
    private static SpiceDeck excited(SpiceDeck deck) {
        List<SpiceStatement> statements = new ArrayList<>();
        for (SpiceStatement statement : deck.statements()) {
            if (statement instanceof SpiceElement.Source source) {
                List<SpiceArgument> rest = new ArrayList<>(source.rest());
                rest.add(new SpiceArgument.Word("AC", SpicePosition.NONE));
                rest.add(new SpiceArgument.Word("1", SpicePosition.NONE));
                statements.add(new SpiceElement.Source(source.name(), source.positive(), source.negative(),
                        source.direct().orElse(null), source.directWord(), source.waveform().orElse(null), rest,
                        SpicePosition.NONE));
            } else {
                statements.add(statement);
            }
        }
        return SpiceDeck.of(deck.title().orElse(null), statements);
    }

    /** The bench's temperature in °C: nominal unless the circuit's conditions say otherwise. */
    private static double temperatureOf(CompiledObject circuit) {
        BenchConditions conditions = ((Circuit) circuit.definition()).conditions();
        return conditions == null ? BenchConditions.NOMINAL_TEMPERATURE : conditions.temperature();
    }

    /**
     * The deck with a control block before its {@code .end} — the card alone on its line, not the {@code .ends} or
     * {@code .endl} a text search for it also finds —: SPICE's own printing set wide and exact, the temperatures the
     * library's engine computes at, then the analysis' commands.
     */
    static String controlled(String deck, List<String> commands, double temperature) {
        Matcher end = END_CARD.matcher(deck);
        int last = -1;
        while (end.find()) {
            last = end.start();
        }
        String body = last >= 0 ? deck.substring(0, last) : deck + "\n";
        List<String> control = new ArrayList<>(List.of(".control", "set nobreak", "set width=100000", "set numdgt=7",
                "option temp=" + number(temperature) + " tnom=" + number(BenchConditions.NOMINAL_TEMPERATURE)));
        control.addAll(commands);
        control.add(".endc");
        return body + String.join("\n", control) + "\n.end\n";
    }

    private static final Pattern END_CARD = Pattern.compile("(?im)^[ \\t]*\\.end[ \\t]*$");

    private static String marker(String section) {
        return "echo " + SpicePrintout.MARKER + " " + section;
    }

    /** Enough points a decade that the span holds at least the points asked. */
    private static int pointsPerDecade(FrequencyRange range) {
        return Math.max(1, (int) Math.ceil((range.points() - 1) / Math.log10(range.to() / range.from())));
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.9g", value);
    }

    /** A table's node voltages and source currents along its axis, named as the library's engine names them. */
    private static Dataset series(String axisName, SpicePrintout.Table table, int axis, Prepared prepared,
                                  Dataset.TimeSeries.AxisScale scale) {
        List<String> columns = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        List<String> units = new ArrayList<>();
        for (int index = 0; index < table.names().size(); index++) {
            if (index == axis) {
                continue;
            }
            Optional<Column> column = prepared.column(table.names().get(index));
            if (column.isPresent() && !columns.contains(column.get().name())) {
                columns.add(column.get().name());
                values.add(table.real(index));
                units.add(column.get().unit());
            }
        }
        return new Dataset.TimeSeries(axisName, table.real(axis), columns, values, units, scale);
    }

    /** A printed name read as the library's: the column it is and its unit. */
    private record Column(String name, String unit) {
    }

    /** A circuit written for SPICE, with what it takes to read SPICE's names back as the circuit's. */
    private record Prepared(SpiceDeck deck, SpiceExporter.Written written, CircuitTopology topology) {

        List<String> nodes() {
            return topology.nodes();
        }

        List<String> sources() {
            return topology.sources();
        }

        /** The nodes SPICE has a name for: not the ones a part keeps inside itself. */
        List<String> printableNodes() {
            return topology.nodes().stream().filter(node -> !INNER_NODE.matcher(node).matches()).toList();
        }

        /** What a printed name is: a node's voltage ({@code out}) or a source's current ({@code v1#branch}). */
        Optional<Column> column(String printed) {
            String name = printed.toLowerCase(Locale.ROOT);
            if (name.endsWith(BRANCH)) {
                String element = name.substring(0, name.length() - BRANCH.length());
                return written.elementNames().entrySet().stream()
                        .filter(entry -> entry.getValue().equalsIgnoreCase(element))
                        .filter(entry -> topology.sources().contains(entry.getKey()))
                        .map(entry -> new Column(entry.getKey(), ColumnNames.AMPERES)).findFirst();
            }
            return topology.nodes().stream().filter(node -> node.equalsIgnoreCase(name))
                    .map(node -> new Column(node, ColumnNames.VOLTS)).findFirst();
        }

        /** Where a node's column is in a table; −1 when SPICE printed none. */
        int columnOf(SpicePrintout.Table table, String node) {
            for (int index = 0; index < table.names().size(); index++) {
                if (table.names().get(index).equalsIgnoreCase(node)) {
                    return index;
                }
            }
            return -1;
        }

        /** The name SPICE knows a source by, when it is an independent one — a behavioural source cannot be swept. */
        Optional<String> independent(String source) {
            String name = written.elementNames().get(source);
            if (name == null) {
                return Optional.empty();
            }
            char letter = Character.toUpperCase(name.charAt(0));
            return letter == 'V' || letter == 'I' ? Optional.of(name) : Optional.empty();
        }

        Optional<String> independentSource() {
            return topology.sources().stream().map(this::independent).flatMap(Optional::stream).findFirst();
        }

        String sweptSource(String asked) {
            String source = asked.isEmpty()
                    ? topology.sources().stream().findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "The circuit has no source to sweep; sweep one of its parameters instead."))
                    : asked;
            if (!topology.sources().contains(source)) {
                throw new IllegalArgumentException("'" + source + "' is not a source of this circuit.");
            }
            return independent(source).orElseThrow(() -> new IllegalArgumentException("SPICE sweeps an independent "
                    + "source, and " + source + " is written as a behavioural one: sweep it on the library's engine."));
        }

        /** Every source excited at 1 is a source SPICE can excite: a behavioural one has no AC value. */
        void requireExcitable() {
            List<String> behavioural = topology.sources().stream().filter(source -> independent(source).isEmpty()).toList();
            if (!behavioural.isEmpty()) {
                throw new IllegalArgumentException("SPICE excites independent sources only, and " + String.join(", ",
                        behavioural) + " is written as a behavioural one: its frequency response is the library's "
                        + "engine's to compute.");
            }
        }
    }
}
