package org.jmouse.kicad.symbol;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.sexpression.node.SExpressionAtom;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionNode;
import org.jmouse.sexpression.node.SExpressionString;

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
 * SExpressionList resistor = KiCadSymbolBuilder.named("Device:R")
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
    private final Map<Integer, List<SExpressionNode>> graphics = new TreeMap<>();
    private final Map<Integer, List<SExpressionNode>> pins = new TreeMap<>();
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
        List<SExpressionNode> through = new ArrayList<>();
        points.forEach(point -> through.add(point(point)));
        return graphic(unit, SExpressionList.of("polyline", SExpressionList.of("pts", through.toArray(SExpressionNode[]::new)),
                stroke(), fill()));
    }

    public KiCadSymbolBuilder rectangle(int unit, KiCadPoint start, KiCadPoint end) {
        return graphic(unit, SExpressionList.of("rectangle", SExpressionList.of("start", number(start.x()), number(start.y())),
                SExpressionList.of("end", number(end.x()), number(end.y())), stroke(), fill()));
    }

    public KiCadSymbolBuilder circle(int unit, KiCadPoint centre, double radius) {
        return graphic(unit, SExpressionList.of("circle", SExpressionList.of("center", number(centre.x()), number(centre.y())),
                SExpressionList.of("radius", number(radius)), stroke(), fill()));
    }

    /**
     * A pin: where a wire meets it, which way it points from there into the body ({@code 0} right, {@code 90} up,
     * {@code 180} left, {@code 270} down), how long it is drawn, and its name ({@code ~} for none) and number.
     */
    public KiCadSymbolBuilder pin(int unit, KiCadPinType type, KiCadPoint at, int angle, double length, String name,
                                  String number) {
        pins.computeIfAbsent(unit, ignored -> new ArrayList<>()).add(SExpressionList.of("pin", SExpressionAtom.of(type.word()),
                SExpressionAtom.of("line"), SExpressionList.of("at", number(at.x()), number(at.y()), SExpressionAtom.of(angle)),
                SExpressionList.of("length", number(length)),
                SExpressionList.of("name", SExpressionString.of(name == null || name.isBlank() ? "~" : name), effects(false)),
                SExpressionList.of("number", SExpressionString.of(number), effects(false))));
        return this;
    }

    /** The symbol's tree, as KiCad 7 writes it inside {@code lib_symbols}. */
    public SExpressionList build() {
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionString.of(libraryId));
        if (power) {
            items.add(SExpressionList.of("power"));
        }
        if (pinNumbersHidden) {
            items.add(SExpressionList.of("pin_numbers", SExpressionAtom.of("hide")));
        }
        items.add(pinNamesHidden ? SExpressionList.of("pin_names", SExpressionList.of("offset", SExpressionAtom.of(0)), SExpressionAtom.of("hide"))
                : SExpressionList.of("pin_names", SExpressionList.of("offset", SExpressionAtom.of(0.254))));
        items.add(SExpressionList.of("in_bom", SExpressionAtom.of(power ? "no" : "yes")));
        items.add(SExpressionList.of("on_board", SExpressionAtom.of(power ? "no" : "yes")));
        properties.forEach((name, value) -> items.add(SExpressionList.of("property", SExpressionString.of(name), SExpressionString.of(value),
                SExpressionList.of("at", SExpressionAtom.of(0), SExpressionAtom.of(0), SExpressionAtom.of(0)),
                effects(!name.equals("Reference") && !name.equals("Value")))));
        // A unit's body and its pins are two children, as KiCad writes them: NAME_unit_1 holds both here.
        String bare = libraryId.substring(libraryId.indexOf(':') + 1);
        TreeMap<Integer, List<SExpressionNode>> units = new TreeMap<>();
        graphics.forEach((unit, drawn) -> units.computeIfAbsent(unit, ignored -> new ArrayList<>()).addAll(drawn));
        pins.forEach((unit, placed) -> units.computeIfAbsent(unit, ignored -> new ArrayList<>()).addAll(placed));
        units.forEach((unit, drawn) -> {
            List<SExpressionNode> child = new ArrayList<>();
            child.add(SExpressionString.of(bare + "_" + unit + "_1"));
            child.addAll(drawn);
            items.add(SExpressionList.of("symbol", child.toArray(SExpressionNode[]::new)));
        });
        return SExpressionList.of("symbol", items.toArray(SExpressionNode[]::new));
    }

    private KiCadSymbolBuilder graphic(int unit, SExpressionList graphic) {
        graphics.computeIfAbsent(unit, ignored -> new ArrayList<>()).add(graphic);
        return this;
    }

    private static SExpressionList point(KiCadPoint point) {
        return SExpressionList.of("xy", number(point.x()), number(point.y()));
    }

    private static SExpressionAtom number(double value) {
        return SExpressionAtom.of(value);
    }

    private static SExpressionList stroke() {
        return SExpressionList.of("stroke", SExpressionList.of("width", SExpressionAtom.of(0)), SExpressionList.of("type", SExpressionAtom.of("default")));
    }

    private static SExpressionList fill() {
        return SExpressionList.of("fill", SExpressionList.of("type", SExpressionAtom.of("none")));
    }

    /** How a text is drawn: KiCad's size, and hidden when it is not shown on the sheet. */
    static SExpressionList effects(boolean hidden) {
        SExpressionList font = SExpressionList.of("font", SExpressionList.of("size", SExpressionAtom.of(TEXT_SIZE), SExpressionAtom.of(TEXT_SIZE)));
        return hidden ? SExpressionList.of("effects", font, SExpressionAtom.of("hide")) : SExpressionList.of("effects", font);
    }
}
