package org.jmouse.parallax.region;

import org.jmouse.formula.ExactRationalEvaluator;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.Rational;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Region;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The region engine: the pixels of a picture where a condition holds.
 *
 * <ul>
 *   <li><b>Integer inputs and {@code exact} parameters</b> — every pixel is a pair of integers and the condition is
 *   decided in exact fractions by {@link ExactRationalEvaluator}: no rounding anywhere, however many digits.</li>
 *   <li><b>Integer inputs otherwise</b> — one pixel per pair of integers, the condition evaluated in doubles.</li>
 *   <li><b>Real inputs</b> — the rectangle sampled at the pixel centres of a square grid of the chosen resolution.</li>
 * </ul>
 *
 * <p>The picture is laid out as a plot: the first input from its first bound on the left to its second on the right,
 * the second from its first bound at the bottom to its second at the top; the first row of the grid is the top. A
 * condition that is an equation holds where its sides agree; any other holds where it is not zero.</p>
 */
public final class RegionComputations {

    private static final int MAXIMUM_SIDE   = 1_024;
    private static final int MAXIMUM_PIXELS = 1_000_000;

    private RegionComputations() {
    }

    /** Refuses, at save, a region whose bounds or condition cannot be evaluated: it decides its first pixel. */
    public static void check(CompiledObject object) {
        Picture.decider(object).holds(0, 0);
    }

    public static class Picture implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.REGION;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof Region;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("resolution", 400, 8, MAXIMUM_SIDE, "Pixels per side over real inputs"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            Region region = (Region) object.definition();
            if (!region.integer()) {
                return (long) settings.integer("resolution") * settings.integer("resolution");
            }
            Decider decider = decider(object);
            return (long) decider.columns() * decider.rows() * (region.exactIntegers().isEmpty() ? 1 : 50);
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            Region region = (Region) object.definition();
            Decider decider = region.integer() ? decider(object) : decider(object, settings.integer("resolution"));
            int[] pixels = new int[decider.columns() * decider.rows()];
            for (int row = 0; row < decider.rows(); row++) {
                if (row % 16 == 0) {
                    progress.checkpoint((double) row / decider.rows(), "deciding");
                }
                for (int column = 0; column < decider.columns(); column++) {
                    pixels[row * decider.columns() + column] = decider.holds(column, row) ? 1 : 0;
                }
            }
            return new Dataset.BinaryGrid(decider.columns(), decider.rows(), pixels);
        }

        static Decider decider(CompiledObject object) {
            return decider(object, 0);
        }

