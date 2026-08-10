package org.jmouse.access.policy;

import org.jmouse.access.policy.model.SourceSpan;

import java.util.List;

/**
 * A policy that will not load.
 *
 * <p>⚠️ Always thrown at <strong>load</strong>, never at decision time. That is the whole argument for
 * writing authorization down rather than inserting rows: a rule that is wrong should stop the
 * application, not go quiet and let a route answer differently from what the file says.
 *
 * <p>It carries its complaints twice, and both readings are load-bearing. {@link #getMessage()} is
 * the paragraph a startup log prints; {@link #problems()} is the same content as {@link PolicyProblem}s
 * addressed by line, which is what an editor needs to put a marker where the mistake is. A screen
 * built on the paragraph would have to parse English back into coordinates.
 */
public class PolicyException extends RuntimeException {

    private final transient List<PolicyProblem> problems;

    public PolicyException(String message) {
        this(message, List.of(PolicyProblem.anywhere(message)));
    }

    public PolicyException(String message, Throwable cause) {
        super(message, cause);
        this.problems = List.of(PolicyProblem.anywhere(message));
    }

    /**
     * The aggregate form: a summary, and every problem that produced it.
     *
     * @param summary  what the log reads, already carrying the problems in prose
     * @param problems the same complaints, each addressed by position
     */
    public PolicyException(String summary, List<PolicyProblem> problems) {
        super(summary);
        this.problems = List.copyOf(problems);
    }

    /** The same, with the line that caused it — which is most of what makes a policy file editable. */
    public static PolicyException at(SourceSpan where, String message) {
        PolicyProblem problem = PolicyProblem.at(where, message);
        return new PolicyException(problem.toString(), List.of(problem));
    }

    public static PolicyException at(SourceSpan where, String message, Throwable cause) {
        return new PolicyException(PolicyProblem.at(where, message).toString(), cause);
    }

    /** Every complaint this failure is made of, each knowing where it belongs. */
    public List<PolicyProblem> problems() {
        return problems;
    }
}
