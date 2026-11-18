package org.jmouse.geometry.index;

import java.util.BitSet;
import java.util.function.IntPredicate;

/**
 * Items known by their number — their place in the list indexed — and their boxes, indexed once, so what stands near a box is found
 * among the few whose boxes come near it rather than among them all. What it answers may hold a few more than come near — the items
 * of the cells a box reaches —, never fewer: a caller tests each it is given. Numbers come in ascending order, so what is done with them
 * is done in the order it would be over the whole list.
 * <p>
 * The question alone: how the boxes are kept is each implementation's own — a tree ({@link BoxTree}) for items scattered over a wide
 * field, a spatial hash of square cells ({@link BoxGrid}) for items crowded edge to edge —, and a caller holds an index by this type,
 * never by its implementation.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface BoxIndex {

    /** How many items it indexes. */
    int count();

    /** The numbers of the items that may come near a box, ascending. */
    int[] near(Box box);

    /** The numbers of the items that may come near a box, as a set to combine with others. */
    BitSet nearSet(Box box);

    /** Whether any item that may come near a box answers yes: each asked once, ascending, the asking stopped at the first yes. */
    default boolean anyNear(Box box, IntPredicate question) {
        for (int number : near(box)) {
            if (question.test(number)) {
                return true;
            }
        }
        return false;
    }

    /** The numbers of the items that may come within a distance of a box, ascending. */
    default int[] near(Box box, double distance) {
        return near(box.grown(distance));
    }
}
