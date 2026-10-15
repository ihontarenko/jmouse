package org.jmouse.formula;

/** The special functions jMF offers beyond java.lang.Math: the gamma family and the error function. */
final class SpecialFunctions {

    /** Lanczos coefficients for g = 7, n = 9: about 15 significant digits across the real line. */
    private static final double LANCZOS_G = 7;
    private static final double[] LANCZOS = {
            0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
            -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6,
            1.5056327351493116e-7};

    private SpecialFunctions() {
    }

    /** Γ(x); the reflection formula covers x < ½, and the poles at 0, −1, −2, … are NaN. */
    static double gamma(double value) {
        if (value <= 0 && value == Math.rint(value)) {
            return Double.NaN;
        }
        if (value < 0.5) {
            return Math.PI / (Math.sin(Math.PI * value) * gamma(1 - value));
        }
        if (value == Math.rint(value) && value <= 171) {
            return factorialOfInteger((long) value - 1);
        }
        double shifted = value - 1;
        double series = LANCZOS[0];
        for (int index = 1; index < LANCZOS.length; index++) {
            series += LANCZOS[index] / (shifted + index);
        }
        double base = shifted + LANCZOS_G + 0.5;
        return Math.sqrt(2 * Math.PI) * Math.pow(base, shifted + 0.5) * Math.exp(-base) * series;
    }

    /** Γ(z) over the complex plane, by the same Lanczos series and the reflection formula left of ½. */
    static Complex gamma(Complex value) {
        if (value.imaginary() == 0) {
            return Complex.of(gamma(value.real()));
        }
        if (value.real() < 0.5) {
            Complex reflected = Complex.of(Math.PI).multiply(value).sin().multiply(gamma(Complex.ONE.subtract(value)));
            return Complex.of(Math.PI).divide(reflected);
        }
        Complex shifted = value.subtract(Complex.ONE);
        Complex series = Complex.of(LANCZOS[0]);
        for (int index = 1; index < LANCZOS.length; index++) {
            series = series.add(Complex.of(LANCZOS[index]).divide(shifted.add(Complex.of(index))));
        }
        Complex base = shifted.add(Complex.of(LANCZOS_G + 0.5));
        return Complex.of(Math.sqrt(2 * Math.PI)).multiply(base.power(shifted.add(Complex.of(0.5))))
                .multiply(base.negate().exp()).multiply(series);
    }

    /** n! for any real n as Γ(n + 1); a whole n is multiplied out exactly. */
    static double factorial(double value) {
        return gamma(value + 1);
    }

    private static double factorialOfInteger(long value) {
        double result = 1;
        for (long factor = 2; factor <= value; factor++) {
            result *= factor;
        }
        return result;
    }

    /** C(n, k); for whole arguments a product that stays exact far beyond what n!/(k!(n−k)!) would. */
    static double binomial(double total, double chosen) {
        if (total == Math.rint(total) && chosen == Math.rint(chosen) && total >= 0) {
            if (chosen < 0 || chosen > total) {
                return 0;
            }
            long k = (long) Math.min(chosen, total - chosen);
            double result = 1;
            for (long index = 1; index <= k; index++) {
                result = result * (total - k + index) / index;
            }
            return Math.rint(result);
        }
        return gamma(total + 1) / (gamma(chosen + 1) * gamma(total - chosen + 1));
    }

    /** erf(x) to about 1e-15: its Taylor series near zero, a continued fraction for erfc further out. */
    static double erf(double value) {
        if (Double.isNaN(value)) {
            return Double.NaN;
        }
        double magnitude = Math.abs(value);
        if (magnitude > 6) {
            return Math.signum(value);
        }
        if (magnitude < 2.5) {
            double term = value;
            double sum = value;
            double square = value * value;
            for (int index = 1; index < 100; index++) {
                term *= -square / index;
                double next = term / (2 * index + 1);
                sum += next;
                if (Math.abs(next) < 1e-17 * Math.abs(sum)) {
                    break;
                }
            }
            return 2 / Math.sqrt(Math.PI) * sum;
        }
        // erfc(x) = exp(-x²)/√π · 1/(x + 1/2/(x + 1/(x + 3/2/(x + …)))), evaluated from the tail.
        double fraction = 0;
        for (int index = 60; index >= 1; index--) {
            fraction = (index / 2.0) / (magnitude + fraction);
        }
        double complement = Math.exp(-magnitude * magnitude) / Math.sqrt(Math.PI) / (magnitude + fraction);
        return Math.signum(value) * (1 - complement);
    }
}
