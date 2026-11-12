package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.layout.CircuitLayout;
import org.jmouse.cad.pcb.layout.PcbLayout;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentFacts;
import org.jmouse.cad.simulation.PartRole;
import org.jmouse.cad.simulation.elaboration.SheetUnfolding;
import org.jmouse.cad.simulation.SimulationReading;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A new pcb for a circuit, as the engine lays it out ({@link CircuitLayout}): the circuit unfolded as a pcb lays out a design
 * whole, the parts that stand on a pcb — every one but the bench's sources and instruments, which drive and read it from outside
 * —, each on its footprint with its pads on the nets the circuit wires them to, where the sheet draws it; and the nets the bench
 * reaches on the pcb, which a header brings out, the ground first.
 */
public final class CircuitBoards {

    private CircuitBoards() {
    }

    /** What a part of a component does that keeps it off the pcb: it drives or reads the circuit from the bench. */
    private static boolean onTheBench(PartRole role) {
        return role == PartRole.SOURCE || role == PartRole.INSTRUMENT;
    }

    /**
     * The pcb a circuit is laid out on, fresh: its parts placed, its edge drawn round them, its ground poured, no copper laid.
     *
     * @param circuitDocument the circuit's document
     * @param catalogue       the documents it names, as the reader may see them
     */
    public static PcbDrawing laidOut(DocumentNode circuitDocument, CatalogueDocuments catalogue, PcbLayout layout) {
        CircuitLayout.Settings settings = layout.circuit();
        Circuit drawn = SimulationReading.circuit(circuitDocument);
        Circuit circuit = SheetUnfolding.of(drawn, catalogue);
        ComponentFacts facts = new ComponentFacts(catalogue);
        Map<String, CircuitLayout.SheetPlace> places = new LinkedHashMap<>();
        for (Placement placement : drawn.layout()) {
            if (placement instanceof Placement.PlacedPart placed) {
                places.putIfAbsent(placed.reference(), new CircuitLayout.SheetPlace(placed.at().x(), placed.at().y(), placed.rotation()));
            }
        }
        List<CircuitLayout.BoardPart> parts = new ArrayList<>();
        Map<String, FootprintDrawing> footprints = new LinkedHashMap<>();
        Set<String> boardNets = new LinkedHashSet<>();
        List<Part> bench = new ArrayList<>();
        for (Part part : circuit.parts()) {
            if (onTheBench(facts.roleOf(part.component()))) {
                bench.add(part);
                continue;
            }
            Optional<String> footprint = Optional.ofNullable(part.footprint()).filter(slug -> !slug.isBlank()).or(() -> facts.footprintOf(part.component()));
            footprint.ifPresent(slug -> catalogue.find(slug).map(FootprintDrawing::of).ifPresent(drawing -> footprints.put(slug, drawing)));
            Map<String, String> nets = facts.padNets(part, footprint);
            boardNets.addAll(nets.values());
            parts.add(new CircuitLayout.BoardPart(part.reference(), footprint, nets));
        }
        Optional<String> ground = boardNets.contains(Circuit.GROUND) ? Optional.of(Circuit.GROUND) : Optional.empty();
        Set<String> benched = new LinkedHashSet<>();
        bench.forEach(part -> part.connections().stream().map(Connection::net).filter(boardNets::contains).forEach(benched::add));
        List<String> benchNets = new ArrayList<>(benched);
        benchNets.sort(Comparator.comparing((String net) -> !ground.map(net::equals).orElse(false)).thenComparing(CircuitBoards::numberedOrder));
        if (!benchNets.isEmpty()) {
            String header = settings.connector().footprintFor(benchNets.size());
            catalogue.find(header).map(FootprintDrawing::of).ifPresent(drawing -> footprints.put(header, drawing));
        }
        return new CircuitLayout(layout).laidOut(new CircuitLayout.Request(circuitDocument.slug(), parts, footprints, places, benchNets, ground));
    }

    /** Nets in the order a person counts them: {@code n2} before {@code n10}. */
    private static String numberedOrder(String net) {
        StringBuilder key = new StringBuilder();
        java.util.regex.Matcher numbers = java.util.regex.Pattern.compile("\\d+|\\D+").matcher(net);
        while (numbers.find()) {
            String piece = numbers.group();
            key.append(Character.isDigit(piece.charAt(0)) ? String.format("%020d", Long.parseLong(piece.length() > 18 ? piece.substring(0, 18) : piece)) : piece);
        }
        return key.toString();
    }
}
