package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.ExpressionsNode;

import java.util.List;

/**
 * {@code script "slice-01" { … }} — the story half of a file: handlers, and the functions they call.
 *
 * <p>Written between braces rather than {@code do … end} because it is a file-scope wrapper rather
 * than a body: it holds declarations, nothing in it runs in order, and a brace says so at a glance.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptNode extends ExpressionsNode {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the handlers declared in this block, in the order they were written.
     *
     * <p>⚠️ Order is kept because two handlers may be written for one event, and "the order they are
     * written in" is the only answer to <em>which runs first</em> that a person can predict from the
     * file.</p>
     *
     * @return the handlers
     */
    public List<HandlerNode> getHandlers() {
        return getExpressions().stream()
                .filter(HandlerNode.class::isInstance).map(HandlerNode.class::cast).toList();
    }

    /**
     * Returns the functions declared in this block.
     *
     * @return the function declarations
     */
    public List<FunctionDeclarationNode> getFunctions() {
        return getExpressions().stream()
                .filter(FunctionDeclarationNode.class::isInstance)
                .map(FunctionDeclarationNode.class::cast).toList();
    }

    /**
     * ⚠️ A block of declarations evaluates to itself — see {@link ScriptDocumentNode#evaluate}.
     *
     * @param context the evaluation context, unused
     * @return this block
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "SCRIPT['%s']".formatted(name);
    }

}
