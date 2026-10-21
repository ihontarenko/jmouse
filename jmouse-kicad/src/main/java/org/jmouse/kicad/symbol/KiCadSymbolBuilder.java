package org.jmouse.kicad.symbol;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.parser.node.KiCadAtom;
import org.jmouse.kicad.parser.node.KiCadList;
import org.jmouse.kicad.parser.node.KiCadNode;
import org.jmouse.kicad.parser.node.KiCadString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A library symbol built as KiCad's tree — what a schematic keeps a copy of in its {@code lib_symbols}, and what
 * {@link KiCadSymbolDefinition} reads back: its properties, the body each unit draws, and its pins. Unit 0 is what every
 * unit draws: a body all units share, a pin all units carry. Coordinates are a library's: millimetres, Y upwards.
 *
 * <pre>{@code
 * KiCadList resistor = KiCadSymbolBuilder.named("Device:R")
 *         .property("Reference", "R").property("Value", "R")
 *         .rectangle(0, new KiCadPoint(-1.016, -2.54), new KiCadPoint(1.016, 2.54))
 *         .pin(1, KiCadPinType.PASSIVE, new KiCadPoint(0, 3.81), 270, 1.27, "~", "1")
 *         .build();
 * }</pre>
 */
public final class KiCadSymbolBuilder {

    /** The size KiCad writes its texts in: 1.27 mm. */
    private static final double TEXT_SIZE = 1.27;

    private final String libraryId;
    private final Map<String, String> properties = new LinkedHashMap<>();
    /** What each unit draws, unit 0 being what all of them draw. */
    private final Map<Integer, List<KiCadNode>> graphics = new TreeMap<>();
    private final Map<Integer, List<KiCadNode>> pins = new TreeMap<>();
    private boolean power;
    private boolean pinNamesHidden;
    private boolean pinNumbersHidden;

    private KiCadSymbolBuilder(String libraryId) {
        this.libraryId = libraryId;
    }

    /** A symbol called by its library id: {@code Device:R}, {@code power:GND}. */
    public static KiCadSymbolBuilder named(String libraryId) {
        if (libraryId == null || libraryId.isBlank()) {
            throw new IllegalArgumentException("A library symbol has a name: 'Device:R'.");
        }
        return new KiCadSymbolBuilder(libraryId);
    }

    /** A power symbol: its value names the net it is, and it is no part of the board. */
    public KiCadSymbolBuilder power() {
        power = true;
        return this;
    }

    public KiCadSymbolBuilder property(String name, String value) {
        properties.put(name, value == null ? "" : value);
        return this;
    }

    public KiCadSymbolBuilder hidePinNames() {
        pinNamesHidden = true;
        return this;
    }

    public KiCadSymbolBuilder hidePinNumbers() {
        pinNumbersHidden = true;
        return this;
    }

