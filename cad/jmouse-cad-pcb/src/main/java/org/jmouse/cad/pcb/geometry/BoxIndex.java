package org.jmouse.cad.pcb.geometry;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

import java.util.Arrays;
import java.util.List;

/**
 * Pieces of copper by their boxes, indexed once, so what stands near a box is found among the few whose boxes come near it — not
 * every piece of the pcb asked of every other. The pieces are known by their place in the list indexed; those near a box come in that
 * order, so what is done with them is done in the order it would be over the whole list.
 */
public final class BoxIndex {

    private final STRtree index = new STRtree();

    private BoxIndex(List<CopperShape.Box> boxes) {
        for (int number = 0; number < boxes.size(); number++) {
            CopperShape.Box box = boxes.get(number);
            index.insert(new Envelope(box.left(), box.right(), box.top(), box.bottom()), number);
        }
    }

    /** The boxes, each known by its place in the list. */
    public static BoxIndex of(List<CopperShape.Box> boxes) {
        return new BoxIndex(boxes);
    }

    /** The places of the boxes that come within a distance of a box, in order: those {@link CopperShape.Box#within} may say yes to. */
    public int[] near(CopperShape.Box box, double distance) {
        List<?> found = index.query(new Envelope(box.left() - distance, box.right() + distance, box.top() - distance, box.bottom() + distance));
        int[] numbers = new int[found.size()];
        for (int at = 0; at < numbers.length; at++) {
            numbers[at] = (Integer) found.get(at);
        }
        Arrays.sort(numbers);
        return numbers;
    }
}
