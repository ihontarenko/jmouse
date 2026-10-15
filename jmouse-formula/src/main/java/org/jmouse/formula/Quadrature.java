package org.jmouse.formula;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.DoubleUnaryOperator;

/**
 * Definite integrals by globally adaptive Gauss–Kronrod (7-point Gauss inside 15-point Kronrod): on each interval
 * the two rules disagree by about its error, and the interval that disagrees most is halved until the sum of the
 * disagreements is within tolerance. The nodes never touch an interval's
 * ends, so an integrable singularity there — {@code 1/sqrt(t)} at 0 — is evaluated around, not at.
 *
 * <p>An infinite bound is mapped onto a finite interval first: {@code [a, ∞)} by {@code t = a + s/(1 - s)},
 * {@code (-∞, ∞)} by {@code t = s/(1 - s²)}. An integral that does not settle within the subdivision budget is
 * NaN — an undefined value, never a guessed one.
 *
 * <p>An integral whose integrand is itself an integral asks for less: the inner one is only as exact as its own
 * tolerance, and an outer one demanding the same precision chases that noise to the end of its budget at every point —
 * the accumulation curve of a convolution then runs for hours. So the inner call tells the integral enclosing it on the
 * same thread, and that one settles at {@link #NESTED_RELATIVE_TOLERANCE}.
 */
public final class Quadrature {

    private static final double ABSOLUTE_TOLERANCE = 1e-12;
    private static final double RELATIVE_TOLERANCE = 1e-10;
    private static final double NESTED_ABSOLUTE_TOLERANCE = 1e-10;
    private static final double NESTED_RELATIVE_TOLERANCE = 1e-7;
    private static final int MAXIMUM_INTERVALS = 2000;
    /**
     * What one integral usually costs in evaluations of its body: a few 15-point pieces. For estimates only — an
     * integral that does not settle may cost up to {@code 15 · MAXIMUM_INTERVALS}.
     */
    public static final long TYPICAL_EVALUATIONS = 150;
    /** The error a result cut off by the interval budget may still carry and be kept. */
    private static final double ACCEPTABLE_ERROR = 1e-6;

    private static final double[] KRONROD_NODES = {
            0.991455371120812639206854697526329, 0.949107912342758524526189684047851,
            0.864864423359769072789712788640926, 0.741531185599394439863864773280788,
            0.586087235467691130294144845693013, 0.405845151377397166906606412076961,
            0.207784955007898467600689403773245, 0.000000000000000000000000000000000};
    private static final double[] KRONROD_WEIGHTS = {
            0.022935322010529224963732008058970, 0.063092092629978553290700663189204,
            0.104790010322250183839876322541518, 0.140653259715525918745189590510238,
            0.169004726639267902826583426598550, 0.190350578064785409913256402421014,
            0.204432940075298892414161999234649, 0.209482141084727828012999174891714};
    /** Gauss weights for the Kronrod nodes at odd positions (1, 3, 5) and the centre. */
    private static final double[] GAUSS_WEIGHTS = {
            0.129484966168869693270611432679082, 0.279705391489276667901467771423780,
            0.381830050505118944950369775488975, 0.417959183673469387755102040816327};

    /** The integral being adapted on this thread, so one evaluated inside its integrand can tell it so. */
    private static final ThreadLocal<Integration> CURRENT = new ThreadLocal<>();

    private Quadrature() {
    }

    public static double integrate(DoubleUnaryOperator integrand, double lower, double upper) {
        if (Double.isNaN(lower) || Double.isNaN(upper)) {
            return Double.NaN;
        }
        if (lower == upper) {
            return 0;
        }
        if (lower > upper) {
            return -integrate(integrand, upper, lower);
        }
        boolean lowerInfinite = lower == Double.NEGATIVE_INFINITY;
        boolean upperInfinite = upper == Double.POSITIVE_INFINITY;
        if (lowerInfinite && upperInfinite) {
            return finite(parameter -> {
                double denominator = 1 - parameter * parameter;
                double point = parameter / denominator;
                return integrand.applyAsDouble(point) * (1 + parameter * parameter) / (denominator * denominator);
            }, -1, 1);
        }
        if (upperInfinite) {
            return finite(parameter -> {
                double denominator = 1 - parameter;
                return integrand.applyAsDouble(lower + parameter / denominator) / (denominator * denominator);
            }, 0, 1);
        }
        if (lowerInfinite) {
            return finite(parameter -> {
                double denominator = 1 - parameter;
                return integrand.applyAsDouble(upper - parameter / denominator) / (denominator * denominator);
            }, 0, 1);
        }
        return finite(integrand, lower, upper);
    }