    /** A line through the points, open; close it by repeating the first. */
    public KiCadSymbolBuilder polyline(int unit, List<KiCadPoint> points) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("A polyline runs through two points at least.");
        }
        List<KiCadNode> through = new ArrayList<>();
        points.forEach(point -> through.add(point(point)));
        return graphic(unit, KiCadList.of("polyline", KiCadList.of("pts", through.toArray(KiCadNode[]::new)),
                stroke(), fill()));
    }

    public KiCadSymbolBuilder rectangle(int unit, KiCadPoint start, KiCadPoint end) {
        return graphic(unit, KiCadList.of("rectangle", KiCadList.of("start", number(start.x()), number(start.y())),
                KiCadList.of("end", number(end.x()), number(end.y())), stroke(), fill()));
    }

    public KiCadSymbolBuilder circle(int unit, KiCadPoint centre, double radius) {
        return graphic(unit, KiCadList.of("circle", KiCadList.of("center", number(centre.x()), number(centre.y())),
                KiCadList.of("radius", number(radius)), stroke(), fill()));
    }

    /**
     * A pin: where a wire meets it, which way it points from there into the body ({@code 0} right, {@code 90} up,
     * {@code 180} left, {@code 270} down), how long it is drawn, and its name ({@code ~} for none) and number.
     */
    public KiCadSymbolBuilder pin(int unit, KiCadPinType type, KiCadPoint at, int angle, double length, String name,
                                  String number) {
        pins.computeIfAbsent(unit, ignored -> new ArrayList<>()).add(KiCadList.of("pin", KiCadAtom.of(type.word()),
                KiCadAtom.of("line"), KiCadList.of("at", number(at.x()), number(at.y()), KiCadAtom.of(angle)),
                KiCadList.of("length", number(length)),
                KiCadList.of("name", KiCadString.of(name == null || name.isBlank() ? "~" : name), effects(false)),
                KiCadList.of("number", KiCadString.of(number), effects(false))));
        return this;
    }

    /** The symbol's tree, as KiCad 7 writes it inside {@code lib_symbols}. */
    public KiCadList build() {
        List<KiCadNode> items = new ArrayList<>();
        items.add(KiCadString.of(libraryId));
        if (power) {
            items.add(KiCadList.of("power"));
        }
        if (pinNumbersHidden) {
            items.add(KiCadList.of("pin_numbers", KiCadAtom.of("hide")));
        }
        items.add(pinNamesHidden ? KiCadList.of("pin_names", KiCadList.of("offset", KiCadAtom.of(0)), KiCadAtom.of("hide"))
                : KiCadList.of("pin_names", KiCadList.of("offset", KiCadAtom.of(0.254))));
        items.add(KiCadList.of("in_bom", KiCadAtom.of(power ? "no" : "yes")));
        items.add(KiCadList.of("on_board", KiCadAtom.of(power ? "no" : "yes")));
        properties.forEach((name, value) -> items.add(KiCadList.of("property", KiCadString.of(name), KiCadString.of(value),
                KiCadList.of("at", KiCadAtom.of(0), KiCadAtom.of(0), KiCadAtom.of(0)),
                effects(!name.equals("Reference") && !name.equals("Value")))));
        // A unit's body and its pins are two children, as KiCad writes them: NAME_unit_1 holds both here.
        String bare = libraryId.substring(libraryId.indexOf(':') + 1);
        TreeMap<Integer, List<KiCadNode>> units = new TreeMap<>();
        graphics.forEach((unit, drawn) -> units.computeIfAbsent(unit, ignored -> new ArrayList<>()).addAll(drawn));
        pins.forEach((unit, placed) -> units.computeIfAbsent(unit, ignored -> new ArrayList<>()).addAll(placed));
        units.forEach((unit, drawn) -> {
            List<KiCadNode> child = new ArrayList<>();
            child.add(KiCadString.of(bare + "_" + unit + "_1"));
            child.addAll(drawn);
            items.add(KiCadList.of("symbol", child.toArray(KiCadNode[]::new)));
        });
        return KiCadList.of("symbol", items.toArray(KiCadNode[]::new));
    }

    private KiCadSymbolBuilder graphic(int unit, KiCadList graphic) {
        graphics.computeIfAbsent(unit, ignored -> new ArrayList<>()).add(graphic);
        return this;
    }

    private static KiCadList point(KiCadPoint point) {
        return KiCadList.of("xy", number(point.x()), number(point.y()));
    }

    private static KiCadAtom number(double value) {
        return KiCadAtom.of(value);
    }

    private static KiCadList stroke() {
        return KiCadList.of("stroke", KiCadList.of("width", KiCadAtom.of(0)), KiCadList.of("type", KiCadAtom.of("default")));
    }

    private static KiCadList fill() {
        return KiCadList.of("fill", KiCadList.of("type", KiCadAtom.of("none")));
    }

    /** How a text is drawn: KiCad's size, and hidden when it is not shown on the sheet. */
    static KiCadList effects(boolean hidden) {
        KiCadList font = KiCadList.of("font", KiCadList.of("size", KiCadAtom.of(TEXT_SIZE), KiCadAtom.of(TEXT_SIZE)));
        return hidden ? KiCadList.of("effects", font, KiCadAtom.of("hide")) : KiCadList.of("effects", font);
    }
}
