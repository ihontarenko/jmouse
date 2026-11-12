package org.jmouse.kicad.pcb;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.sexpression.node.SExpressionList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A footprint as it stands on a board. KiCad keeps a copy of the footprint in the board, written the way it stands
 * there: on the back its pads and lines are already mirrored and on the back's layers, and each pad's angle is the angle
 * it has on the board. {@link #footprint()} undoes that, so what it answers is the footprint as a library holds it.
 *
 * @param libraryId the {@code LIBRARY:NAME} it was placed from
 * @param path      the symbol it stands for, {@code /sheet-uuid/symbol-uuid}; empty for one that only the board has
 * @param reference what it is called on this board, {@code R3}
 * @param at        where its origin stands, in millimetres, Y downwards
 * @param angle     degrees it is turned, counter-clockwise as seen from the front
 * @param padNets   the net each pad carries on the board, by pad number, as KiCad names the net
 * @param lettering where its reference is printed, in the footprint's own frame; empty when the board does not say
 */
public record KiCadPcbFootprint(String libraryId, Optional<String> path, String reference, KiCadSide side, KiCadPoint at,
                                double angle, KiCadFootprint footprint, Map<String, String> padNets,
                                Optional<Lettering> lettering) {

    /** Reference is what KiCad 8 names the property, and what KiCad 7 names the text. */
    private static final String REFERENCE_FIELD = "Reference";
    private static final String REFERENCE_TEXT = "reference";
    /** The library a footprint with no name of its own is said to be of. */
    private static final String OWN_LIBRARY = "Board";

    public KiCadPcbFootprint {
        padNets = Map.copyOf(padNets);
    }

    /** Where a reference is printed: in the footprint's frame, turned relative to the footprint. */
    public record Lettering(KiCadPoint at, double angle) {
    }

    /** A {@code (footprint "LIBRARY:NAME" …)} list of a board. */
    static KiCadPcbFootprint of(SExpressionList list) {
        SExpressionList at = list.requireList(AT);
        double angle = at.size() > 3 ? at.number(3) : 0;
        KiCadSide side = KiCadSide.ofLayer(list.wordOf(LAYER).orElse(KiCadSide.FRONT.copper()));
        KiCadFootprint written = KiCadFootprint.of(list);
        String reference = referenceOf(list);
        // KiCad keeps a footprint its library no longer names under no name at all, each one its own.
        String libraryId = written.name().isBlank() ? OWN_LIBRARY + ":" + reference : written.name();
        int separator = libraryId.indexOf(':');
        String name = separator < 0 ? libraryId : libraryId.substring(separator + 1);
        return new KiCadPcbFootprint(libraryId, list.wordOf(PATH), reference, side, KiCadPoint.of(at), angle,
                asInLibrary(written, name, side, angle), padNetsOf(list), letteringOf(list, side, angle));
    }

    /** Where a point of the footprint, as a library holds it, stands on the board. */
    public KiCadPoint onBoard(KiCadPoint point) {
        KiCadPoint written = side.unmirrored(point);
        double radians = Math.toRadians(angle);
        return new KiCadPoint(at.x() + written.x() * Math.cos(radians) + written.y() * Math.sin(radians),
                at.y() - written.x() * Math.sin(radians) + written.y() * Math.cos(radians));
    }

    /** Whether it stands for a symbol of the schematic. */
    public boolean ofSchematic() {
        return path.isPresent();
    }

    private static String referenceOf(SExpressionList list) {
        for (SExpressionList property : list.lists(PROPERTY)) {
            if (property.word(1).equals(REFERENCE_FIELD)) {
                return property.word(2);
            }
        }
        for (SExpressionList text : list.lists(FOOTPRINT_TEXT)) {
            if (text.word(1).equals(REFERENCE_TEXT)) {
                return text.word(2);
            }
        }
        return "";
    }

    private static Optional<Lettering> letteringOf(SExpressionList list, KiCadSide side, double angle) {
        Optional<SExpressionList> field = list.lists(PROPERTY).stream()
                .filter(property -> property.word(1).equals(REFERENCE_FIELD)).findFirst()
                .or(() -> list.lists(FOOTPRINT_TEXT).stream().filter(text -> text.word(1).equals(REFERENCE_TEXT)).findFirst());
        return field.flatMap(written -> written.list(AT)).map(at -> {
            double turned = at.size() > 3 ? at.number(3) : 0;
            return new Lettering(side.unmirrored(KiCadPoint.of(at)), turned - angle);
        });
    }

    /** Each pad's net; a number standing on several pads carries one net, and the first that names it answers. */
    private static Map<String, String> padNetsOf(SExpressionList list) {
        Map<String, String> nets = new LinkedHashMap<>();
        for (SExpressionList pad : list.lists(PAD)) {
            pad.list(NET).filter(net -> net.size() > 2).ifPresent(net -> nets.putIfAbsent(pad.word(1), net.word(2)));
        }
        return nets;
    }

    private static KiCadFootprint asInLibrary(KiCadFootprint written, String name, KiCadSide side, double angle) {
        List<KiCadPad> pads = written.pads().stream().map(pad -> asInLibrary(pad, side, angle)).toList();
        List<KiCadFootprintGraphic> graphics = written.graphics().stream().map(graphic -> asInLibrary(graphic, side)).toList();
        return new KiCadFootprint(name, written.description(), written.tags(), written.properties(), pads, graphics,
                written.fields(), written.models());
    }

    /** A front pad's angle had the footprint's added; a back pad's own angle was negated first. */
    private static KiCadPad asInLibrary(KiCadPad pad, KiCadSide side, double angle) {
        double own = side == KiCadSide.BACK ? angle - pad.angle() : pad.angle() - angle;
        return new KiCadPad(pad.number(), pad.type(), pad.shape(), side.unmirrored(pad.at()), ((own % 360) + 360) % 360,
                pad.width(), pad.height(), pad.drill(), pad.roundness(), pad.layers().stream().map(side::libraryLayer).toList(),
                pad.maskMargin(), pad.pasteMargin(),
                pad.primitives().stream().map(primitive -> asInLibrary(primitive, side)).toList());
    }

    private static KiCadFootprintGraphic asInLibrary(KiCadFootprintGraphic graphic, KiCadSide side) {
        return graphic.moved(side::unmirrored, side.libraryLayer(graphic.layer()));
    }

}
