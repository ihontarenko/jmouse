package org.jmouse.cad.simulation.engine;

/**
 * What a model unit carries from one accepted step to the next: each charge's value at the last three accepted points
 * and its rate, each state equation's rate — what the trapezoidal rule integrates against —, and each charge as measured
 * at the attempt being judged.
 *
 * <p>A charge is evaluated once per settled attempt, at its solution ({@link #measured}); its error is judged against
 * the polynomial through its own accepted values, and the step that is taken carries that same value on. Measured
 * again at the solution for the acceptance, and once more at the predicted voltages for the error, it was three
 * evaluations of a junction's whole charge formula on every step — a third of a diode multiplier's time.</p>
 */
final class UnitHistory {

    /** Each charge at the last accepted point, and at the two before it. */
    final double[] chargePrevious;
    final double[] chargeEarlier;
    final double[] chargeOldest;
    final double[] ratePrevious;
    /** Each charge at the solution of the attempt being judged. */
    final double[] measured;
    /** Each equation's rate as last accepted; only a state equation's is kept. */
    final double[] stateRatePrevious;

    UnitHistory(int charges, int equations) {
        this.chargePrevious = new double[charges];
        this.chargeEarlier = new double[charges];
        this.chargeOldest = new double[charges];
        this.ratePrevious = new double[charges];
        this.measured = new double[charges];
        this.stateRatePrevious = new double[equations];
    }

    /** A charge where a transient starts: at rest, as if it had stood there all along. */
    void start(int charge, double value) {
        chargePrevious[charge] = value;
        chargeEarlier[charge] = value;
        chargeOldest[charge] = value;
        ratePrevious[charge] = 0;
    }

    /** What a charge's rate carries from the steps before: coefficient · q_previous, plus the previous rate under the trapezoidal rule. */
    double carried(int charge, double coefficient, boolean trapezoidal) {
        return coefficient * chargePrevious[charge] + (trapezoidal ? ratePrevious[charge] : 0);
    }

    /** The largest share of its allowed error any charge used: as measured, against its own predicted value. */
    double chargeTruncation(Prediction prediction) {
        double largest = 0;
        for (int charge = 0; charge < measured.length; charge++) {
            double expected = prediction.of(chargePrevious[charge], chargeEarlier[charge], chargeOldest[charge]);
            largest = Math.max(largest, Truncation.share(measured[charge] - expected, measured[charge], Truncation.CHARGE));
        }
        return largest;
    }

    /** The attempt is taken: each charge's rate over the step, and its measured value the one the next step starts from. */
    void accept(double coefficient, boolean trapezoidal) {
        for (int charge = 0; charge < measured.length; charge++) {
            ratePrevious[charge] = coefficient * (measured[charge] - chargePrevious[charge]) - (trapezoidal ? ratePrevious[charge] : 0);
            chargeOldest[charge] = chargeEarlier[charge];
            chargeEarlier[charge] = chargePrevious[charge];
            chargePrevious[charge] = measured[charge];
        }
    }
}
