package org.jmouse.kicad.symbol;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.parser.node.KiCadList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A symbol as a library defines it — or as a schematic keeps a copy of it in its {@code lib_symbols}: its name, its
 * properties, its units and their pins. A unit's drawing lives in sub-symbols named {@code NAME_UNIT_STYLE}: unit 0 is
 * every unit's, style 1 the normal body and 2 the De Morgan one, whose pins are the same pins drawn otherwise.
 *
 * @param name       {@code Device:R} in a schematic's copy, {@code R} in a library
 * @param extendsName the symbol it derives from in its library, or empty
 * @param power      whether it is a power symbol: its pin's net is named by its value
 * @param properties Reference, Value, Footprint, Datasheet, Description … as written
 * @param pins       every pin of every unit, the normal body's
 * @param tree       the list it was read from, for a consumer that wants its drawing
 */
public record KiCadSymbolDefinition(String name, String extendsName, boolean power, Map<String, String> properties,
                                    List<KiCadPin> pins, KiCadList tree) {

    private static final Pattern UNIT = Pattern.compile(".*_(\\d+)_(\\d+)$");
    private static final int DE_MORGAN = 2;

    public KiCadSymbolDefinition {
        properties = Map.copyOf(properties);
        pins = List.copyOf(pins);
    }

    /** A symbol list: {@code (symbol "Device:R" … (symbol "R_1_1" (pin …)))}. */
    public static KiCadSymbolDefinition of(KiCadList symbol) {
        Map<String, String> properties = new LinkedHashMap<>();
        symbol.lists(PROPERTY).forEach(property -> properties.put(property.word(1), property.word(2)));
        List<KiCadPin> pins = new ArrayList<>();
        for (KiCadList body : symbol.lists(SYMBOL)) {
            Matcher unit = UNIT.matcher(body.word(1));
            if (unit.matches() && Integer.parseInt(unit.group(2)) == DE_MORGAN) {
                continue;
            }
            int number = unit.matches() ? Integer.parseInt(unit.group(1)) : 0;
            body.lists(PIN).forEach(pin -> pins.add(KiCadPin.of(pin, number)));
        }
        return new KiCadSymbolDefinition(symbol.word(1), symbol.wordOf(EXTENDS).orElse(""),
                symbol.list(POWER).isPresent(), properties, pins, symbol);
    }

    /** The pins a unit of it has: its own and every unit's. */
    public List<KiCadPin> pinsOf(int unit) {
        return pins.stream().filter(pin -> pin.unit() == 0 || pin.unit() == unit).toList();
    }

    public String property(String name) {
        return properties.getOrDefault(name, "");
    }

    /** The same symbol derived from its parent: the parent's units and pins, its own properties over the parent's. */
    KiCadSymbolDefinition derivedFrom(KiCadSymbolDefinition parent) {
        Map<String, String> merged = new LinkedHashMap<>(parent.properties);
        merged.putAll(properties);
        return new KiCadSymbolDefinition(name, extendsName, parent.power, merged, pins.isEmpty() ? parent.pins : pins, tree);
    }
}
