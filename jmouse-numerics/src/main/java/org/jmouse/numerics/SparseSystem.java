package org.jmouse.numerics;

import java.util.Arrays;

/**
 * A square linear system assembled entry by entry and solved by sparse LU elimination — the way a circuit simulator
 * must solve one, because a circuit's rows hold a handful of entries out of hundreds and dense elimination spends
 * almost all of its work on zeros, its cost growing with the cube of the circuit.
 *
 * <ul>
 *   <li><b>Slots.</b> Every entry the system has ever held — assembled or created as fill-in — owns a slot in a flat
 *       array. Assembling adds into slots; clearing zeroes them. Nothing is allocated per solve.</li>
 *   <li><b>Ordering.</b> Unknowns are eliminated in minimum-degree order on the symmetric pattern of the matrix,
 *       computed again only when an entry appears that the pattern did not hold: eliminating in an arbitrary order
 *       fills a sparse matrix in until it is dense.</li>
 *   <li><b>A plan, replayed.</b> The first elimination chooses each pivot by threshold — the diagonal while it is at
 *       least a tenth of the largest candidate, else the largest — and writes down what it did: each step's pivot, the
 *       rows below it, the columns right of it, and the slot every update lands in. A Newton iteration assembles the
 *       same pattern with other values, so the next solves only replay that plan, as SPICE refactors with its pivots
 *       kept. A pivot that has become small against the entries below it ends the replay, and the elimination is
 *       planned again.</li>
 * </ul>
 *
 * <p>Not for two threads at once.</p>
 */
public final class SparseSystem {

    private static final double SINGULAR            = 1e-14;
    /** A diagonal pivot is chosen while it is at least this share of the largest candidate in its column. */
    private static final double DIAGONAL_PREFERENCE = 0.1;
    /** A replayed pivot is kept while it is at least this share of the largest entry below it — SPICE's threshold. */
    private static final double REPLAY_THRESHOLD    = 1e-3;
    private static final int    ABSENT              = -1;
    private static final int    NOT_ELIMINATED      = Integer.MAX_VALUE;

    private final int      size;
    /** Each entry's slot, or {@link #ABSENT}. */
    private final int[][]  slotOf;
    /** Each row's columns and each column's rows that own a slot. */
    private final int[][]  rowColumns;
    private final int[]    rowCounts;
    private final int[][]  columnRows;
    private final int[]    columnCounts;
    private final double[] rightSide;
    private final double[] forward;
    private final double[] solution;
    /** What was assembled before the Newton guess enters: the linear part, the same in every iteration of a step. */
    private double[] baseValues = new double[16];
    private int      baseSlots;
    private final double[] baseRightSide;
    /** What was assembled before anything of a step enters: the part constant for the whole run; null until kept. */
    private double[] constantValues;
    private double[] constantRightSide;
    /** The values as assembled, and the copy the elimination works on; both by slot. */
    private double[] assembled = new double[16];
    private double[] factors   = new double[16];
    private int      slots;
    /** An entry appeared that the ordering did not see. */
    private boolean  patternChanged = true;
    private int[]    order;
    private Plan     plan;

    public SparseSystem(int size) {
        this.size = size;
        this.slotOf = new int[size][size];
        for (int[] row : slotOf) {
            Arrays.fill(row, ABSENT);
        }
        this.rowColumns = new int[size][4];
        this.rowCounts = new int[size];
        this.columnRows = new int[size][4];
        this.columnCounts = new int[size];
        this.rightSide = new double[size];
        this.forward = new double[size];
        this.solution = new double[size];
        this.baseRightSide = new double[size];
    }

    public int size() {
        return size;
    }

    /** Starts a new assembly: every entry and the right side are zero again. */
    public void clear() {
        Arrays.fill(assembled, 0, slots, 0);
        Arrays.fill(rightSide, 0);
    }

    /** Keeps what is assembled so far, so that {@link #clearToBase()} can start the next assembly from it. */
    public void keepAsBase() {
        if (baseValues.length < slots) {
            baseValues = new double[assembled.length];
        }
        System.arraycopy(assembled, 0, baseValues, 0, slots);
        baseSlots = slots;
        System.arraycopy(rightSide, 0, baseRightSide, 0, size);
    }

    /** Starts a new assembly from what {@link #keepAsBase()} kept: one copy instead of stamping it all again. */
    public void clearToBase() {
        System.arraycopy(baseValues, 0, assembled, 0, baseSlots);
        Arrays.fill(assembled, baseSlots, slots, 0);
        System.arraycopy(baseRightSide, 0, rightSide, 0, size);
    }

