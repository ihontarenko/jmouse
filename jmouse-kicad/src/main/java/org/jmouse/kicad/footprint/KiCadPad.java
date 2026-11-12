package org.jmouse.kicad.footprint;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.sexpression.node.SExpressionAtom;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionNode;
import org.jmouse.sexpression.node.SExpressionString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A pad of a footprint, in millimetres from the footprint's origin, Y downwards as on the board.
 *
 * @param number    what the package calls it — {@code 1}, {@code A3}, {@code EP}; empty for a mounting hole
 * @param angle     degrees it is turned, counter-clockwise as KiCad turns it
 * @param drill     the hole's width and height (an oval hole has two); empty for a pad on the surface
 * @param roundness a rounded rectangle's corner radius as a share of its smaller side; 0 otherwise
 * @param layers    the layers it is on: {@code F.Cu}, {@code *.Cu}, {@code F.Mask} …
 * @param primitives a custom pad's own outline, drawn about its centre and turned with it; none for any other shape
 */
public record KiCadPad(String number, KiCadPadType type, KiCadPadShape shape, KiCadPoint at, double angle, double width,
                       double height, Optional<double[]> drill, double roundness, List<String> layers,
                       Optional<Double> maskMargin, Optional<Double> pasteMargin, List<KiCadFootprintGraphic> primitives) {

    /** What a pad's net is written as when it carries none: no list at all. */
    public static final String NO_NET = "";

    public KiCadPad {
        layers = List.copyOf(layers);
        primitives = primitives == null ? List.of() : List.copyOf(primitives);
    }

    /** A pad as a footprint writes it: {@code (pad "1" thru_hole rect (at 0 0) (size 1.9 2) (drill 1.1) (layers …))}. */
    static KiCadPad of(SExpressionList pad) {
        SExpressionList at = pad.requireList(AT);
        SExpressionList size = pad.requireList(SIZE);
        Optional<double[]> drill = pad.list(DRILL).map(KiCadPad::drillOf);
        double roundness = pad.list(ROUNDNESS).map(ratio -> ratio.number(1)).orElse(0.0);
        List<String> layers = pad.list(LAYERS).map(KiCadPad::wordsOf).orElse(List.of());
        return new KiCadPad(pad.word(1), KiCadPadType.of(pad.word(2)), KiCadPadShape.of(pad.word(3)), KiCadPoint.of(at),
                at.size() > 3 ? at.number(3) : 0, size.number(1), size.number(2), drill, roundness, layers,
                pad.list(MASK_MARGIN).map(margin -> margin.number(1)), pasteMarginOf(pad, Math.min(size.number(1), size.number(2))),
                primitivesOf(pad));
    }

    /** A custom pad's {@code (primitives (gr_poly …) …)}: the shapes its copper is, about its centre. */
    private static List<KiCadFootprintGraphic> primitivesOf(SExpressionList pad) {
        List<KiCadFootprintGraphic> primitives = new ArrayList<>();
        pad.list(PRIMITIVES).ifPresent(written -> written.items().forEach(item -> {
            if (item instanceof SExpressionList shape) {
                KiCadFootprintGraphic.of(shape).ifPresent(primitives::add);
            }
        }));
        return primitives;
    }

    /**
     * The pad as a footprint writes it, the list {@link #of} reads back: on a board it carries its net, numbered as the board
     * numbers its nets.
     *
     * @param netNumber the board's number of its net; 0 with {@link #NO_NET} for a pad of a library footprint or on no net
     */
    public SExpressionList written(int netNumber, String netName, String identity) {
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionString.of(number));
        items.add(SExpressionAtom.of(type.word()));
        items.add(SExpressionAtom.of(shape.word()));
        items.add(angle == 0 ? SExpressionList.of(AT, SExpressionAtom.of(at.x()), SExpressionAtom.of(at.y()))
                : SExpressionList.of(AT, SExpressionAtom.of(at.x()), SExpressionAtom.of(at.y()), SExpressionAtom.of(angle)));
        items.add(SExpressionList.of(SIZE, SExpressionAtom.of(width), SExpressionAtom.of(height)));
        drill.ifPresent(hole -> items.add(hole[0] == hole[1] ? SExpressionList.of(DRILL, SExpressionAtom.of(hole[0]))
                : SExpressionList.of(DRILL, SExpressionAtom.of(OVAL_DRILL), SExpressionAtom.of(hole[0]), SExpressionAtom.of(hole[1]))));
        items.add(SExpressionList.of(LAYERS, layers.stream().map(SExpressionString::of).toArray(SExpressionNode[]::new)));
        if (shape == KiCadPadShape.ROUNDED_RECTANGLE) {
            items.add(SExpressionList.of(ROUNDNESS, SExpressionAtom.of(roundness)));
        }
        maskMargin.ifPresent(margin -> items.add(SExpressionList.of(MASK_MARGIN, SExpressionAtom.of(margin))));
        pasteMargin.ifPresent(margin -> items.add(SExpressionList.of(PASTE_MARGIN, SExpressionAtom.of(margin))));
        if (!netName.isEmpty()) {
            items.add(SExpressionList.of(NET, SExpressionAtom.of(netNumber), SExpressionString.of(netName)));
        }
        if (!primitives.isEmpty()) {
            // The anchor is the pad's least shape; its copper is the primitives', drawn whole over it.
            items.add(SExpressionList.of(PAD_OPTIONS, SExpressionList.of(CLEARANCE, SExpressionAtom.of("outline")),
                    SExpressionList.of(ANCHOR, SExpressionAtom.of(KiCadPadShape.CIRCLE.word()))));
            items.add(SExpressionList.of(PRIMITIVES, primitives.stream().map(primitive -> primitive.written(true, null))
                    .toArray(SExpressionNode[]::new)));
        }
        if (identity != null) {
            items.add(SExpressionList.of(UUID, SExpressionString.of(identity)));
        }
        return SExpressionList.of(PAD, items.toArray(SExpressionNode[]::new));
    }

    /** The word an oblong hole's drill opens with. */
    private static final String OVAL_DRILL = "oval";

    /** What its paste grows by: {@code solder_paste_margin}, and {@code solder_paste_margin_ratio} of its shorter side. */
    private static Optional<Double> pasteMarginOf(SExpressionList pad, double shorterSide) {
        Optional<Double> margin = pad.list(PASTE_MARGIN).map(said -> said.number(1));
        Optional<Double> ratio = pad.list(PASTE_RATIO).map(said -> said.number(1));
        return margin.isEmpty() && ratio.isEmpty() ? Optional.empty()
                : Optional.of(margin.orElse(0.0) + ratio.orElse(0.0) * shorterSide);
    }

    /** {@code (drill 1.1)}, {@code (drill oval 1.2 2.4)}, {@code (drill 1.1 (offset …))}: the hole's width and height. */
    private static double[] drillOf(SExpressionList drill) {
        List<Double> numbers = new ArrayList<>();
        for (SExpressionNode item : drill.items().subList(1, drill.items().size())) {
            if (item instanceof SExpressionAtom atom && atom.isNumber()) {
                numbers.add(atom.number());
            }
        }
        double width = numbers.isEmpty() ? 0 : numbers.getFirst();
        double height = numbers.size() > 1 ? numbers.get(1) : width;
        return new double[] {width, height};
    }

    private static List<String> wordsOf(SExpressionList list) {
        List<String> words = new ArrayList<>();
        for (SExpressionNode item : list.items().subList(1, list.items().size())) {
            if (item instanceof SExpressionString text) {
                words.add(text.text());
            } else if (item instanceof SExpressionAtom atom) {
                words.add(atom.value());
            }
        }
        return words;
    }

    /** Whether one of its layers ends with this: {@code .Mask} of {@code *.Mask} and {@code F.Mask}. */
    public boolean isOn(String ending) {
        return layers.stream().anyMatch(layer -> layer.endsWith(ending));
    }

    /** Whether it is copper on the board: every pad but a bare hole. */
    public boolean copper() {
        return type != KiCadPadType.UNPLATED_HOLE;
    }
}
