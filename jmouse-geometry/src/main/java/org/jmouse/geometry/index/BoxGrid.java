package org.jmouse.geometry.index;

import java.util.BitSet;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Boxes by the square cells of a grid over them: an item is in every cell its box reaches, and what comes near a box is the items of the
 * cells that box reaches. Laid out flat — for each cell where its numbers start in one array of numbers —, so an index costs two arrays
 * of ints however many cells there are. The index for items crowded edge to edge — a layer's copper, a pour's outline —, where a cell
 * answers with no tree to walk. A box over more cells than there are items is answered with every item: walking the cells would cost
 * more.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class BoxGrid implements BoxIndex {

    /**
     * How a grid is cut: how wide a cell is, and the most cells it may take — its cells widened, twice at a time, where the boxes' span
     * would take more.
     */
    public record Cells(double width, long most) {
    }

    private final int    count;
    private final double cell;
    private final long   firstColumn;
    private final long   firstRow;
    private final int    columns;
    private final int    rows;
    private final int[]  starts;
    private final int[]  numbers;

    private BoxGrid(List<Box> boxes, double cellWidth, long mostCells) {
        this.count = boxes.size();
        Box span = spanOf(boxes);
        double width = cellWidth > 0 ? cellWidth : 1;
        while (cellsAcross(span.left(), span.right(), width) * cellsAcross(span.top(), span.bottom(), width) > Math.max(1, mostCells)) {
            width *= 2;
        }
        this.cell = width;
        this.firstColumn = cellOf(span.left());
        this.firstRow = cellOf(span.top());
        this.columns = (int) (cellOf(span.right()) - firstColumn + 1);
        this.rows = (int) (cellOf(span.bottom()) - firstRow + 1);
        // Counted first, then each cell's numbers written where its count put them.
        this.starts = new int[columns * rows + 1];
        for (Box box : boxes) {
            forEachCell(box, place -> starts[place + 1]++);
        }
        for (int place = 0; place < columns * rows; place++) {
            starts[place + 1] += starts[place];
        }
        this.numbers = new int[starts[columns * rows]];
        int[] written = new int[columns * rows];
        for (int number = 0; number < boxes.size(); number++) {
            int held = number;
            forEachCell(boxes.get(number), place -> numbers[starts[place] + written[place]++] = held);
        }
    }

    /**
     * The boxes in cells of a width, each known by its place in the list; the cells widened, twice at a time, where the boxes' span
     * would take more than {@code mostCells}.
     */
    public static BoxGrid of(List<Box> boxes, double cellWidth, long mostCells) {
        return new BoxGrid(boxes, cellWidth, mostCells);
    }

    /** The boxes in cells cut as asked, each known by its place in the list. */
    public static BoxGrid of(List<Box> boxes, Cells cells) {
        return new BoxGrid(boxes, cells.width(), cells.most());
    }

    /** The boxes in about as many cells as there are boxes, so a cell holds a few: for items spread evenly over their span. */
    public static BoxGrid even(List<Box> boxes) {
        Box span = spanOf(boxes);
        double side = Math.max(span.width(), span.height());
        return new BoxGrid(boxes, side > 0 ? side / Math.max(1, Math.sqrt(boxes.size())) : 1, Math.max(1, boxes.size()));
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public int[] near(Box box) {
        return nearSet(box).stream().toArray();
    }

    @Override
    public BitSet nearSet(Box box) {
        BitSet found = new BitSet(count);
        long fromColumn = cellOf(box.left());
        long toColumn = cellOf(box.right());
        long fromRow = cellOf(box.top());
        long toRow = cellOf(box.bottom());
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

    /** The box holding every box; one of no size at the origin where there are none. */
    private static Box spanOf(List<Box> boxes) {
        if (boxes.isEmpty()) {
            return Box.point(0, 0);
        }
        Box span = boxes.getFirst();
        for (Box box : boxes) {
            span = span.joined(box);
        }
        return span;
    }

    private static double cellsAcross(double from, double to, double width) {
        return Math.floor(to / width) - Math.floor(from / width) + 1;
    }

    private void forEachCell(Box box, IntConsumer visit) {
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
