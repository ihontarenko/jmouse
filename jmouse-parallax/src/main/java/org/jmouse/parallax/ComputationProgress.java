package org.jmouse.parallax;

import java.time.Duration;

/** Where a long computation reports how far it is, and learns that it should stop. */
public interface ComputationProgress {

    /**
     * Reports nowhere and stops the work once {@code limit} of wall time has passed: what bounds a computation run
     * inline, whose cost estimate cannot see the worst case of an integral that will not settle.
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

    ComputationProgress NONE = new ComputationProgress() {
        @Override
        public void report(double fraction, String message) {
        }

        @Override
        public boolean cancelled() {
            return false;
        }
    };

    void report(double fraction, String message);

    boolean cancelled();

    /** Throws when the work has been cancelled; computations call it between rows, steps or samples. */
    default void checkpoint(double fraction, String message) {
        if (cancelled()) {
            throw new ComputationCancelled();
        }
        report(Math.max(0, Math.min(1, fraction)), message);
    }

    final class ComputationCancelled extends RuntimeException {
        public ComputationCancelled() {
            super("The computation was cancelled.");
        }
    }
}
