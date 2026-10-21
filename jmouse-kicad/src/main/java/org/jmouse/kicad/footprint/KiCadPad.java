package org.jmouse.kicad.footprint;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.parser.node.KiCadAtom;
import org.jmouse.kicad.parser.node.KiCadList;
import org.jmouse.kicad.parser.node.KiCadNode;
import org.jmouse.kicad.parser.node.KiCadString;

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
 */
public record KiCadPad(String number, KiCadPadType type, KiCadPadShape shape, KiCadPoint at, double angle, double width,
                       double height, Optional<double[]> drill, double roundness, List<String> layers) {

    public KiCadPad {
        layers = List.copyOf(layers);
    }

    /** A pad as a footprint writes it: {@code (pad "1" thru_hole rect (at 0 0) (size 1.9 2) (drill 1.1) (layers …))}. */
    static KiCadPad of(KiCadList pad) {
        KiCadList at = pad.requireList(AT);
        KiCadList size = pad.requireList(SIZE);
        Optional<double[]> drill = pad.list(DRILL).map(KiCadPad::drillOf);
        double roundness = pad.list(ROUNDNESS).map(ratio -> ratio.number(1)).orElse(0.0);
        List<String> layers = pad.list(LAYERS).map(KiCadPad::wordsOf).orElse(List.of());
        return new KiCadPad(pad.word(1), KiCadPadType.of(pad.word(2)), KiCadPadShape.of(pad.word(3)), KiCadPoint.of(at),
                at.size() > 3 ? at.number(3) : 0, size.number(1), size.number(2), drill, roundness, layers);
    }

    /** {@code (drill 1.1)}, {@code (drill oval 1.2 2.4)}, {@code (drill 1.1 (offset …))}: the hole's width and height. */
    private static double[] drillOf(KiCadList drill) {
        List<Double> numbers = new ArrayList<>();
        for (KiCadNode item : drill.items().subList(1, drill.items().size())) {
            if (item instanceof KiCadAtom atom && atom.isNumber()) {
                numbers.add(atom.number());
            }
        }
        double width = numbers.isEmpty() ? 0 : numbers.getFirst();
        double height = numbers.size() > 1 ? numbers.get(1) : width;
        return new double[] {width, height};
    }

    private static List<String> wordsOf(KiCadList list) {
        List<String> words = new ArrayList<>();
        for (KiCadNode item : list.items().subList(1, list.items().size())) {
            if (item instanceof KiCadString text) {
                words.add(text.text());
            } else if (item instanceof KiCadAtom atom) {
                words.add(atom.value());
            }
        }
        return words;
    }

    /** Whether it is copper on the board: every pad but a bare hole. */
    public boolean copper() {
        return type != KiCadPadType.UNPLATED_HOLE;
    }
}
