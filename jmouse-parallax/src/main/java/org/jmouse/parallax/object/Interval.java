package org.jmouse.parallax.object;

/** A closed real interval [minimum, maximum]: a domain axis, a parameter range, a time span. */
public record Interval(double minimum, double maximum) {

    public Interval {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || !(minimum < maximum)) {
            throw new IllegalArgumentException("An interval needs finite bounds with minimum < maximum, not ["
                    + minimum + ", " + maximum + "].");
        }
    }

    public static Interval of(double minimum, double maximum) {
        return new Interval(minimum, maximum);
    }

    public double length() {
        return maximum - minimum;
    }

    public double at(double fraction) {
        return minimum + fraction * (maximum - minimum);
    }

    /** The i-th of n evenly spaced samples, both ends included. */
    public double sample(int index, int count) {
        return count <= 1 ? (minimum + maximum) / 2 : minimum + (maximum - minimum) * index / (count - 1);
    }

    public boolean contains(double value) {
        return value >= minimum && value <= maximum;
    }
}
