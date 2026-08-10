package org.jmouse.access.el;

import org.jmouse.access.policy.model.SourceSpan;
import org.jmouse.el.parser.ParseException;

/**
 * A {@code .jmp} file that will not parse.
 *
 * <p>Stage 1 owns nothing but text, so everything this can report is structural: a statement in a
 * block that cannot hold it, a role carrying an instance scope, a denial written where only a bundle
 * belongs. Whether a scope exists, whether a permission was ever registered, whether a condition
 * compiles — none of that is knowable here and all of it is stage 2's to refuse.</p>
 *
 * <p>Keeping the two apart is the point: a parser failure and a policy failure should never look
 * alike to whoever is reading the message.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PolicyParseException extends ParseException {

    private final SourceSpan at;

    /**
     * Constructs a parse failure anchored at a position in the file.
     *
     * @param at      where in the file the offending construction begins
     * @param message what is wrong with it
     */
    public PolicyParseException(SourceSpan at, String message) {
        super(at + ": " + message);
        this.at = at;
    }

    /**
     * Returns where in the file the offending construction begins.
     *
     * @return the line and column of the failure
     */
    public SourceSpan at() {
        return at;
    }
}
