package org.jmouse.kicad.footprint;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.sexpression.SExpressionDocument;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * A footprint — a {@code .kicad_mod} file, one part's land pattern —: its pads and the lines it draws on the board's
 * layers, in millimetres from its origin, Y downwards, where it prints the placed part's reference and value, and the 3D
 * models that show its body. Its other texts and keep-outs are not read.
 *
 * @param name        the footprint's own name, {@code TO-220-3_Vertical}
 * @param description KiCad's {@code descr}
 * @param tags        KiCad's {@code tags}, split at spaces
 * @param properties  Reference, Value, Datasheet … as written
 * @param fields      where its reference and value are printed
 * @param models      its 3D models, in KiCad's order
 */
public record KiCadFootprint(String name, String description, List<String> tags, Map<String, String> properties,
                             List<KiCadPad> pads, List<KiCadFootprintGraphic> graphics, List<KiCadFootprintField> fields,
                             List<KiCadModel> models) {

    public KiCadFootprint {
        tags = List.copyOf(tags);
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        pads = List.copyOf(pads);
        graphics = List.copyOf(graphics);
        fields = List.copyOf(fields);
        models = List.copyOf(models);
    }

    /**
     * The footprint a document holds.
     *
     * @throws IllegalArgumentException when it is not a footprint
     */
    public static KiCadFootprint of(SExpressionDocument document) {
        if (!document.root().is(FOOTPRINT)) {
            throw new IllegalArgumentException(document.name() + " is a " + document.kind() + ", not a footprint.");
        }
        return of(document.root());
    }

    /** A {@code (footprint "NAME" …)} list. */
    public static KiCadFootprint of(SExpressionList footprint) {
        Map<String, String> properties = new LinkedHashMap<>();
        footprint.lists(PROPERTY).forEach(property -> properties.put(property.word(1), property.word(2)));
        List<KiCadPad> pads = new ArrayList<>();
        footprint.lists(PAD).forEach(pad -> pads.add(KiCadPad.of(pad)));
        List<KiCadFootprintGraphic> graphics = new ArrayList<>();
        List<KiCadFootprintField> fields = new ArrayList<>();
        for (SExpressionNode item : footprint.items()) {
            if (item instanceof SExpressionList list) {
                KiCadFootprintGraphic.of(list).ifPresent(graphics::add);
                KiCadFootprintField.of(list).ifPresent(fields::add);
            }
        }
        String tags = footprint.wordOf(TAGS).orElse("");
        return new KiCadFootprint(footprint.word(1), footprint.wordOf(DESCRIPTION).orElse(""),
                tags.isBlank() ? List.of() : List.of(tags.trim().split("\\s+")), properties, pads, graphics, fields,
                footprint.lists(MODEL).stream().map(KiCadModel::of).toList());
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

    /** The same footprint with these fields besides its own: a part's, which a board copies onto the footprint it stands on. */
    public KiCadFootprint withProperties(Map<String, String> more) {
        Map<String, String> all = new LinkedHashMap<>(properties);
        all.putAll(more);
        return new KiCadFootprint(name, description, tags, all, pads, graphics, fields, models);
    }

    /**
     * How it is mounted, as KiCad's files of positions and its bill take it: {@code through_hole} where a plated hole of its
     * holds a lead, else {@code smd} where a pad of its has none; empty for one of no pads, a mark.
     */
    public Optional<String> mounting() {
        boolean through = pads.stream().anyMatch(pad -> pad.drill().isPresent() && pad.copper());
        boolean surface = pads.stream().anyMatch(pad -> pad.drill().isEmpty());
        return through ? Optional.of(THROUGH_HOLE) : surface ? Optional.of(SURFACE_MOUNT) : Optional.empty();
    }

    /** KiCad's words for how a footprint is mounted. */
    private static final String THROUGH_HOLE = "through_hole";
    private static final String SURFACE_MOUNT = "smd";
}
