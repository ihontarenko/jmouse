package org.jmouse.telegram.smoke;

import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramRefusal;

import java.util.function.Supplier;

/**
 * The bookkeeping every smoke class in this module shares.
 *
 * <p>Not a test framework and not trying to be one — this repository has no JUnit, and a smoke class
 * is a {@code main} somebody runs. What it saves is each class reinventing a counter and, more to the
 * point, each one deciding differently whether a failed check should stop the run.
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

    /** The refusal a call produced, or {@code null} if it did not refuse. */
    public static TelegramRefusal refusalOf(Runnable call) {
        try {
            call.run();
        } catch (TelegramException exception) {
            return exception.refusal();
        }

        return null;
    }

    /** Whether a constructor or builder rejected its arguments. */
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
     * <p>⚠️ Throws rather than returning a code, so a smoke class run from a build script or an IDE
     * cannot report success while a check failed.
     */
    public static void report() {
        System.out.printf("%n%d checks, %d failed%n", total, failed);

        if (failed > 0) {
            throw new AssertionError(failed + " smoke check(s) failed");
        }
    }
}
