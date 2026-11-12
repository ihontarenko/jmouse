package org.jmouse.cad.exchange.pcb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a pcb is built of: its parts grouped into lines, one a kind of part — its component, its value and its
 * footprint —, in the order the pcb first seats each kind. Every format that lists what to buy or what to place
 * reads it — an assembly table, an IPC-2581 file —, and so does whatever prices or shows it.
 */
public record BillOfMaterials(List<Line> lines) {

    /**
     * One kind of part and every part of that kind.
     *
     * @param footprint the footprint's slug; empty when neither the part nor its component names one
     * @param parts     the parts, in the pcb's order
     */
    public record Line(String component, String value, Optional<String> footprint, List<PcbFabrication.SeatedPart> parts) {

        public Line {
            parts = List.copyOf(parts);
        }

        public int quantity() {
            return parts.size();
        }

        public List<String> designators() {
            return parts.stream().map(PcbFabrication.SeatedPart::printed).toList();
        }

        /** Whether any of its pads goes through a hole: the part is mounted through the pcb, not on its surface. */
        public boolean throughHole() {
            return parts.getFirst().throughHole();
        }

        /** How many pads the kind's footprint has; 0 when it has none to read. */
        public int padCount() {
            return parts.getFirst().drawing().map(drawing -> drawing.pads().size()).orElse(0);
        }
    }

    public BillOfMaterials {
        lines = List.copyOf(lines);
    }

    public static BillOfMaterials of(PcbFabrication fabrication) {
        Map<List<String>, List<PcbFabrication.SeatedPart>> kinds = new LinkedHashMap<>();
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            // A hole is drilled and a mark is printed, neither bought.
            if (part.nothingToMount()) {
                continue;
            }
            kinds.computeIfAbsent(List.of(part.component(), part.value(), part.footprint().orElse("")), ignored -> new ArrayList<>())
                    .add(part);
        }
        return new BillOfMaterials(kinds.values().stream().map(parts -> new Line(parts.getFirst().component(),
                parts.getFirst().value(), parts.getFirst().footprint(), parts)).toList());
    }
}