    /**
     * Keeps what is assembled so far as the constant part — what reads neither the guess nor the time nor the step, a
     * resistor's conductance —, so that {@link #clearToConstant()} starts every assembly of a run from it.
     */
    public void keepAsConstant() {
        constantValues = Arrays.copyOf(assembled, slots);
        constantRightSide = rightSide.clone();
    }

    /** Starts a new assembly from what {@link #keepAsConstant()} kept, or from zero when nothing was. */
    public void clearToConstant() {
        if (constantValues == null) {
            clear();
            return;
        }
        System.arraycopy(constantValues, 0, assembled, 0, constantValues.length);
        Arrays.fill(assembled, constantValues.length, slots, 0);
        System.arraycopy(constantRightSide, 0, rightSide, 0, size);
    }

    public void add(int row, int column, double value) {
        // The slot first: creating it may replace the array, and `assembled[slot(…)]` would have read the old one.
        int slot = slot(row, column);
        assembled[slot] += value;
    }

    /**
     * The slot of an entry, made part of the pattern if it was not — what a device asks once, to add into the entry by
     * {@link #addAt} on every assembly after, as a SPICE device keeps pointers into its matrix. A slot stays valid for
     * the life of the system.
     */
    public int slot(int row, int column) {
        int slot = slotOf[row][column];
        if (slot == ABSENT) {
            slot = createSlot(row, column);
            patternChanged = true;
        }
        return slot;
    }

    public void addAt(int slot, double value) {
        assembled[slot] += value;
    }

    public void addRightSide(int row, double value) {
        rightSide[row] += value;
    }

    /**
     * Solves the assembled system; the assembly stays as it was. Answers a fresh array, or null when the system is
     * singular.
     */
    public double[] solve() {
        if (patternChanged) {
            order = minimumDegreeOrder();
            plan = null;
            patternChanged = false;
        }
        if (plan == null || !replay(plan)) {
            plan = planElimination();
            if (plan == null) {
                return null;
            }
        }
        substituteBack(plan);
        return solution.clone();
    }

    /**
     * How far {@code at} is from satisfying the assembled system: the length of A·at − b, each row divided by its
     * largest coefficient. Assembled as a Newton linearization at {@code at}, it is how far the nonlinear equations
     * themselves are from holding there. The rows are in different units — a node's in amperes, a state's in volts a
     * second times a slew rate of 3·10⁵ — and unscaled, the one state row was the whole length: each row is put in the
     * units of the unknown it mostly speaks of.
     */
    public double residualNorm(double[] at) {
        double sum = 0;
        for (int row = 0; row < size; row++) {
            double residual = -rightSide[row];
            double largest = 0;
            int[] columns = rowColumns[row];
            for (int entry = 0; entry < rowCounts[row]; entry++) {
                int column = columns[entry];
                double coefficient = assembled[slotOf[row][column]];
                residual += coefficient * at[column];
                largest = Math.max(largest, Math.abs(coefficient));
            }
            double scaled = largest > 0 ? residual / largest : residual;
            sum += scaled * scaled;
        }
        return Math.sqrt(sum);
    }

    /** After a failed solve: an unknown whose row or column holds nothing — what nothing in the circuit determines — or -1. */
    public int unconstrained() {
        for (int index = 0; index < size; index++) {
            if (isEmpty(rowColumns[index], rowCounts[index], index, true)
                    || isEmpty(columnRows[index], columnCounts[index], index, false)) {
                return index;
            }
        }
        return -1;
    }

    private boolean isEmpty(int[] others, int count, int index, boolean row) {
        for (int position = 0; position < count; position++) {
            int slot = row ? slotOf[index][others[position]] : slotOf[others[position]][index];
            if (assembled[slot] != 0) {
                return false;
            }
        }
        return true;
    }

    /** Eliminates along the plan; false when a pivot has become too small to keep and the plan must be made again. */
    private boolean replay(Plan plan) {
        startElimination();
        for (int step = 0; step < size; step++) {
            double pivot = Math.abs(factors[plan.pivotSlot[step]]);
            double largest = 0;
            for (int lower = plan.lowerStart[step]; lower < plan.lowerStart[step + 1]; lower++) {
                largest = Math.max(largest, Math.abs(factors[plan.lowerSlot[lower]]));
            }
            if (pivot < SINGULAR || pivot < REPLAY_THRESHOLD * largest) {
                return false;
            }
            eliminateColumn(plan, step);
        }
        return true;
    }

