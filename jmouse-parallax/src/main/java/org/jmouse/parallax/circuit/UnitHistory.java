package org.jmouse.parallax.circuit;

/**
 * What a model unit carries from one accepted step to the next: each charge's value and rate, and each state equation's
 * rate — what the trapezoidal rule integrates against.
 */
final class UnitHistory {

    final double[] chargePrevious;
    final double[] ratePrevious;
    /** Each equation's rate as last accepted; only a state equation's is kept. */
    final double[] stateRatePrevious;

    UnitHistory(int charges, int equations) {
        this.chargePrevious = new double[charges];
        this.ratePrevious = new double[charges];
        this.stateRatePrevious = new double[equations];
    }

    /** What a charge's rate carries from the steps before: coefficient · q_previous, plus the previous rate under the trapezoidal rule. */
    double carried(int charge, double coefficient, boolean trapezoidal) {
        return coefficient * chargePrevious[charge] + (trapezoidal ? ratePrevious[charge] : 0);
    }

    /** A charge accepted at {@code value}: its rate over the step, and the value the next step starts from. */
    double accept(int charge, double value, double coefficient, boolean trapezoidal) {
        ratePrevious[charge] = coefficient * (value - chargePrevious[charge]) - (trapezoidal ? ratePrevious[charge] : 0);
        chargePrevious[charge] = value;
        return ratePrevious[charge];
    }
}
