package org.jmouse.parallax.probability;

/** ln Γ by the Lanczos approximation (g = 7, nine terms), and ln B from it. */
final class SpecialFunctions {

    private static final double[] LANCZOS = {
            0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.61502916214059,
            12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
    };

    private SpecialFunctions() {
    }

    static double logGamma(double value) {
        if (value < 0.5) {
            return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * value))) - logGamma(1 - value);
        }
        double shifted = value - 1;
        double sum = LANCZOS[0];
        for (int index = 1; index < LANCZOS.length; index++) {
            sum += LANCZOS[index] / (shifted + index);
        }
        double t = shifted + 7.5;
        return 0.5 * Math.log(2 * Math.PI) + (shifted + 0.5) * Math.log(t) - t + Math.log(sum);
    }

    static double logBeta(double alpha, double beta) {
        return logGamma(alpha) + logGamma(beta) - logGamma(alpha + beta);
    }
}
