package org.jmouse.numerics;

import java.time.Duration;

/** Where a long computation reports how far it is, and learns that it should stop. */
public interface ComputationProgress {

    /** Reports nowhere and never stops the work. */
    ComputationProgress NONE = new ComputationProgress() {
        @Override
        public void report(double fraction, String message) {
        }

        @Override
        public boolean cancelled() {
            return false;
        }
    };

    /**
     * Reports nowhere and stops the work once {@code limit} of wall time has passed: what bounds a computation run inline,
     * whose cost cannot be told in advance.
     */
    static ComputationProgress within(Duration limit) {
        long deadline = System.nanoTime() + limit.toNanos();
        return new ComputationProgress() {
            @Override
            public void report(double fraction, String message) {
            }

            @Override
            public boolean cancelled() {
                return System.nanoTime() - deadline > 0;
            }
        };
    }

    /** How far it is — a share of the whole, 0 to 1 — and what it is doing. */
    void report(double fraction, String message);

    /** Whether the work should stop. */
    boolean cancelled();
}
