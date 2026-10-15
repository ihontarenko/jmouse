package org.jmouse.notifier.smoke;

import java.util.function.Supplier;

/**
 * The bookkeeping every notifier smoke class shares — this module's and its channels'.
 *
 * <p>Not a test framework: this repository has no JUnit, and a smoke class is a {@code main} somebody
 * runs.
 */
public final class Checks {

    private static int total;
    private static int failed;

    private Checks() {
    }

    public static void check(String what, boolean passed) {
        total++;

        if (!passed) {
            failed++;
        }

        System.out.printf("%s  %s%n", passed ? "ok  " : "FAIL", what);
    }

    /** Whether a constructor or factory rejected its arguments. */
    public static boolean throwsIllegalArgument(Supplier<?> call) {
        try {
            call.get();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    /**
     * Prints the tally and fails the run if anything did.
     *
     * <p>⚠️ Throws rather than returning a code, so a run cannot report success while a check failed.
     */
    public static void report() {
        System.out.printf("%n%d checks, %d failed%n", total, failed);

        if (failed > 0) {
            throw new AssertionError(failed + " smoke check(s) failed");
        }
    }
}
