package org.jmouse.cad.simulation.engine;

/**
 * Where the integration is predicted to stand a while past its present point: the polynomial through the last accepted
 * points — a parabola through three, a line through two, the present held where there is only one —, kept as its
 * weights on those points as well as its values. The weights are what lets anything carried from step to step be
 * predicted from its own history by the same polynomial: a model's charge is predicted from the charges it was accepted
 * at, as SPICE predicts it, never evaluated again at predicted voltages.
 *
 * @param values    the predicted state
 * @param atState   the weight on the present point
 * @param atEarlier the weight on the one before it
 * @param atOldest  the weight on the one before that
 */
record Prediction(double[] values, double atState, double atEarlier, double atOldest) {

    /**
     * The prediction {@code ahead} seconds past the present point — negative for a time already passed —, through
     * whichever of the earlier points there are.
     *
     * @param earlier the point one step back, {@code earlierStep} before the present; null where there is none
     * @param oldest  the point before that, {@code oldestStep} before {@code earlier}; null where there is none
     */
    static Prediction ahead(double[] state, double[] earlier, double earlierStep, double[] oldest, double oldestStep,
                            double ahead) {
        if (earlier == null) {
            return new Prediction(state, 1, 0, 0);
        }
        if (oldest == null) {
            double slope = ahead / earlierStep;
            return weighted(state, earlier, null, 1 + slope, -slope, 0);
        }
        double near = earlierStep;
        double far = earlierStep + oldestStep;
        double atState = (ahead + near) * (ahead + far) / (near * far);
        double atEarlier = -ahead * (ahead + far) / (near * oldestStep);
        double atOldest = ahead * (ahead + near) / (far * oldestStep);
        return weighted(state, earlier, oldest, atState, atEarlier, atOldest);
    }

    /** Anything carried from step to step, predicted from its own values at the same points. */
    double of(double atPresent, double atEarlierPoint, double atOldestPoint) {
        return atState * atPresent + atEarlier * atEarlierPoint + atOldest * atOldestPoint;
    }

    private static Prediction weighted(double[] state, double[] earlier, double[] oldest, double atState, double atEarlier,
                                       double atOldest) {
        double[] values = new double[state.length];
        for (int index = 0; index < values.length; index++) {
            double value = atState * state[index] + atEarlier * earlier[index];
            values[index] = oldest == null ? value : value + atOldest * oldest[index];
        }
        return new Prediction(values, atState, atEarlier, atOldest);
    }
}
