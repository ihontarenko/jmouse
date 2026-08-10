package org.jmouse.access.policy;

import org.jmouse.access.policy.model.SourceSpan;

/**
 * One thing wrong with a policy, and where it was written.
 *
 * <p>Binding reports <strong>every</strong> problem rather than the first, and until now it reported
 * them as a single joined string. That reads well in a startup log and is useless to an editor: a
 * screen showing a policy file has to put a marker on line 41, which needs the line rather than a
 * paragraph mentioning it.
 *
 * <p>So the aggregate keeps its shape and gains its parts. {@link PolicyException#getMessage()} is
 * unchanged for whoever reads a stack trace; {@link PolicyException#problems()} is the same content
 * addressed by position, for whoever is drawing a gutter.
 *
 * @param at      where in the document the offending declaration begins, or {@link SourceSpan#none()}
 *                where the complaint is about the document as a whole — a scope the application
 *                registers and the file forgot has no line to point at, and inventing one would put a
 *                marker on an innocent declaration
 * @param message what is wrong with it, in the words the log would have used
 */
public record PolicyProblem(SourceSpan at, String message) {

    /** A complaint about a particular line. */
    public static PolicyProblem at(SourceSpan where, String message) {
        return new PolicyProblem(where == null ? SourceSpan.none() : where, message);
    }

    /** A complaint about the document rather than about any one line in it. */
    public static PolicyProblem anywhere(String message) {
        return new PolicyProblem(SourceSpan.none(), message);
    }

    /** Whether this complaint can be put beside a line. */
    public boolean namesAPlace() {
        return at.line() > 0;
    }

    /** The complaint as one line of a log — the spelling every message in this module already had. */
    @Override
    public String toString() {
        return namesAPlace() ? at + ": " + message : message;
    }
}
