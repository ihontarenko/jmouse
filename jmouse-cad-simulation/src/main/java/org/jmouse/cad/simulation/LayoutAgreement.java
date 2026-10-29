package org.jmouse.cad.simulation;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.component.Pin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Whether a circuit's drawing says what its parts say. The parts line is the netlist — the truth the engine runs —; the
 * layout draws it. A wire joins two terminals, so every terminal one stretch of wires reaches must be on one net: a part's
 * pin on the net its line wires it to, a mark on the net it names — a ground on node {@value Circuit#GROUND} —, a junction
 * on whatever its wires bring. A stretch that reaches two nets is a drawing of another circuit than the one that runs.
 */
public final class LayoutAgreement {

    /** The marks that are node 0 when they name no net of their own: a ground, a common. */
    private static final Set<String> GROUNDS = Set.of(SimulationVocabulary.GROUND, "common");

    /** The marks that name no net: a junction is the wires it joins. */
    private static final String JUNCTION = "junction";

    private LayoutAgreement() {
    }

    /**
     * Each stretch of wires that reaches more than one net, said as the nets and the terminals that carry them; empty when
     * the drawing agrees with the parts.
     */
    public static List<String> contradictions(Circuit circuit, CatalogueDocuments catalogue) {
        Map<String, String> netOfTerminal = terminalNets(circuit, catalogue);
        Map<String, String> joined = new HashMap<>();
        for (Placement placement : circuit.layout()) {
            if (placement instanceof Placement.Wire wire) {
                join(joined, terminal(circuit, wire.from()), terminal(circuit, wire.to()));
            }
        }
        Map<String, Map<String, Set<String>>> stretches = new LinkedHashMap<>();
        joined.keySet().forEach(terminal -> {
            String net = netOfTerminal.get(terminal);
            if (net != null) {
                stretches.computeIfAbsent(find(joined, terminal), ignored -> new TreeMap<>())
                        .computeIfAbsent(net, ignored -> new LinkedHashSet<>()).add(terminal);
            }
        });
        return stretches.values().stream().filter(nets -> nets.size() > 1)
                .map(nets -> "one stretch of wires reaches " + String.join(" and ", nets.entrySet().stream()
                        .map(net -> "'" + net.getKey() + "' (" + String.join(", ", net.getValue()) + ")").toList()))
                .toList();
    }

    /** Every terminal the drawing can name and the net it is on: each part's pins, each mark that names a net. */
    private static Map<String, String> terminalNets(Circuit circuit, CatalogueDocuments catalogue) {
        Map<String, String> nets = new HashMap<>();
        Map<String, Optional<SimulatedComponent>> compiled = new HashMap<>();
        for (Part part : circuit.parts()) {
            Optional<SimulatedComponent> component = compiled.computeIfAbsent(part.component(), slug -> catalogue.find(slug)
                    .filter(document -> document.kind().equals(SimulationModule.COMPONENT))
                    .map(document -> ComponentCompilation.compile(SimulationReading.component(document), catalogue)));
            List<Connection> connections = part.connections();
            boolean twoInOrder = part.inPinOrder() && connections.size() == SimulationVocabulary.IN_ORDER_TERMINALS.size();
            for (int index = 0; index < connections.size(); index++) {
                Connection connection = connections.get(index);
                if (connection.named().isPresent()) {
                    nets.put(part.reference() + "." + connection.pin(), connection.net());
                    continue;
                }
                if (twoInOrder) {
                    nets.put(part.reference() + "." + SimulationVocabulary.IN_ORDER_TERMINALS.get(index), connection.net());
                }
                int position = index;
                component.filter(found -> position < found.pins().size()).map(found -> found.pins().get(position))
                        .map(Pin::name).ifPresent(pin -> nets.put(part.reference() + "." + pin, connection.net()));
            }
        }
        for (Placement placement : circuit.layout()) {
            if (placement instanceof Placement.Mark mark && !mark.symbol().equals(JUNCTION)) {
                boolean ground = GROUNDS.contains(mark.symbol()) && mark.net() == null;
                nets.put(mark.reference(), ground ? Circuit.GROUND : mark.net());
            }
        }
        return nets;
    }

    /** A wire's end as the drawing names it: a mark by its reference alone, a part's pin by its reference and pin. */
    private static String terminal(Circuit circuit, String written) {
        int dot = written.indexOf('.');
        String reference = dot < 0 ? written : written.substring(0, dot);
        boolean mark = circuit.layout().stream().anyMatch(placement -> placement instanceof Placement.Mark found
                && found.reference().equals(reference));
        return mark ? reference : written;
    }

    private static void join(Map<String, String> joined, String first, String second) {
        joined.putIfAbsent(first, first);
        joined.putIfAbsent(second, second);
        String firstRoot = find(joined, first);
        String secondRoot = find(joined, second);
        if (!firstRoot.equals(secondRoot)) {
            joined.put(firstRoot, secondRoot);
        }
    }

    private static String find(Map<String, String> joined, String key) {
        String root = joined.getOrDefault(key, key);
        while (!root.equals(joined.getOrDefault(root, root))) {
            root = joined.getOrDefault(root, root);
        }
        return root;
    }
}
