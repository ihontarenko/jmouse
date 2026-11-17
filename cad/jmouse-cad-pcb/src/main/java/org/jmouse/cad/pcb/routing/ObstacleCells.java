package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.geometry.CopperShape;

import java.util.BitSet;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Pieces of copper by the cells of a grid over the board, each by its number: what stands near a box is the pieces of the cells
 * the box reaches, not every piece of the board. Laid out flat — for each cell where its numbers start in one array of numbers —,
 * so an index costs two arrays of ints however many cells a pour covers; and made once for a layer of a board, every fence over
 * that layer asking it with gaps of its own.
 */
final class ObstacleCells {

    /** The most cells a grid is laid out in: a board with copper far apart is indexed in wider cells rather than in more of them. */
    private static final long MOST_CELLS = 1L << 20;

    private final int    count;
    private final double cell;
    private final long   firstColumn;
    private final long   firstRow;
    private final int    columns;
    private final int    rows;
    private final int[]  starts;
    private final int[]  numbers;

    /** @param bucket how wide a cell is, in millimetres — wider where the board would take more than {@link #MOST_CELLS} */
    ObstacleCells(List<CopperShape> shapes, double bucket) {
        this.count = shapes.size();
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (CopperShape shape : shapes) {
            left = Math.min(left, shape.box().left());
            top = Math.min(top, shape.box().top());
            right = Math.max(right, shape.box().right());
            bottom = Math.max(bottom, shape.box().bottom());
        }
        if (shapes.isEmpty()) {
            left = top = right = bottom = 0;
        }
        double width = bucket;
        while ((Math.floor(right / width) - Math.floor(left / width) + 1) * (Math.floor(bottom / width) - Math.floor(top / width) + 1) > MOST_CELLS) {
            width *= 2;
        }
        this.cell = width;
        this.firstColumn = cellOf(left);
        this.firstRow = cellOf(top);
        this.columns = (int) (cellOf(right) - firstColumn + 1);
        this.rows = (int) (cellOf(bottom) - firstRow + 1);
        // Counted first, then each cell's numbers written where its count put them.
        this.starts = new int[columns * rows + 1];
        for (CopperShape shape : shapes) {
            forEachCell(shape.box(), place -> starts[place + 1]++);
        }
        for (int place = 0; place < columns * rows; place++) {
            starts[place + 1] += starts[place];
        }
        this.numbers = new int[starts[columns * rows]];
        int[] written = new int[columns * rows];
        for (int number = 0; number < shapes.size(); number++) {
            int held = number;
            forEachCell(shapes.get(number).box(), place -> numbers[starts[place] + written[place]++] = held);
        }
    }

    /** How many pieces it indexes. */
    int count() {
        return count;
    }

    /** The numbers of the pieces whose cells a box reaches; every number where the box covers more cells than there are pieces. */
    BitSet near(double left, double top, double right, double bottom) {
        BitSet found = new BitSet(count);
        long fromColumn = cellOf(left);
        long toColumn = cellOf(right);
        long fromRow = cellOf(top);
        long toRow = cellOf(bottom);
        if ((toColumn - fromColumn + 1) * (toRow - fromRow + 1) > count) {
            found.set(0, count);
            return found;
        }
        for (long column = Math.max(fromColumn, firstColumn); column <= Math.min(toColumn, firstColumn + columns - 1); column++) {
            for (long row = Math.max(fromRow, firstRow); row <= Math.min(toRow, firstRow + rows - 1); row++) {
                int place = placeOf(column, row);
                for (int at = starts[place]; at < starts[place + 1]; at++) {
                    found.set(numbers[at]);
                }
            }
        }
        return found;
    }

    private void forEachCell(CopperShape.Box box, IntConsumer visit) {
        for (long column = cellOf(box.left()); column <= cellOf(box.right()); column++) {
            for (long row = cellOf(box.top()); row <= cellOf(box.bottom()); row++) {
                visit.accept(placeOf(column, row));
            }
        }
    }

    private int placeOf(long column, long row) {
        return (int) ((row - firstRow) * columns + (column - firstColumn));
    }

    /** The column — or the row: the grid is square — a coordinate falls in. */
    private long cellOf(double coordinate) {
        return (long) Math.floor(coordinate / cell);
    }
}
