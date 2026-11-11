package org.jmouse.cad.spice;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.node.SpiceStatement;
import org.jmouse.spice.node.SpiceValue;
import org.jmouse.units.Temperatures;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A circuit's transient computed by a SPICE program — a second engine, set beside the engine's own: the circuit written
 * as its deck, closed the way its program needs ({@link SpiceDialect}) to run it on an even grid of the samples asked and
 * print every vector, and the printout read back under the circuit's own names — a node by its net, a source's current
 * by its part.
 *
 * <p>It starts from SPICE's operating point at time 0, where the engine starts from rest; {@code uic} is not used, since
 * without the operating point's stepping a model's first point does not converge. A deck that leaves a part out is
 * refused before the program runs: its answer would be another circuit's, said as this one's.</p>
 */
public final class SpiceTransient {

    private final CatalogueDocuments catalogue;
    private final SpiceSimulator     simulator;
    private final SpiceDialect       dialect;
    private final SpiceDevices       devices;

    public SpiceTransient(CatalogueDocuments catalogue, SpiceSimulator simulator, SpiceDialect dialect) {
        this(catalogue, simulator, dialect, SpiceDevices.standard());
    }

    public SpiceTransient(CatalogueDocuments catalogue, SpiceSimulator simulator, SpiceDialect dialect, SpiceDevices devices) {
        this.catalogue = catalogue;
        this.simulator = simulator;
        this.dialect = dialect;
        this.devices = devices;
    }

    /**
     * A transient as SPICE computed it.
     *
     * @param voltages each net's voltage, by the net's name
     * @param currents each source's current into its first pin, by its part's reference
     * @param deck     the deck that ran, its closing included
     * @param notes    what the deck did not carry, or carried differently
     */
    public record Run(double[] time, Map<String, double[]> voltages, Map<String, double[]> currents, String deck,
                      List<String> notes) {

        public Run {
            voltages = Map.copyOf(voltages);
            currents = Map.copyOf(currents);
            notes = List.copyOf(notes);
        }
    }

    /**
     * Runs a circuit.
     *
     * @param parameters its parameters at other values than the document's — where a bench's sliders stand
     * @param samples    how many even steps the run is printed in
     * @throws IllegalArgumentException when it is no circuit, or the deck leaves a part out
     * @throws SpiceRunException        when the program gives no answer
     */
    public Run run(DocumentNode document, Map<String, Double> parameters, int samples) {
        if (!document.kind().equals(SimulationModule.CIRCUIT)) {
            throw new IllegalArgumentException(dialect.program() + " runs a circuit; this is a " + document.kind().word() + ".");
        }
        if (samples < 2) {
            throw new IllegalArgumentException("A transient is printed in at least two samples.");
        }
        SpiceExporter.Written written = new SpiceExporter(catalogue, devices, parameters).write(document);
        List<String> notes = written.notes().stream().map(SpiceNote::toString).toList();
        if (!written.leftOut().isEmpty()) {
            throw new IllegalArgumentException(dialect.program() + " cannot carry " + String.join(", ", written.leftOut())
                    + ", so its answer would be another circuit's: " + String.join("; ", notes));
        }
        String text = closed(written.deck(), samples).toSource();
        SpiceDialect.PrintedVectors printed = dialect.read(simulator.run(text));
        return read(printed, SimulationReading.circuit(document), written.elementNames(), text, notes);
    }

    /**
     * The deck with its transient printed in {@code samples} even steps, and what its program needs to run it at the
     * bench's nominal temperature and print every vector put before its {@code .end}.
     */
    private SpiceDeck closed(SpiceDeck deck, int samples) {
        List<SpiceStatement> statements = new ArrayList<>();
        double celsius = Temperatures.celsius(BenchConditions.NOMINAL_TEMPERATURE);
        double step = 0;
        for (SpiceStatement statement : deck.statements()) {
            if (statement instanceof SpiceCard.Transient run) {
                step = seconds(run.stop()) / samples;
                statements.add(new SpiceCard.Transient(SpiceValue.number(step), run.stop(), null, null, false,
                        SpicePosition.NONE));
            } else if (statement instanceof SpiceCard.End end) {
                statements.addAll(dialect.runAndPrint(step, celsius));
                statements.add(end);
            } else {
                statements.add(statement);
            }
        }
        return SpiceDeck.of(deck.title().orElse(null), statements);
    }

    /** The printed vectors under the circuit's names: a node by its net, a source's branch by its part. */
    private static Run read(SpiceDialect.PrintedVectors printed, Circuit circuit, Map<String, String> elementNames,
                            String deck, List<String> notes) {
        Map<String, double[]> voltages = new LinkedHashMap<>();
        for (Part part : circuit.parts()) {
            for (Connection connection : part.connections()) {
                double[] values = printed.nodes().get(connection.net().toLowerCase(Locale.ROOT));
                if (values != null) {
                    voltages.putIfAbsent(connection.net(), values);
                }
            }
        }
        Map<String, double[]> currents = new LinkedHashMap<>();
        elementNames.forEach((reference, element) -> {
            double[] values = printed.branches().get(element.toLowerCase(Locale.ROOT));
            if (values != null) {
                currents.put(reference, values);
            }
        });
        return new Run(printed.time(), voltages, currents, deck, notes);
    }

    private static double seconds(SpiceValue value) {
        if (value instanceof SpiceValue.Number number) {
            return number.value();
        }
        throw new IllegalArgumentException("the deck's transient runs for '" + value + "', which is no number of seconds");
    }
}
