package org.jmouse.cad.exchange.sheet;

import org.jmouse.cad.Point;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationVocabulary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.jmouse.cad.exchange.sheet.SheetGeometry.BOX_LEAD;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.BOX_PITCH;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.GRID;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.along;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.labelWidth;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.roundUp;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.snap;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.widest;

/**
 * How a component is drawn on a sheet, as read from its documents: its pins and blocks, the symbol it is drawn with and
 * which of the symbol's ports each pin is, or — without a symbol — the sides of the box its pins leave.
 *
 * @param component the component's slug
 * @param title     what it is called
 * @param units     the blocks of its package, {@code A}, {@code B}; empty for one
 * @param pins      its pins, by name, in the order it writes them
 * @param shared    the pins every block shares: a supply, a ground
 * @param pinOrder  its pins' names in the order it writes them
 * @param drawnWith the symbol it is drawn with, when it has one
 * @param drawing   that symbol's drawing, when the symbol is there to be read
 * @param ports     each port of the symbol that is named otherwise than its pin → the pin
 * @param sides     a box's sides as the component writes them ({@code symbol box}), each with its pins in order
 * @param written   the component as written
 * @param compiled  the component as the engine compiles it
 */
public record SymbolLook(String component, String title, List<String> units, Map<String, Pin> pins, Set<String> shared,
                         List<String> pinOrder, Optional<String> drawnWith, Optional<SymbolDrawing> drawing,
                         Map<String, String> ports, Map<String, List<String>> sides, Component written,
                         SimulatedComponent compiled) {

    /** The side a box lists its supply pins under; they leave its top. */
    private static final String POWER_SIDE = "power";

    /** The sides of a box in the order its pins are laid out. */
    private static final List<String> SIDES = List.of(SimulationVocabulary.LEFT, SimulationVocabulary.RIGHT,
            SimulationVocabulary.TOP, SimulationVocabulary.BOTTOM);

    /** The component's pin a terminal is: {@code positive} its first, {@code negative} its second, else itself. */
    public String pinOf(String terminal) {
        int inOrder = SimulationVocabulary.IN_ORDER_TERMINALS.indexOf(terminal);
        return inOrder >= 0 && !pins.containsKey(terminal) && inOrder < pinOrder.size() ? pinOrder.get(inOrder) : terminal;
    }

    /** The point the drawing puts a component's pin at, through the port its symbol binds to it; empty without one. */
    public Optional<Point> symbolPoint(String pin) {
        return drawing.flatMap(found -> found.pins().stream()
                .filter(drawn -> ports.getOrDefault(drawn.name(), drawn.name()).equals(pin))
                .findFirst().map(drawn -> new Point(drawn.x(), drawn.y())));
    }

    /** A pin's place along its side: as the box's rows write it, then as the component's pins do. */
    public int order(String terminal) {
        String pin = bare(terminal);
        int index = 0;
        for (List<String> row : sides.values()) {
            for (String written : row) {
                if (written.equals(pin)) {
                    return index;
                }
                index++;
            }
        }
        int declared = pinOrder.indexOf(pin);
        return declared < 0 ? Integer.MAX_VALUE : index + declared;
    }

    /**
     * What a box prints at a pin, as the sheet's {@code pinCaption} does: its description when that is one word — {@code PA0},
     * {@code ~{RESET}} written plainly —, else its name.
     */
    public String caption(String pin) {
        Pin model = pins.get(pin);
        String description = model == null || model.label() == null ? "" : model.label().strip();
        if (description.isEmpty() || description.chars().anyMatch(Character::isWhitespace)) {
            return pin;
        }
        return description.replaceAll("~\\{([^}]*)}", "$1");
    }

    /** The side of its box a pin leaves from: the side the component writes it on, else power on top, ground below, outputs right. */
    public String sideOf(String pin) {
        for (Map.Entry<String, List<String>> side : sides.entrySet()) {
            if (side.getValue().contains(pin)) {
                return side.getKey().equals(POWER_SIDE) ? SimulationVocabulary.TOP : side.getKey();
            }
        }
        Pin model = pins.get(pin);
        if (model != null && model.power()) {
            return SimulationVocabulary.TOP;
        }
        if (model != null && model.ground()) {
            return SimulationVocabulary.BOTTOM;
        }
        return model != null && model.output() ? SimulationVocabulary.RIGHT : SimulationVocabulary.LEFT;
    }

    /**
     * Each terminal of a part — what a wire names it by — and its net: a pin's name for a part wired by name,
     * {@code positive} and {@code negative} for a two-terminal part wired in pin order, else the component's pin in that order.
     */
    public Map<String, String> wiring(Part part) {
        Map<String, String> wired = new LinkedHashMap<>();
        List<Connection> connections = part.connections();
        boolean twoInOrder = part.inPinOrder() && connections.size() == SimulationVocabulary.IN_ORDER_TERMINALS.size();
        for (int index = 0; index < connections.size(); index++) {
            Connection connection = connections.get(index);
            String terminal = twoInOrder ? SimulationVocabulary.IN_ORDER_TERMINALS.get(index)
                    : connection.named().orElse(index < pinOrder.size() ? pinOrder.get(index) : null);
            if (terminal != null) {
                wired.put(terminal, connection.net());
            }
        }
        return wired;
    }

    /** Each terminal of a part and the component's pin it is, in the order its box or its pins write them. */
    public Map<String, String> terminalPins(Part part) {
        Map<String, String> terminals = new LinkedHashMap<>();
        wiring(part).keySet().stream().sorted((first, second) -> Integer.compare(order(pinOf(first)), order(pinOf(second))))
                .forEach(terminal -> terminals.put(terminal, pinOf(terminal)));
        return terminals;
    }

    /**
     * Where each terminal of a part drawn as a box stands, in the part's own drawing: its pins on the sides its component
     * writes — power on top, ground below, outputs right, the rest left —, two grid steps apart, as the sheet's
     * {@code boxGeometry} lays them.
     *
     * @param own each terminal the box draws → the component's pin it is
     */
    public Map<String, Point> boxPorts(Map<String, String> own) {
        Map<String, List<String>> laid = new LinkedHashMap<>();
        for (String side : SIDES) {
            laid.put(side, new ArrayList<>());
        }
        own.forEach((terminal, pin) -> laid.get(sideOf(bare(pin))).add(terminal));
        laid.values().forEach(list -> list.sort((first, second) -> Integer.compare(order(own.get(first)), order(own.get(second)))));
        List<String> left = laid.get(SimulationVocabulary.LEFT);
        List<String> right = laid.get(SimulationVocabulary.RIGHT);
        List<String> top = laid.get(SimulationVocabulary.TOP);
        List<String> bottom = laid.get(SimulationVocabulary.BOTTOM);
        // The box is as wide as what it prints at its pins — their captions, as the sheet measures them.
        Function<List<String>, Double> captions = terminals -> widest(terminals.stream().map(terminal -> caption(bare(own.get(terminal)))).toList());
        double across = Math.max(Math.max(captions.apply(top), captions.apply(bottom)), BOX_PITCH);
        double horizontalPitch = roundUp(across + 8, BOX_PITCH);
        boolean nameInside = !top.isEmpty() && !bottom.isEmpty();
        double inner = Math.max(Math.max(captions.apply(left) + captions.apply(right) + 12 + (nameInside ? labelWidth(title) : 0),
                top.size() * horizontalPitch), Math.max(bottom.size() * horizontalPitch, 48));
        double halfWidth = roundUp(inner / 2, GRID);
        double halfHeight = Math.max(GRID, roundUp(Math.max(left.size(), right.size()) * BOX_PITCH / 2, GRID));
        Map<String, Point> ports = new LinkedHashMap<>();
        for (String side : SIDES) {
            List<String> list = laid.get(side);
            for (int index = 0; index < list.size(); index++) {
                if (list == left || list == right) {
                    double y = snap(along(list, index, BOX_PITCH));
                    double edge = list == left ? -halfWidth : halfWidth;
                    ports.put(list.get(index), new Point(edge + (list == left ? -BOX_LEAD : BOX_LEAD), y));
                } else {
                    double x = snap(along(list, index, horizontalPitch));
                    double edge = list == top ? -halfHeight : halfHeight;
                    ports.put(list.get(index), new Point(x, edge + (list == top ? -BOX_LEAD : BOX_LEAD)));
                }
            }
        }
        return ports;
    }

    /** A block's pin, {@code A.output}, is the pin {@code output}. */
    public static String bare(String pin) {
        return pin == null ? "" : pin.substring(pin.indexOf('.') + 1);
    }

    /** The block a terminal names, {@code A} of {@code A.output}; null for a pin of the whole part. */
    public static String unitOf(String terminal) {
        int dot = terminal.indexOf('.');
        return dot > 0 ? terminal.substring(0, dot) : null;
    }
}
