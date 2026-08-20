package org.jmouse.access.spi;

/**
 * A condition function could not answer.
 *
 * <h2>Why this is a type rather than one more caught {@code RuntimeException}</h2>
 *
 * <p>A condition that throws is read as {@code false}, and that was safe for as long as conditions were
 * pure: an axis that may only narrow can lose somebody a permission by answering false and can never
 * hand one out. Once a function may reach Redis or a table, "it threw" stops being hypothetical — and
 * the reasoning stops holding, because <strong>false is not fail-closed in both directions</strong>:
 *
 * <table>
 *   <caption>What {@code false} means, by the kind of rule the condition is attached to</caption>
 *   <tr><th>Rule</th><th>{@code false} means</th><th>Outcome</th></tr>
 *   <tr><td>{@code allow x when f(…)}</td><td>the allow does not apply</td><td>refused ✅</td></tr>
 *   <tr><td>{@code deny x when f(…)}</td><td>the deny does not apply</td><td><strong>permitted</strong> ⚠️</td></tr>
 * </table>
 *
 * <p>So a dead counter store would silently lift every quota written as a deny. There is no single
 * boolean that is safe in both positions, which is why a failure has to <em>reach the axis</em>, where
 * the kind of rule is known, instead of being flattened into an answer where it is not.
 *
 * <h2>⚠️ Failing open is possible and has to be asked for</h2>
 *
 * <p>{@link #failsOpen()} is false unless the function said otherwise. A function that would rather be
 * ignored than enforced when it cannot answer — a soft advisory rule, a nice-to-have signal — says so
 * on itself, once, rather than every rule that uses it having to remember.
 */
public class ConditionFunctionFailure extends RuntimeException {

    private final String  functionName;
    private final boolean failsOpen;

    public ConditionFunctionFailure(String functionName, boolean failsOpen, Throwable cause) {
        super("the condition function '" + functionName + "' could not answer", cause);

        this.functionName = functionName;
        this.failsOpen    = failsOpen;
    }

    /** Which function it was, so a log line names something a reader can go and look at. */
    public String functionName() {
        return functionName;
    }

    /**
     * Whether an unanswerable call should be ignored rather than enforced.
     *
     * <p>False by default, which means the rule is applied as though it held: a quota that cannot be
     * read refuses rather than waves through.
     */
    public boolean failsOpen() {
        return failsOpen;
    }
}
