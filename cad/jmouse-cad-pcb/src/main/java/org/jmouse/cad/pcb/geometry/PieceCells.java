package org.jmouse.cad.pcb.geometry;

import org.jmouse.core.collections.LongObjectMap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * The pieces of a large shape by the cells of a grid over it — a pour's outline, hundreds of pieces —, so a question about what
 * stands near a small box walks the pieces of the cells that box reaches, not every piece of the shape. A piece is in every cell
 * its own box reaches; a cell is about as wide as the shape divided by the root of its pieces, so a cell holds a few.
 */
final class PieceCells {

    /** The scale a cell is keyed by, so a column and a row make one key. */
    private static final long CELLS = 1L << 31;

    private final List<double[]>               pieces;
    private final double                       left;
    private final double                       top;
    private final double                       cell;
    private final LongObjectMap<List<Integer>> cells = new LongObjectMap<>();

    PieceCells(List<double[]> pieces) {
        this.pieces = pieces;
        double minimumX = Double.POSITIVE_INFINITY;
        double minimumY = Double.POSITIVE_INFINITY;
        double maximumX = Double.NEGATIVE_INFINITY;
        double maximumY = Double.NEGATIVE_INFINITY;
        for (double[] piece : pieces) {
            minimumX = Math.min(minimumX, Math.min(piece[0], piece[2]));
            minimumY = Math.min(minimumY, Math.min(piece[1], piece[3]));
            maximumX = Math.max(maximumX, Math.max(piece[0], piece[2]));
            maximumY = Math.max(maximumY, Math.max(piece[1], piece[3]));
        }
        this.left = minimumX;
        this.top = minimumY;
        double side = Math.max(maximumX - minimumX, maximumY - minimumY);
        this.cell = side > 0 ? side / Math.max(1, Math.sqrt(pieces.size())) : 1;
        for (int index = 0; index < pieces.size(); index++) {
            double[] piece = pieces.get(index);
            for (long column = columnOf(Math.min(piece[0], piece[2])); column <= columnOf(Math.max(piece[0], piece[2])); column++) {
                for (long row = rowOf(Math.min(piece[1], piece[3])); row <= rowOf(Math.max(piece[1], piece[3])); row++) {
                    cells.computeIfAbsent(column * CELLS + row, key -> new ArrayList<>()).add(index);
                }
            }
        }
    }

    /**
     * Whether any piece whose cell a box reaches answers yes — each piece asked once —: the pieces that may stand within the box,
     * and none of those farther off.
     */
    boolean anyNear(double boxLeft, double boxTop, double boxRight, double boxBottom, IntPredicate question) {
        long columns = columnOf(boxRight) - columnOf(boxLeft) + 1;
        long rows = rowOf(boxBottom) - rowOf(boxTop) + 1;
        // A box over more cells than there are pieces is asked of every piece: walking the cells would cost more.
        if (columns * rows > pieces.size()) {
            for (int index = 0; index < pieces.size(); index++) {
                if (question.test(index)) {
                    return true;
                }
            }
            return false;
        }
        BitSet asked = new BitSet(pieces.size());
        for (long column = columnOf(boxLeft); column <= columnOf(boxRight); column++) {
            for (long row = rowOf(boxTop); row <= rowOf(boxBottom); row++) {
                List<Integer> held = cells.get(column * CELLS + row);
                if (held == null) {
                    continue;
                }
                for (int index : held) {
                    if (!asked.get(index)) {
                        asked.set(index);
                        if (question.test(index)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private long columnOf(double x) {
        return (long) Math.floor((x - left) / cell);
    }

    private long rowOf(double y) {
        return (long) Math.floor((y - top) / cell);
    }
}