        /** How every pixel of the region is decided; {@code resolution} matters to real inputs only. */
        static Decider decider(CompiledObject object, int resolution) {
            Region region = (Region) object.definition();
            if (!region.integer()) {
                return new RealDecider(object, region, resolution == 0 ? 400 : resolution);
            }
            return region.exactIntegers().isEmpty() ? new IntegerDecider(object, region) : new ExactDecider(object, region);
        }
    }

    /** What a pixel of the grid is and whether the condition holds there. */
    private interface Decider {

        int columns();

        int rows();

        boolean holds(int column, int row);
    }

    /** One axis of an integer grid: from its first bound to its second, a step of one either way. */
    private record IntegerAxis(BigInteger from, BigInteger to) {

        int size() {
            BigInteger size = to.subtract(from).abs().add(BigInteger.ONE);
            if (size.compareTo(BigInteger.valueOf(MAXIMUM_SIDE)) > 0) {
                throw new FormulaException("A region is at most " + MAXIMUM_SIDE + " integers wide and high.", null, -1);
            }
            return size.intValue();
        }

        /** The integer at a position counted from the first bound. */
        BigInteger at(int position) {
            BigInteger offset = BigInteger.valueOf(position);
            return to.compareTo(from) >= 0 ? from.add(offset) : from.subtract(offset);
        }
    }

    /** {@code exact}: the bounds and the condition in exact fractions; sliders enter as the decimals they show. */
    private static final class ExactDecider implements Decider {

        private final Formula               condition;
        private final Map<String, Rational> values = new HashMap<>();
        private final List<String>          variables;
        private final IntegerAxis           horizontal;
        private final IntegerAxis           vertical;

        ExactDecider(CompiledObject object, Region region) {
            this.condition = FormulaLanguage.standard().parse(region.condition());
            this.variables = region.variables();
            object.parameterValues().forEach((name, value) -> values.put(name, Rational.ofDecimal(value)));
            region.exactIntegers().forEach((name, digits) -> values.put(name, Rational.of(new BigInteger(digits))));
            object.exactIntegerValues().forEach((name, value) -> values.put(name, Rational.of(value)));
            this.horizontal = new IntegerAxis(bound(region.from().get(0)), bound(region.to().get(0)));
            this.vertical = new IntegerAxis(bound(region.from().get(1)), bound(region.to().get(1)));
            requireSize(horizontal.size(), vertical.size());
        }

        private BigInteger bound(String written) {
            Rational value;
            try {
                value = ExactRationalEvaluator.evaluate(FormulaLanguage.standard().parse(written), values);
            } catch (IllegalArgumentException refused) {
                throw new FormulaException("The bound '" + written + "' has no exact value: " + refused.getMessage(), written, -1);
            }
            if (!value.isInteger()) {
                throw new FormulaException("An integer input's bound is an integer, not " + value + ".", written, -1);
            }
            return value.numerator();
        }

        public int columns() {
            return horizontal.size();
        }

        public int rows() {
            return vertical.size();
        }

        public boolean holds(int column, int row) {
            Map<String, Rational> point = new HashMap<>(values);
            point.put(variables.get(0), Rational.of(horizontal.at(column)));
            point.put(variables.get(1), Rational.of(vertical.at(rows() - 1 - row)));
            try {
                return ExactRationalEvaluator.holds(condition, point);
            } catch (IllegalArgumentException refused) {
                throw new FormulaException("The condition has no exact value: " + refused.getMessage(), null, -1);
            }
        }
    }

    /** Integer inputs decided in doubles: one pixel per pair of integers. */
    private static final class IntegerDecider implements Decider {

        private final BoundFunction condition;
        private final boolean       equation;
        private final IntegerAxis   horizontal;
        private final IntegerAxis   vertical;

        IntegerDecider(CompiledObject object, Region region) {
            this.condition = object.function(region.condition(), region.variables());
            this.equation = condition.expression() instanceof Formula.Equation;
            this.horizontal = new IntegerAxis(whole(object, region.from().get(0)), whole(object, region.to().get(0)));
            this.vertical = new IntegerAxis(whole(object, region.from().get(1)), whole(object, region.to().get(1)));
            requireSize(horizontal.size(), vertical.size());
        }

        public int columns() {
            return horizontal.size();
        }

        public int rows() {
            return vertical.size();
        }

        public boolean holds(int column, int row) {
            return decided(condition.at(horizontal.at(column).doubleValue(), vertical.at(rows() - 1 - row).doubleValue()), equation);
        }
    }

    /** Real inputs: the pixel centres of a square grid over the rectangle between the bounds. */
    private static final class RealDecider implements Decider {

        private final BoundFunction condition;
        private final boolean       equation;
        private final double[]      from;
        private final double[]      to;
        private final int           resolution;

        RealDecider(CompiledObject object, Region region, int resolution) {
            this.condition = object.function(region.condition(), region.variables());
            this.equation = condition.expression() instanceof Formula.Equation;
            this.from = region.from().stream().mapToDouble(object::constant).toArray();
            this.to = region.to().stream().mapToDouble(object::constant).toArray();
            this.resolution = resolution;
        }

        public int columns() {
            return resolution;
        }

        public int rows() {
            return resolution;
        }

        public boolean holds(int column, int row) {
            double x = from[0] + (column + 0.5) / resolution * (to[0] - from[0]);
            double y = from[1] + (resolution - row - 0.5) / resolution * (to[1] - from[1]);
            return decided(condition.at(x, y), equation);
        }
    }

    /** An equation holds where its residual vanishes; any other condition where it is not zero. */
    private static boolean decided(double value, boolean equation) {
        if (equation) {
            return Math.abs(value) <= 1e-9;
        }
        return value != 0 && !Double.isNaN(value);
    }

    private static BigInteger whole(CompiledObject object, String written) {
        double value = object.constant(written);
        if (value != Math.rint(value) || Math.abs(value) > 9_007_199_254_740_991d) {
            throw new FormulaException("An integer input's bound is an integer, not " + value + ".", written, -1);
        }
        return BigInteger.valueOf((long) value);
    }

    private static void requireSize(int columns, int rows) {
        if ((long) columns * rows > MAXIMUM_PIXELS) {
            throw new FormulaException("A region has at most " + MAXIMUM_PIXELS + " pixels.", null, -1);
        }
    }
}
