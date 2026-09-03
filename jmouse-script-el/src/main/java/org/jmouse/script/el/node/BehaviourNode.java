package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.ExpressionsNode;

import java.util.List;

/**
 * {@code behaviour "gatherer" do … end} — the mechanic half of a file.
 *
 * <p>A behaviour is a named bag of functions a host drives on its own schedule. There is no state
 * machine construction in this language and there is not going to be one: a state machine is
 * {@code if} / {@code elseif} on a field plus facade calls, which is a shape anybody can read and
 * step through, and an arrow syntax would be a second language to learn for the same result.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BehaviourNode extends ExpressionsNode {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the functions this behaviour is made of.
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
        return "BEHAVIOUR['%s']".formatted(name);
    }

}