    /**
     * Global adaptation: the interval with the largest error estimate is halved next, until the estimates add up
     * to the tolerance. Out of budget, a result whose error is still small against it is kept; otherwise NaN.
     */
    private static double finite(DoubleUnaryOperator integrand, double lower, double upper) {
        Integration integration = new Integration(CURRENT.get());
        if (integration.enclosing != null) {
            integration.enclosing.integrandIntegrates = true;
        }
        CURRENT.set(integration);
        try {
            return adapt(integrand, lower, upper, integration);
        } finally {
            if (integration.enclosing == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(integration.enclosing);
            }
        }
    }

    private static double adapt(DoubleUnaryOperator integrand, double lower, double upper, Integration integration) {
        PriorityQueue<Piece> pieces = new PriorityQueue<>(Comparator.comparingDouble(Piece::error).reversed());
        Piece whole = piece(integrand, lower, upper);
        pieces.add(whole);
        double total = whole.kronrod();
        double error = whole.error();
        for (int intervals = 1; intervals < MAXIMUM_INTERVALS; intervals++) {
            if (!Double.isFinite(total)) {
                return Double.NaN;
            }
            if (error <= integration.tolerance(total)) {
                return total;
            }
            Piece worst = pieces.poll();
            double middle = 0.5 * (worst.lower() + worst.upper());
            if (middle <= worst.lower() || middle >= worst.upper()) {
                break;
            }
            Piece left = piece(integrand, worst.lower(), middle);
            Piece right = piece(integrand, middle, worst.upper());
            pieces.add(left);
            pieces.add(right);
            total += left.kronrod() + right.kronrod() - worst.kronrod();
            error += left.error() + right.error() - worst.error();
        }
        return Double.isFinite(total) && error <= ACCEPTABLE_ERROR * Math.max(1, Math.abs(total)) ? total : Double.NaN;
    }

    /** One integral being adapted, and whether its integrand has turned out to be an integral too. */
    private static final class Integration {

        private final Integration enclosing;
        private boolean integrandIntegrates;

        private Integration(Integration enclosing) {
            this.enclosing = enclosing;
        }

        private double tolerance(double total) {
            if (integrandIntegrates) {
                return Math.max(NESTED_ABSOLUTE_TOLERANCE, NESTED_RELATIVE_TOLERANCE * Math.abs(total));
            }
            return Math.max(ABSOLUTE_TOLERANCE, RELATIVE_TOLERANCE * Math.abs(total));
        }
    }

    private record Piece(double lower, double upper, double kronrod, double error) {
    }

    private static Piece piece(DoubleUnaryOperator integrand, double lower, double upper) {
        double centre = 0.5 * (lower + upper);
        double halfLength = 0.5 * (upper - lower);
        double centreValue = integrand.applyAsDouble(centre);
        double kronrod = centreValue * KRONROD_WEIGHTS[7];
        double gauss = centreValue * GAUSS_WEIGHTS[3];
        for (int index = 0; index < 7; index++) {
            double offset = halfLength * KRONROD_NODES[index];
            double sum = integrand.applyAsDouble(centre - offset) + integrand.applyAsDouble(centre + offset);
            kronrod += KRONROD_WEIGHTS[index] * sum;
            if (index % 2 == 1) {
                gauss += GAUSS_WEIGHTS[index / 2] * sum;
            }
        }
        kronrod *= halfLength;
        gauss *= halfLength;
        return new Piece(lower, upper, kronrod, Math.abs(kronrod - gauss));
    }
}
