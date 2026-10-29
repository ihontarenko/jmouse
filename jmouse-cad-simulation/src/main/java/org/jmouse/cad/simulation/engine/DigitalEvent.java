package org.jmouse.cad.simulation.engine;

/**
 * One thing that happened to a unit's discrete side, which its behaviour may handle:
 * {@code on <kind> '<subject>' [when …] do … end}.
 *
 * @param subject what it happened to — the input that changed, the expression that crossed, the timer or period
 *                that came due; null for {@code start}
 */
record DigitalEvent(DigitalUnit unit, Kind kind, String subject) {

    /** The events a behaviour may handle. */
    enum Kind {
        /** Once, at t = 0. */
        START,
        /** A digital input went high, went low, or changed at all. */
        RISING, FALLING, CHANGE,
        /** A jMF expression of the unit's voltages crossed zero, upwards or downwards. */
        ABOVE, BELOW,
        /** Periodically, from t = 0. */
        EVERY,
        /** A {@code schedule('name', delay)} came due. */
        TIMER;

        /** As a behaviour writes it: {@code on rising 'clk'}. */
        String keyword() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** The kind a handler names; refused, naming the model, for a word that is no event. */
        static Kind of(String keyword, String model) {
            for (Kind kind : values()) {
                if (kind.keyword().equals(keyword)) {
                    return kind;
                }
            }
            throw new IllegalArgumentException(model + ": 'on " + keyword
                    + "' is not an event of the simulator — start, rising, falling, change, above, below, every, timer.");
        }
    }
}
