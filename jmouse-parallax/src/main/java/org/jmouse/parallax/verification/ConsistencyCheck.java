package org.jmouse.parallax.verification;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;

/**
 * A check that the mathematics agrees with itself for one object: a symbolic derivative with a finite difference,
 * a section with the surface it was cut from, a trajectory run forwards and back with where it started. Checks are
 * how a catalogue finds out that an object, a computation or a numerical method has drifted.
 */
public interface ConsistencyCheck {

    String code();

    String description();

    boolean appliesTo(ObjectDefinition definition);

    CheckResult check(CompiledObject object);

    enum Outcome { PASSED, WARNED, FAILED, SKIPPED }

    /** What a check found: its verdict, the measured error against the tolerance, and a sentence about it. */
    record CheckResult(Outcome outcome, Double measured, Double tolerance, String detail) {

        public static CheckResult judge(double measured, double tolerance, double warning, String detail) {
            Outcome outcome = !Double.isFinite(measured) ? Outcome.FAILED
                    : measured <= tolerance ? Outcome.PASSED : measured <= warning ? Outcome.WARNED : Outcome.FAILED;
            return new CheckResult(outcome, measured, tolerance, detail);
        }

        public static CheckResult skipped(String why) {
            return new CheckResult(Outcome.SKIPPED, null, null, why);
        }
    }
}
