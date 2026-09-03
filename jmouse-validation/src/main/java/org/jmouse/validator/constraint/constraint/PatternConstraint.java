package org.jmouse.validator.constraint.constraint;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The shape a text value has to have. 🔤
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * @Pattern('regex':'^[A-Z]{2}-\d{4}$','message':'Two letters, a dash, four digits')
 * }</pre>
 *
 * <h3>Behaviour</h3>
 * <ul>
 *     <li>{@code null} is valid — compose with {@code required} when presence is the question.</li>
 *     <li>The whole value must match, not merely contain a match. A rule reading
 *         {@code '^[A-Z]{2}-\d{4}$'} and one reading {@code '[A-Z]{2}-\d{4}'} therefore mean the same
 *         thing, which is what somebody writing the second one meant.</li>
 *     <li>A non-text value is compared by its {@code toString()}; a number matching a digit pattern is
 *         a reasonable thing to ask for and refusing it would be a surprise.</li>
 * </ul>
 *
 * <p>⚠️ <strong>The pattern is compiled when it is set, not when a value arrives.</strong> A malformed
 * regex is a mistake by whoever wrote the rule, and it has to reach them: compiled per value it would
 * surface as a failure for whoever submitted a record, having passed every review in between. Setting
 * an unparseable pattern throws here, at binding, where the file and line are still known.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PatternConstraint implements Constraint {

    private String  regex;
    private Pattern compiled;
    private String  message;

    /**
     * Returns a stable constraint code.
     *
     * @return {@code "pattern"}
     */
    @Override
    public String code() {
        return "pattern";
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Object[] arguments() {
        return new Object[]{regex};
    }

    @Override
    public ConstraintExecutor<PatternConstraint> executor() {
        return Executor.INSTANCE;
    }

    private static final class Executor implements ConstraintExecutor<PatternConstraint> {

        private static final Executor INSTANCE = new Executor();

        @Override
        public boolean test(Object value, PatternConstraint constraint) {
            if (value == null || constraint.compiled == null) {
                return true;
            }

            return constraint.compiled.matcher(String.valueOf(value)).matches();
        }
    }

    /**
     * @return the pattern as it was written, or {@code null} when none was set
     */
    public String getRegex() {
        return regex;
    }

    /**
     * Sets the pattern a value has to match, compiling it immediately.
     *
     * @param regex the pattern, as an author wrote it
     * @throws IllegalArgumentException when it does not compile, naming the pattern and the reason
     */
    public void setRegex(String regex) {
        this.regex = regex;
        this.compiled = compile(regex);
    }

    /**
     * @return custom validation message (may be {@code null})
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets custom validation message.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Compiles the pattern, refusing it here rather than at validation time.
     *
     * <p>No anchoring is added: {@code Matcher.matches} already requires the whole value, so a pattern
     * carrying {@code ^} and {@code $} and one without them agree, and neither is rewritten behind its
     * author's back.</p>
     *
     * @param regex the pattern, as an author wrote it
     * @return the compiled pattern, or {@code null} when none was set
     */
    private static Pattern compile(String regex) {
        if (regex == null || regex.isEmpty()) {
            return null;
        }

        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException malformed) {
            throw new IllegalArgumentException(
                    "A validation pattern does not compile: '" + regex + "' — "
                    + malformed.getDescription() + " at position " + malformed.getIndex(), malformed);
        }
    }
}
