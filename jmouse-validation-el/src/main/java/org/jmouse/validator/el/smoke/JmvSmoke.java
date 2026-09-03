package org.jmouse.validator.el.smoke;

/**
 * Runs every jMV smoke class and exits non-zero if any of them found something. 🚦
 *
 * <p>Thin on purpose: each file below owns one claim the language makes, and this one owns only the
 * order they run in and the exit code. A dispatcher that also asserted would be a fifth place to look
 * when something fails.</p>
 *
 * <pre>{@code
 * java -cp … org.jmouse.validator.el.smoke.JmvSmoke
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmvSmoke {

    public static void main(String[] arguments) {
        int failures = BranchingSmoke.run()
                       + RoundTripSmoke.run()
                       + TriviaSmoke.run()
                       + BuilderSmoke.run()
                       + ShapeSmoke.run()
                       + CollectAllSmoke.run()
                       + StopSmoke.run();

        System.out.println();
        System.out.println(failures == 0 ? "jMV: everything holds." : "jMV: " + failures + " failed.");

        System.exit(failures);
    }

    private JmvSmoke() {
    }
}