    /**
     * Eliminates with fresh pivots on the minimum-degree order, writing down every step as it goes; null when a column
     * has no usable pivot.
     */
    private Plan planElimination() {
        startElimination();
        Plan.Builder builder = new Plan.Builder(size);
        boolean[] pivoted = new boolean[size];
        int[] stepOfColumn = new int[size];
        Arrays.fill(stepOfColumn, NOT_ELIMINATED);
        for (int step = 0; step < size; step++) {
            int column = order[step];
            int pivotRow = pivotOf(column, pivoted);
            if (pivotRow < 0) {
                return null;
            }
            pivoted[pivotRow] = true;
            stepOfColumn[column] = step;
            builder.pivot(column, pivotRow, slotOf[pivotRow][column]);
            int[] pivotColumns = rowColumns[pivotRow];
            for (int position = 0; position < rowCounts[pivotRow]; position++) {
                int target = pivotColumns[position];
                if (stepOfColumn[target] == NOT_ELIMINATED) {
                    builder.upper(target, slotOf[pivotRow][target]);
                }
            }
            int[] rows = columnRows[column];
            for (int position = 0; position < columnCounts[column]; position++) {
                int row = rows[position];
                if (pivoted[row]) {
                    continue;
                }
                builder.lower(row, slotOf[row][column]);
                for (int upper = builder.upperFrom(); upper < builder.upperCount(); upper++) {
                    int target = builder.upperColumn(upper);
                    int slot = slotOf[row][target];
                    builder.update(slot == ABSENT ? createSlot(row, target) : slot);
                }
            }
            Plan partial = builder.closeStep();
            eliminateColumn(partial, step);
        }
        return builder.build();
    }

    private void startElimination() {
        System.arraycopy(assembled, 0, factors, 0, slots);
        System.arraycopy(rightSide, 0, forward, 0, size);
    }

    private void eliminateColumn(Plan plan, int step) {
        double pivot = factors[plan.pivotSlot[step]];
        double pivotRight = forward[plan.pivotRow[step]];
        int upperFrom = plan.upperStart[step];
        int upperTo = plan.upperStart[step + 1];
        int update = plan.updateStart[step];
        for (int lower = plan.lowerStart[step]; lower < plan.lowerStart[step + 1]; lower++) {
            double factor = factors[plan.lowerSlot[lower]] / pivot;
            if (factor != 0) {
                for (int upper = upperFrom; upper < upperTo; upper++) {
                    factors[plan.updateSlot[update + upper - upperFrom]] -= factor * factors[plan.upperSlot[upper]];
                }
                forward[plan.lowerRow[lower]] -= factor * pivotRight;
            }
            update += upperTo - upperFrom;
        }
    }

    private void substituteBack(Plan plan) {
        for (int step = size - 1; step >= 0; step--) {
            double sum = forward[plan.pivotRow[step]];
            for (int upper = plan.upperStart[step]; upper < plan.upperStart[step + 1]; upper++) {
                sum -= factors[plan.upperSlot[upper]] * solution[plan.upperColumn[upper]];
            }
            solution[plan.column[step]] = sum / factors[plan.pivotSlot[step]];
        }
    }

    /** The pivot of a column: its diagonal while that is large enough, else its largest candidate; -1 when none is. */
    private int pivotOf(int column, boolean[] pivoted) {
        int largestRow = -1;
        double largest = SINGULAR;
        int[] rows = columnRows[column];
        for (int position = 0; position < columnCounts[column]; position++) {
            int row = rows[position];
            double magnitude = Math.abs(factors[slotOf[row][column]]);
            if (!pivoted[row] && magnitude > largest) {
                largest = magnitude;
                largestRow = row;
            }
        }
        if (largestRow < 0) {
            return -1;
        }
        int diagonal = slotOf[column][column];
        if (!pivoted[column] && diagonal != ABSENT && Math.abs(factors[diagonal]) >= DIAGONAL_PREFERENCE * largest) {
            return column;
        }
        return largestRow;
    }

    private int createSlot(int row, int column) {
        if (slots == assembled.length) {
            assembled = Arrays.copyOf(assembled, slots * 2);
            factors = Arrays.copyOf(factors, slots * 2);
        }
        int slot = slots++;
        assembled[slot] = 0;
        factors[slot] = 0;
        slotOf[row][column] = slot;
        rowColumns[row] = appended(rowColumns[row], rowCounts[row]++, column);
        columnRows[column] = appended(columnRows[column], columnCounts[column]++, row);
        return slot;
    }

