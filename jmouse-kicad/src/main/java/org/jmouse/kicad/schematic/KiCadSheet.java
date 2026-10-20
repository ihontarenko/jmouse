package org.jmouse.kicad.schematic;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadProperty;
import org.jmouse.kicad.parser.node.KiCadList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A sheet placed on a sheet: the box that stands for a child schematic, and the pins it carries for the child's
 * hierarchical labels.
 *
 * @param uuid  its identity: with its parent's path, the path of the child's instance — {@code /root/this}
 * @param name  what the box is called ({@code Sheetname})
 * @param file  the child schematic's file, relative to this one ({@code Sheetfile})
 * @param at    its top-left corner
 * @param size  its width and height
 * @param pins  its pins: each joined to the child's hierarchical label of its name
 */
public record KiCadSheet(String uuid, String name, String file, KiCadPoint at, KiCadPoint size, List<Pin> pins) {

    /** A pin of the box: its name, the point it connects at. */
    public record Pin(String name, KiCadPoint at) {
    }

    public KiCadSheet {
        pins = List.copyOf(pins);
    }

    static KiCadSheet of(KiCadList sheet) {
        Map<String, String> properties = new LinkedHashMap<>();
        sheet.lists(PROPERTY).forEach(property -> properties.put(property.word(1), property.word(2)));
        List<Pin> pins = sheet.lists(PIN).stream()
                .map(pin -> new Pin(pin.word(1), KiCadPoint.of(pin.requireList(AT)))).toList();
        return new KiCadSheet(sheet.wordOf(UUID).orElse(""), KiCadProperty.SHEET_NAME.in(properties),
                KiCadProperty.SHEET_FILE.in(properties), KiCadPoint.of(sheet.requireList(AT)),
                KiCadPoint.of(sheet.requireList(SIZE)), pins);
    }
}
