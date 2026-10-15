package org.jmouse.scheduler.spi;

import java.util.Objects;

/** A {@link TriggerGate}'s answer: pass, or refuse with a reason a person can read. */
public sealed interface GateVerdict permits GateVerdict.Pass, GateVerdict.Refuse {

    /** The occurrence may be dispatched. */
    static GateVerdict pass() {
        return Pass.INSTANCE;
    }

    /**
     * @param reason why, in words — it is what history shows
     * @return a refusal
     */
    static GateVerdict refuse(String reason) {
        return new Refuse(reason);
    }

    /** The occurrence may be dispatched. */
    final class Pass implements GateVerdict {

        private static final Pass INSTANCE = new Pass();

        private Pass() {
        }

        @Override
        public String toString() {
            return "PASS";
        }
    }

    /**
     * The occurrence is not dispatched.
     *
     * @param reason why, never blank
     */
    record Refuse(String reason) implements GateVerdict {

        public Refuse {
            Objects.requireNonNull(reason, "reason");

            if (reason.isBlank()) {
                throw new IllegalArgumentException("A gate refusal must say why.");
            }
        }
    }
}