    /**
     * The minimum-degree elimination order of the symmetric pattern A + Aᵀ: always the unknown with the fewest
     * neighbours next, its neighbours joined to one another as its elimination would join them.
     */
    private int[] minimumDegreeOrder() {
        boolean[][] adjacent = new boolean[size][size];
        int[] degree = new int[size];
        for (int row = 0; row < size; row++) {
            for (int position = 0; position < rowCounts[row]; position++) {
                int column = rowColumns[row][position];
                if (column != row && !adjacent[row][column]) {
                    adjacent[row][column] = true;
                    adjacent[column][row] = true;
                    degree[row]++;
                    degree[column]++;
                }
            }
        }
        boolean[] eliminated = new boolean[size];
        int[] chosenOrder = new int[size];
        int[] neighbours = new int[size];
        for (int step = 0; step < size; step++) {
            int chosen = -1;
            for (int candidate = 0; candidate < size; candidate++) {
                if (!eliminated[candidate] && (chosen < 0 || degree[candidate] < degree[chosen])) {
                    chosen = candidate;
                }
            }
            chosenOrder[step] = chosen;
            eliminated[chosen] = true;
            int count = 0;
            for (int other = 0; other < size; other++) {
                if (adjacent[chosen][other] && !eliminated[other]) {
                    neighbours[count++] = other;
                    degree[other]--;
                }
            }
            for (int first = 0; first < count; first++) {
                for (int second = first + 1; second < count; second++) {
                    int one = neighbours[first];
                    int another = neighbours[second];
                    if (!adjacent[one][another]) {
                        adjacent[one][another] = true;
                        adjacent[another][one] = true;
                        degree[one]++;
                        degree[another]++;
                    }
                }
            }
        }
        return chosenOrder;
    }

    private static int[] appended(int[] list, int position, int value) {
        int[] grown = position == list.length ? Arrays.copyOf(list, list.length * 2) : list;
        grown[position] = value;
        return grown;
    }

    /**
     * What one elimination did, step by step, as flat arrays: step k pivots {@code column[k]} on {@code pivotRow[k]};
     * its rows below are {@code lowerStart[k] … lowerStart[k + 1]}, its columns right of the pivot
     * {@code upperStart[k] … upperStart[k + 1]}, and every pair of the two has its target slot, row by row, from
     * {@code updateStart[k]}.
     */
    private record Plan(int[] column, int[] pivotRow, int[] pivotSlot,
                        int[] lowerStart, int[] lowerRow, int[] lowerSlot,
                        int[] upperStart, int[] upperColumn, int[] upperSlot,
                        int[] updateStart, int[] updateSlot) {

        /** Writes a plan down while the elimination makes it; a closed step can already be eliminated from. */
        private static final class Builder {

            private final int[] column;
            private final int[] pivotRow;
            private final int[] pivotSlot;
            private final int[] lowerStart;
            private final int[] upperStart;
            private final int[] updateStart;
            private final Ints  lowerRow    = new Ints();
            private final Ints  lowerSlot   = new Ints();
            private final Ints  upperColumn = new Ints();
            private final Ints  upperSlot   = new Ints();
            private final Ints  updateSlot  = new Ints();
            private int step;

            Builder(int size) {
                this.column = new int[size];
                this.pivotRow = new int[size];
                this.pivotSlot = new int[size];
                this.lowerStart = new int[size + 1];
                this.upperStart = new int[size + 1];
                this.updateStart = new int[size + 1];
            }

            void pivot(int pivotColumn, int row, int slot) {
                column[step] = pivotColumn;
                pivotRow[step] = row;
                pivotSlot[step] = slot;
            }

            void upper(int target, int slot) {
                upperColumn.add(target);
                upperSlot.add(slot);
            }

            void lower(int row, int slot) {
                lowerRow.add(row);
                lowerSlot.add(slot);
            }

            void update(int slot) {
                updateSlot.add(slot);
            }

            int upperFrom() {
                return upperStart[step];
            }

            int upperCount() {
                return upperColumn.size;
            }

            int upperColumn(int index) {
                return upperColumn.values[index];
            }

            /** Ends the current step and answers the plan so far, which can eliminate it. */
            Plan closeStep() {
                step++;
                lowerStart[step] = lowerRow.size;
                upperStart[step] = upperColumn.size;
                updateStart[step] = updateSlot.size;
                return new Plan(column, pivotRow, pivotSlot, lowerStart, lowerRow.values, lowerSlot.values,
                        upperStart, upperColumn.values, upperSlot.values, updateStart, updateSlot.values);
            }

            Plan build() {
                return new Plan(column, pivotRow, pivotSlot, lowerStart, lowerRow.trimmed(), lowerSlot.trimmed(),
                        upperStart, upperColumn.trimmed(), upperSlot.trimmed(), updateStart, updateSlot.trimmed());
            }
        }
    }

    /** A growing list of ints without boxing. */
    private static final class Ints {

        private int[] values = new int[16];
        private int   size;

        void add(int value) {
            values = appended(values, size++, value);
        }

        int[] trimmed() {
            return Arrays.copyOf(values, size);
        }
    }
}
