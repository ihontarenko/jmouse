package org.jmouse.script.el.host;

import org.jmouse.script.el.SourceSpan;

/**
 * A {@code .jms} file that parses and cannot be loaded.
 *
 * <h2>⚠️ At load, never later</h2>
 *
 * <p>Every failure this reports is a name the host does not know: an event nobody fires, a facade
 * nobody declared, a function nobody registered. All of them are knowable the moment the file is read,
 * and all of them are reported then — never silently no-opped, and never deferred to the first time a
 * handler happens to fire. A script that fails on minute forty of a session is the failure mode this
 * class exists to prevent.</p>
 *
 * <p>Distinct from {@link org.jmouse.script.el.ScriptParseException} so that a syntax mistake and a
 * host mismatch never look alike to whoever has to read the message.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptBindException extends RuntimeException {

    private final transient SourceSpan at;
    private final           String     detail;

    /**
     * Constructs a bind failure anchored at a position in the file.
     *
     * @param at     where in the file the offending construction begins
     * @param detail what is wrong with it
     */
    public ScriptBindException(SourceSpan at, String detail) {
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
     * A check knows a node and a line and has no business knowing which document it belongs to — that is
     * the binder's, and it is one place rather than a name threaded through every check that might ever
     * fail.</p>
     *
     * @param document what the file is called
     * @return this failure, naming the file
     */
    public ScriptBindException in(String document) {
        return at.namesADocument() ? this : new ScriptBindException(at.in(document), detail);
    }
}
