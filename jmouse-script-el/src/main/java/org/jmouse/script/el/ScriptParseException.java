package org.jmouse.script.el;

import org.jmouse.el.parser.ParseException;

/**
 * A {@code .jms} file that will not parse.
 *
 * <p>Stage 1 owns nothing but text, so everything this can report is structural: a body that never
 * closes, a handler with no {@code do}, a comparison written where a value was meant to be set.
 * Whether an event exists, whether an {@code @} name is a declared facade, whether a function was ever
 * registered — none of that is knowable here and all of it is the binder's to refuse.</p>
 *
 * <p>Keeping the two apart is the point: a parser failure and a host failure should never look alike to
 * whoever is reading the message.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptParseException extends ParseException {

    private final transient SourceSpan at;
    private final           String     detail;

    /**
     * Constructs a parse failure anchored at a position in the file.
     *
     * @param at     where in the file the offending construction begins
     * @param detail what is wrong with it
     */
    public ScriptParseException(SourceSpan at, String detail) {
        super(at + ": " + detail);
        this.at = at;
        this.detail = detail;
    }

    /**
     * Returns where in the file the offending construction begins.
     *
     * @return the line and column of the failure
     */
    public SourceSpan at() {
        return at;
    }

    /**
     * Returns what is wrong, without the position in front of it.
     *
     * @return the message on its own
     */
    public String detail() {
        return detail;
    }

    /**
     * Returns the same failure, attributed to a file.
     *
     * <p>⚠️ <strong>The file name is attached at the boundary, not where the failure is raised.</strong>
     * A parser knows a cursor and a line and has no business knowing what the text it is reading is
     * called — {@link ScriptEvaluator} is the only thing that does, and it is one place rather than a
     * name threaded through every parser that might ever fail.</p>
     *
     * @param document what the file is called
     * @return this failure, naming the file
     */
    public ScriptParseException in(String document) {
        return at.namesADocument() ? this : new ScriptParseException(at.in(document), detail);
    }
}
