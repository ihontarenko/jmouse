package org.jmouse.kicad.footprint;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.parser.KiCadDocument;
import org.jmouse.kicad.parser.node.KiCadList;
import org.jmouse.kicad.parser.node.KiCadNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A footprint — a {@code .kicad_mod} file, one part's land pattern —: its pads and the lines it draws on the board's
 * layers, in millimetres from its origin, Y downwards. Its 3D model, texts and keep-outs are not read.
 *
 * @param name        the footprint's own name, {@code TO-220-3_Vertical}
 * @param description KiCad's {@code descr}
 * @param tags        KiCad's {@code tags}, split at spaces
 * @param properties  Reference, Value, Datasheet … as written
 */
public record KiCadFootprint(String name, String description, List<String> tags, Map<String, String> properties,
                             List<KiCadPad> pads, List<KiCadFootprintGraphic> graphics) {

    public KiCadFootprint {
        tags = List.copyOf(tags);
        properties = Map.copyOf(properties);
        pads = List.copyOf(pads);
        graphics = List.copyOf(graphics);
    }

    /**
     * The footprint a document holds.
     *
     * @throws IllegalArgumentException when it is not a footprint
     */
    public static KiCadFootprint of(KiCadDocument document) {
        if (!document.root().is(FOOTPRINT)) {
            throw new IllegalArgumentException(document.name() + " is a " + document.kind() + ", not a footprint.");
        }
        return of(document.root());
    }

    /** A {@code (footprint "NAME" …)} list. */
    public static KiCadFootprint of(KiCadList footprint) {
        Map<String, String> properties = new LinkedHashMap<>();
        footprint.lists(PROPERTY).forEach(property -> properties.put(property.word(1), property.word(2)));
        List<KiCadPad> pads = new ArrayList<>();
        footprint.lists(PAD).forEach(pad -> pads.add(KiCadPad.of(pad)));
        List<KiCadFootprintGraphic> graphics = new ArrayList<>();
        for (KiCadNode item : footprint.items()) {
            if (item instanceof KiCadList list) {
                KiCadFootprintGraphic.of(list).ifPresent(graphics::add);
            }
        }
        String tags = footprint.wordOf(TAGS).orElse("");
        return new KiCadFootprint(footprint.word(1), footprint.wordOf(DESCRIPTION).orElse(""),
                tags.isBlank() ? List.of() : List.of(tags.trim().split("\\s+")), properties, pads, graphics);
    }

    /** The numbers its copper pads carry, each once, in the order a package counts them. */
    public Set<String> padNumbers() {
        Set<String> numbers = new TreeSet<>(KiCadFootprint::packageOrder);
        pads.stream().filter(KiCadPad::copper).map(KiCadPad::number).filter(number -> !number.isEmpty())
                .forEach(numbers::add);
        return numbers;
    }

    /** Numbers by their value, a grid name (A3) after them, by its letters. */
    private static int packageOrder(String first, String second) {
        boolean firstNumeric = first.matches("\\d+");
        boolean secondNumeric = second.matches("\\d+");
        if (firstNumeric && secondNumeric) {
            return Long.compare(Long.parseLong(first), Long.parseLong(second));
        }
        if (firstNumeric != secondNumeric) {
            return firstNumeric ? -1 : 1;
        }
        return first.compareTo(second);
    }
}
