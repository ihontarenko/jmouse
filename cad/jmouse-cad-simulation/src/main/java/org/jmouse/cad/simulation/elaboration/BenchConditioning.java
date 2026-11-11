package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.component.Equation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A circuit as it runs on a bench: what the bench does to some of its parts applied, so that nothing downstream needs to
 * know a bench exists. Every part reads the bench's figures already ({@link ElaboratedCircuit#parameters}); here:
 *
 * <ul>
 *   <li>a part the bench gives a tolerance and a coefficient of its own has them as its own {@code tolerance} and
 *       {@code temperature_coefficient}, which its equations read;</li>
 *   <li>a source's offset is a source of its own: in series with a voltage source, through a net of its own; beside a
 *       current source, between the same nets;</li>
 *   <li>a failed part is replaced by what it became: open, or shorted through the pins its component names.</li>
 * </ul>
 */
public final class BenchConditioning {

    /** The parameters a part's own tolerance and drift are, as its component reads them. */
    public static final String TOLERANCE = "tolerance";
    public static final String TEMPERATURE_COEFFICIENT = "temperature_coefficient";

    /** What a failed part's stand-ins are named after it: {@code R1_failed_1}. */
    private static final String STAND_IN = "_failed_";

    /** What an offset and the net it adds are named: {@code O1}, {@code offset_1}. */
    private static final String OFFSET = "O";
    private static final String OFFSET_NET = "offset_";

    /** How far apart a failed-open part leaves its pins, and how close a failed-short one ties them, ohms. */
    private static final double OPEN_OHMS = 1e9;
    private static final double SHORT_OHMS = 1e-3;

    /** What a voltage equation is, of the two a generator's drive can be. */
    private static final String VOLTAGE = "voltage";

    private BenchConditioning() {
    }

    /** Whether an element is what a failed part became — a stand-in, not a part of the circuit as it was written. */
    public static boolean isStandIn(String id) {
        return id.contains(STAND_IN);
    }

    /** The circuit with its bench applied; a circuit on a bench that does nothing to any part, as it is. */
    public static ElaboratedCircuit apply(ElaboratedCircuit circuit) {
        BenchConditions bench = circuit.bench();
        if (bench.tolerances().isEmpty() && bench.offsets().isEmpty() && bench.failures().isEmpty()) {
            return circuit;
        }
        Set<String> ids = new HashSet<>();
        Set<String> nets = new HashSet<>();
        for (ElaboratedPart part : circuit.parts()) {
            ids.add(part.reference());
            nets.addAll(part.connections().values());
        }
        List<ElaboratedPart> parts = new ArrayList<>();
        List<EngineElement> elements = new ArrayList<>(circuit.elements());
        int number = 1;
        for (ElaboratedPart part : circuit.parts()) {
            BenchConditions.Failure failure = bench.failures().get(part.reference());
            if (failure != null) {
                elements.addAll(failed(part, failure, ids));
                continue;
            }
            ElaboratedPart placed = withTolerance(part, bench.tolerances().get(part.reference()));
            Double offset = bench.offsets().get(part.reference());
            Optional<Equation.Branch> drive = part.component().generated();
            if (offset == null || drive.isEmpty()) {
                parts.add(placed);
                continue;
            }
            while (ids.contains(OFFSET + number) || nets.contains(OFFSET_NET + number)) {
                number++;
            }
            String id = OFFSET + number;
            String net = OFFSET_NET + number;
            ids.add(id);
            nets.add(net);
            String positive = placed.connections().get(drive.get().from());
            String negative = placed.connections().get(drive.get().to());
            if (drive.get().kind().equals(VOLTAGE)) {
                parts.add(rewired(placed, drive.get().from(), net));
                elements.add(new EngineElement(id, EngineElement.Kind.VOLTAGE_SOURCE, positive, net, offset));
            } else {
                parts.add(placed);
                elements.add(new EngineElement(id, EngineElement.Kind.CURRENT_SOURCE, positive, negative, offset));
            }
        }
        List<ElaboratedPart> written = new ArrayList<>();
        for (ElaboratedPart part : circuit.written().parts()) {
            written.add(withTolerance(part, bench.tolerances().get(part.reference())));
        }
        return new ElaboratedCircuit(parts, elements, circuit.values(), bench, circuit.duration(),
                new ElaboratedCircuit.Written(written, circuit.written().fixtures()), circuit.assemblies());
    }

    /** A part with the tolerance and the coefficient the bench gives it as its own; as it is when the bench gives none. */
    private static ElaboratedPart withTolerance(ElaboratedPart part, BenchConditions.Tolerance tolerance) {
        if (tolerance == null) {
            return part;
        }
        Map<String, Double> conditioned = new LinkedHashMap<>(part.conditioned());
        conditioned.put(TOLERANCE, tolerance.share());
        conditioned.put(TEMPERATURE_COEFFICIENT, tolerance.coefficient());
        return new ElaboratedPart(part.reference(), part.component(), part.connections(), part.family(), part.settings(),
                conditioned, part.reportedAs());
    }

    private static ElaboratedPart rewired(ElaboratedPart part, String pin, String net) {
        Map<String, String> connections = new LinkedHashMap<>(part.connections());
        connections.put(pin, net);
        return new ElaboratedPart(part.reference(), part.component(), connections, part.family(), part.settings(),
                part.conditioned(), part.reportedAs());
    }

    /**
     * A part as it is after it failed, as its component says it fails: open, each of its nets left on a gigaohm to ground
     * so none floats; short, the pins it names — every pin when it names none — tied to the first of them through a
     * milliohm, and the others left open. A part whose nets are all ground leaves nothing.
     */
    private static List<EngineElement> failed(ElaboratedPart part, BenchConditions.Failure failure, Set<String> ids) {
        Map<String, String> nets = part.connections();
        List<String> tied = new ArrayList<>();
        if (failure.shorts()) {
            for (String pin : failure.pins().isEmpty() ? nets.keySet() : failure.pins()) {
                String net = nets.get(pin);
                if (net == null) {
                    throw new IllegalArgumentException(part.reference() + " fails short through '" + pin + "', which it has no pin of.");
                }
                tied.add(net);
            }
        }
        List<EngineElement> replacement = new ArrayList<>();
        Set<String> placed = new LinkedHashSet<>();
        int number = 1;
        for (String net : nets.values()) {
            // A net is placed once: tied to the short's first net, or left open on its leak.
            if (!placed.add(net)) {
                continue;
            }
            boolean bridged = tied.contains(net);
            if (bridged ? net.equals(tied.getFirst()) : net.equals(Circuit.GROUND)) {
                continue;
            }
            String id = part.reference() + STAND_IN + number++;
            ids.add(id);
            replacement.add(bridged ? new EngineElement(id, EngineElement.Kind.CONDUCTANCE, tied.getFirst(), net, SHORT_OHMS)
                    : new EngineElement(id, EngineElement.Kind.CONDUCTANCE, net, Circuit.GROUND, OPEN_OHMS));
        }
        return replacement;
    }
}
