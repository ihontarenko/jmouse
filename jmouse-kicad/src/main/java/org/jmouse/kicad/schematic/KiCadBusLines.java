package org.jmouse.kicad.schematic;

import org.jmouse.kicad.KiCadPoint;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The bus lines of a sheet as KiCad joins them: stretches meeting end to end, or an end on another's middle, are one line, and a
 * line is the bus a label standing on it names. A box's bus pin a line reaches is that line's bus: KiCad joins a pin
 * {@code DQ[0..31]} to a line labelled {@code DPC[0..31]} member by member, in their order (kicad-cli 10, on its video demo).
 *
 * @param schematic the sheet
 * @param aliases   the project's bus aliases, by which a label or a pin may name a bus
 */
public record KiCadBusLines(KiCadSchematic schematic, Map<String, List<String>> aliases) {

    public KiCadBusLines {
        aliases = Map.copyOf(aliases);
    }

    /**
     * The bus the line reaching a point is named by: a label on any stretch of it that names a bus; for a line no label names, the
     * first box pin it reaches, in the order the sheet writes its boxes — KiCad joins the pins a line ties, member by member
     * (kicad-cli 10, on the CM5 demo) —, else the hierarchical label it leaves the sheet by; empty for no line, or a line naming no bus.
     */
    public Optional<KiCadBus> busReaching(KiCadPoint at) {
        List<KiCadWire> stretches = schematic.buses();
        Set<KiCadWire> line = new LinkedHashSet<>();
        Deque<KiCadWire> waiting = new ArrayDeque<>();
        stretches.stream().filter(stretch -> touches(stretch, at)).forEach(waiting::add);
        while (!waiting.isEmpty()) {
            KiCadWire stretch = waiting.pop();
            if (!line.add(stretch)) {
                continue;
            }
            for (KiCadWire other : stretches) {
                if (!line.contains(other) && (touches(other, stretch.start()) || touches(other, stretch.end())
                        || touches(stretch, other.start()) || touches(stretch, other.end()))) {
                    waiting.add(other);
                }
            }
        }
        Optional<KiCadBus> labelled = schematic.labels().stream().filter(label -> label.kind() != KiCadLabel.Kind.HIERARCHICAL)
                .filter(label -> line.stream().anyMatch(stretch -> touches(stretch, label.at())))
                .map(label -> KiCadBus.of(label.text(), aliases)).flatMap(Optional::stream).findFirst();
        return labelled.or(() -> schematic.sheets().stream().flatMap(box -> box.pins().stream())
                .filter(pin -> line.stream().anyMatch(stretch -> touches(stretch, pin.at())))
                .map(pin -> KiCadBus.of(pin.name(), aliases)).flatMap(Optional::stream).findFirst())
                .or(() -> schematic.labels().stream().filter(label -> label.kind() == KiCadLabel.Kind.HIERARCHICAL)
                        .filter(label -> line.stream().anyMatch(stretch -> touches(stretch, label.at())))
                        .map(label -> KiCadBus.of(label.text(), aliases)).flatMap(Optional::stream).findFirst());
    }

    private static boolean touches(KiCadWire stretch, KiCadPoint point) {
        List<KiCadPoint> points = stretch.points();
        for (int index = 1; index < points.size(); index++) {
            if (point.liesOn(points.get(index - 1), points.get(index))) {
                return true;
            }
        }
        return false;
    }
}
