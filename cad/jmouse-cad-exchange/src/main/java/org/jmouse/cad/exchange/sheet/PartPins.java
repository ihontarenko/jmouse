package org.jmouse.cad.exchange.sheet;

import org.jmouse.cad.Point;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.jmouse.cad.exchange.sheet.SymbolLook.bare;
import static org.jmouse.cad.exchange.sheet.SymbolLook.unitOf;

/**
 * Where the pins of a part stand on its sheet: each place it is drawn at — the whole part, or a block of it placed on its
 * own ({@code U4.B at …}) —, the terminals drawn there, and the point of each, in the part's own drawing and on the sheet.
 *
 * <p>The engine's one answer to where a pin is: what a schematic is written to another tool with, and what the sheet's
 * checks find a no-connect mark on.</p>
 */
public final class PartPins {

    /**
     * How a part's blocks are placed on its sheet.
     *
     * @param together the blocks drawn as one, at the part's own place: every block not placed on its own
     * @param placed   each place the part is drawn at
     */
    public record Arrangement(List<String> together, List<PlacedUnit> placed) {

        public Arrangement {
            together = List.copyOf(together);
            placed = List.copyOf(placed);
        }

        /** Every terminal of the part on the sheet, where it stands. */
        public Map<String, Point> onSheet() {
            Map<String, Point> points = new LinkedHashMap<>();
            placed.forEach(unit -> points.putAll(unit.onSheet()));
            return points;
        }
    }

    /**
     * One place a part is drawn at.
     *
     * @param item  where it stands
     * @param unit  the block drawn there; null for a part of one block
     * @param drawn whether it is drawn with its symbol: every pin it wires is one of the symbol's ports. A part without a
     *              symbol, or wired to a pin its symbol does not draw, is drawn as a box.
     * @param own   each terminal it wires here → the component's pin it is
     * @param local each terminal drawn here, in the part's own drawing: its wired ones; the pins of its symbol it leaves open;
     *              and a wired pin again under the name its symbol gives it — {@code terminal_one} of a resistor wired as
     *              {@code positive}, {@code A.Vcc} of a supply every block shares —, since a wire may name it so
     * @param open  the terminals of its symbol it leaves open: a pin it does not wire under any name
     */
    public record PlacedUnit(Placement.PlacedPart item, String unit, boolean drawn, Map<String, String> own, Map<String, Point> local,
                             Set<String> open) {

        public PlacedUnit {
            own = Collections.unmodifiableMap(new LinkedHashMap<>(own));
            local = Collections.unmodifiableMap(new LinkedHashMap<>(local));
            open = Collections.unmodifiableSet(new LinkedHashSet<>(open));
        }

        /** Every terminal drawn here, under every name a wire may give it, where it stands on the sheet. */
        public Map<String, Point> onSheet() {
            Map<String, Point> points = new LinkedHashMap<>();
            local.forEach((terminal, point) -> points.put(terminal, SheetGeometry.onSheet(item, point)));
            return points;
        }

        /** Each pin drawn here once — wired or open, under the name a wire gives it —, where it stands on the sheet. */
        public Map<String, Point> pinsOnSheet() {
            Map<String, Point> points = new LinkedHashMap<>();
            local.forEach((terminal, point) -> {
                if (own.containsKey(terminal) || open.contains(terminal)) {
                    points.put(terminal, SheetGeometry.onSheet(item, point));
                }
            });
            return points;
        }
    }

    private PartPins() {
    }

    /** Where a part's pins stand at each place it is drawn. */
    public static Arrangement of(Part part, SymbolLook look, List<Placement.PlacedPart> placed) {
        Map<String, String> terminalPins = look.terminalPins(part);
        Set<String> apart = new LinkedHashSet<>();
        placed.stream().filter(item -> item.unit() != null).forEach(item -> apart.add(item.unit()));
        List<String> together = look.units().stream().filter(unit -> !apart.contains(unit)).toList();
        List<PlacedUnit> units = placed.stream().map(item -> {
            String unit = item.unit() != null ? item.unit() : together.isEmpty() ? null : together.getFirst();
            Map<String, String> own = new LinkedHashMap<>();
            terminalPins.forEach((terminal, pin) -> {
                String letter = unitOf(terminal);
                boolean mine = item.unit() != null ? item.unit().equals(letter) : letter == null || !apart.contains(letter);
                if (mine) {
                    own.put(terminal, pin);
                }
            });
            return placedUnit(look, item, unit, own);
        }).toList();
        return new Arrangement(together, units);
    }

    private static PlacedUnit placedUnit(SymbolLook look, Placement.PlacedPart item, String unit, Map<String, String> own) {
        Optional<SymbolDrawing> drawing = look.drawing();
        boolean drawn = drawing.isPresent() && own.values().stream().allMatch(pin -> look.symbolPoint(bare(pin)).isPresent());
        Map<String, Point> local = new LinkedHashMap<>();
        Set<String> open = new LinkedHashSet<>();
        if (drawn) {
            own.forEach((terminal, pin) -> local.put(terminal, look.symbolPoint(bare(pin)).orElseThrow()));
            Set<String> wired = new LinkedHashSet<>();
            own.values().forEach(pin -> wired.add(bare(pin)));
            // A pin left open is drawn too, and a wire may end on it, or a no-connect mark stand on it.
            for (SymbolDrawing.Pin drawnPin : drawing.get().pins()) {
                String pin = look.ports().getOrDefault(drawnPin.name(), drawnPin.name());
                String terminal = unit != null && look.units().size() > 1 ? unit + "." + pin : pin;
                if (local.putIfAbsent(terminal, new Point(drawnPin.x(), drawnPin.y())) == null && !wired.contains(pin)) {
                    open.add(terminal);
                }
            }
        } else {
            local.putAll(look.boxPorts(own));
        }
        return new PlacedUnit(item, unit, drawn, own, local, open);
    }
}
