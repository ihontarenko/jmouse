package org.jmouse.mapper.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@code refuse source|target before|after { … }} block.
 *
 * <h2>⚠️ Two of the four combinations carry the whole design</h2>
 *
 * <p><strong>{@code source after} is refused when the file is read.</strong> A mapping does not modify
 * its source, so the assertion would be the same test performed later, having built and filled a target
 * only to throw it away. The grid allows it; the language does not.</p>
 *
 * <p><strong>{@code target before} runs only when the caller supplied the target instance.</strong> It
 * exists so an object carrying a marker — a lock, a version, a terminal state — can refuse the write
 * before anything touches it. ⚠️ It does <em>not</em> run when the mapper constructs the target: a
 * fresh object carries nothing but type defaults, and every assertion about it would be an assertion
 * about {@code null} and zero. That means the block legitimately does not run for half the call sites,
 * which is exactly the shape of a guarantee that is not one — so it has to stay visible from outside,
 * here and wherever it is reported.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RefuseNode extends AbstractExpression {

    private final List<AssertionNode> assertions = new ArrayList<>();

    private Subject subject;
    private Phase   phase;

    /** What a block is about. */
    public enum Subject { SOURCE, TARGET }

    /** When it runs. */
    public enum Phase { BEFORE, AFTER }

    /** @return whether this block asserts about the source or the target */
    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    /** @return whether it runs before the mapping or after it */
    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    /**
     * Adds an assertion.
     *
     * <p>⚠️ Every assertion in a block is evaluated, never short-circuited at the first that holds, so
     * one run reports everything wrong with the data rather than one thing at a time.</p>
     *
     * @param assertion the assertion to add
     */
    public void add(AssertionNode assertion) {
        assertions.add(assertion);
    }

    /** @return the assertions, in the order written */
    public List<AssertionNode> getAssertions() {
        return assertions;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "refuse %s %s [%d]".formatted(
                subject.name().toLowerCase(), phase.name().toLowerCase(), assertions.size());
    }
}
