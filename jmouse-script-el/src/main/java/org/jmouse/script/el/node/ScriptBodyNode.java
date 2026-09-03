package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ScopedChain;
import org.jmouse.el.language.node.IfBranchNode;
import org.jmouse.el.language.node.IfNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.script.el.budget.ScriptExecution;

import java.util.List;

/**
 * A node whose children are statements, evaluated in the order they were written.
 *
 * <p>Every word-delimited construction in the language is one of these — a handler, a behaviour's
 * function, a declared function, a loop. What separates them is their header; what they do with their
 * body is this one method.</p>
 *
 * <h2>⚠️ A body owns its {@code local} names</h2>
 *
 * <p>A body that declares a {@code local} runs inside a scope of its own, pushed on entry and popped on
 * the way out — including on the way out through a {@code return}, which is why the pop sits in a
 * {@code finally}.</p>
 *
 * <p>Without it, {@code local} is a lie. Two handlers written for one event share an evaluation
 * context, so the first handler's {@code local weight} is still there when the second one runs: a
 * second handler that reads {@code weight} without declaring it gets the first one's value, silently,
 * and only sometimes — the failure appears when somebody adds a handler <em>above</em> it, months
 * later, and nothing anywhere points at the cause. A loop variable named after something the host put
 * in the context is the same fault with a shorter fuse.</p>
 *
 * <h2>⚠️ And a body that declares none pays nothing</h2>
 *
 * <p>Pushing a scope costs a map, and some hosts dispatch inside a fixed-rate loop. Whether a body
 * needs one is a property of its text, so it is decided once, on first evaluation, and remembered — a
 * handler that only calls facades never allocates.</p>
 *
 * <p>The scan looks through {@code if} branches, because a branch is not a scope here: a {@code local}
 * written inside one belongs to the body that holds it. A nested body — a loop, a declared function —
 * is <strong>not</strong> scanned, because it pushes its own.</p>
 *
 * <p>⚠️ <strong>Statements are read by index, not iterated.</strong> An enhanced {@code for} over a
 * list allocates an iterator per pass — small, and multiplied by every body, every branch and every
 * frame. The list is a {@link java.util.RandomAccess} view that {@link ExpressionsNode} hands back
 * without copying, so reading it by index costs nothing at all.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
abstract public class ScriptBodyNode extends ExpressionsNode {

    private Boolean scoped;

    /**
     * Evaluates every statement in this body and returns what the last one produced.
     *
     * @param context the evaluation context
     * @return the value of the final statement, or {@code null} for an empty body
     */
    protected Object evaluateBody(EvaluationContext context) {
        if (!needsScope()) {
            return runStatements(context);
        }

        ScopedChain chain = context.getScopedChain();

        chain.push();

        try {
            return runStatements(context);
        } finally {
            chain.pop();
        }
    }

    /**
     * Whether this body has to run inside a scope of its own.
     *
     * <p>Decided once and remembered. A subclass that binds a name of its own — a loop and its
     * variable — says so by overriding.</p>
     *
     * @return {@code true} when the body declares a name that must not outlive it
     */
    protected boolean needsScope() {
        if (scoped == null) {
            scoped = declaresLocals(getExpressions());
        }

        return scoped;
    }

    /**
     * Runs the statements in order, without touching the scope chain.
     *
     * <p>⚠️ <strong>The execution is read once, here, and held in a local.</strong> It is a map lookup,
     * and the loop below it may run sixty times a second — asking the context per statement would make
     * the budget cost more than the statements it is counting. A host that declared no budget gets
     * {@code null} and one null check per statement, which is what "a budget with nothing set costs
     * nothing" means in practice.</p>
     *
     * @param context the evaluation context
     * @return the value of the final statement
     */
    private Object runStatements(EvaluationContext context) {
        List<Expression> statements = getExpressions();
        ScriptExecution  execution  = ScriptExecution.from(context);
        Object           result     = null;

        for (int index = 0; index < statements.size(); index++) {
            if (execution != null) {
                execution.step(describe());
            }

            result = statements.get(index).evaluate(context);
        }

        return result;
    }

    /**
     * What to call this body in a refusal — "the handler for 'unload'", "the function 'tick'".
     *
     * <p>⚠️ A constant per node, not a string built per statement. It is read on the counting path.</p>
     *
     * @return a phrase naming this body
     */
    protected abstract String describe();

    /**
     * Whether any statement written directly in this body — branches included — binds a local name.
     *
     * @param statements the statements to scan
     * @return {@code true} when at least one {@code local} belongs to this body
     */
    private static boolean declaresLocals(List<Expression> statements) {
        for (int index = 0; index < statements.size(); index++) {
            Expression statement = statements.get(index);

            if (statement instanceof LocalNode) {
                return true;
            }

            if (statement instanceof IfNode branch) {
                for (IfBranchNode alternative : branch.getBranches()) {
                    if (declaresLocals(alternative.getExpressions())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
