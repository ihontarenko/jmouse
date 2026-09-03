package org.jmouse.validator.el.smoke;

import org.jmouse.validator.el.JmvReader;
import org.jmouse.validator.el.runtime.CompiledValidation;
import org.jmouse.validator.el.runtime.ValidationOutcome;
import org.jmouse.validator.el.translate.JmvCompiler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The plumbing every smoke class needs, so none of them writes it again. 🧪
 *
 * <p>Not a test framework and deliberately not on one: these run from {@code main}, print what they
 * found, and exit non-zero when something is wrong — which is what makes them usable from a shell, a
 * build, or a session that wants to see the language work without a test runner in the way.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SmokeReport {

    private final String title;

    private int checked;
    private int failed;

    public SmokeReport(String title) {
        this.title = title;

        System.out.println();
        System.out.println("-- " + title + " " + "-".repeat(Math.max(0, 72 - title.length())));
    }

    /**
     * Compiles a document, so a smoke class says what it is testing and not how to build it.
     *
     * @param source the {@code .jmv} text
     * @return the compiled document
     */
    public static CompiledValidation compile(String source) {
        return new JmvCompiler().translate(new JmvReader().parse(source, "smoke.jmv"));
    }

    /**
     * A record, written the way a smoke case reads best — pairs, and nulls allowed.
     *
     * @param pairs field, value, field, value…
     * @return the record
     */
    public static Map<String, Object> record(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (int index = 0; index < pairs.length; index += 2) {
            values.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }

        return values;
    }

    /**
     * Reports one expectation.
     *
     * @param what     what was being asked
     * @param expected what should have happened
     * @param actual   what did
     */
    public void expect(String what, Object expected, Object actual) {
        checked++;

        boolean holds = String.valueOf(expected).equals(String.valueOf(actual));

        if (!holds) {
            failed++;
        }

        System.out.printf("%-5s %-52s %s%n", holds ? "ok" : "FAIL", what,
                          holds ? actual : actual + "   (expected " + expected + ")");
    }

    /**
     * Reports the complaints an outcome raised, as one comparable line.
     *
     * @param what     what was being asked
     * @param expected the messages, in order
     * @param outcome  what the document said
     */
    public void expectErrors(String what, List<String> expected, ValidationOutcome outcome) {
        expect(what, expected, outcome.errors().stream().map(error -> error.message()).toList());
    }

    /** @return the number of failures, for an exit code */
    public int failures() {
        System.out.printf("     %d checked, %d failed - %s%n", checked, failed, title);

        return failed;
    }
}
