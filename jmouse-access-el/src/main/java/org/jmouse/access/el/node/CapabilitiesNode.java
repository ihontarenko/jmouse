package org.jmouse.access.el.node;

import org.jmouse.access.el.PolicyParseException;
import org.jmouse.access.policy.model.PolicyCapabilityDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code capabilities { … }} — everything a grant in this installation can be about.
 *
 * <p>Two kinds of line, deliberately: {@code gate|limit|quota} declares a capability's <em>shape</em>,
 * and {@code paid} names the ones that are closed until something grants them. They are independent —
 * a seat is a limit and is paid — so the block collects both and joins them here rather than making
 * either line carry the other's fact.</p>
 *
 * <p>⚠️ A {@code paid} line naming a key the block never declares is refused. Silently ignoring it
 * would leave somebody believing they had closed a capability that is wide open, which is the worst
 * way for this file to be wrong.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class CapabilitiesNode extends PolicyBlockNode {

    /**
     * Returns the declarations in this block, in the order they were written.
     *
     * @throws org.jmouse.access.el.PolicyParseException when the block holds anything else, or a
     *                                                   {@code paid} line names something undeclared
     */
    public List<PolicyCapabilityDeclaration> toCapabilityDeclarations() {
        List<CapabilityDeclarationNode> declared = new ArrayList<>();
        Set<String>                     paid     = new LinkedHashSet<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof CapabilityDeclarationNode declaration) {
                declared.add(declaration);
            } else if (expression instanceof PaidCapabilitiesNode line) {
                paid.addAll(line.getKeys());
            } else {
                throw reject(expression, "a capabilities block holds declarations like "
                        + "'limit seat \"Seats\" per organization' and 'paid custody' lines");
            }
        }

        return join(declared, paid);
    }

    private List<PolicyCapabilityDeclaration> join(
            List<CapabilityDeclarationNode> declared, Set<String> paid) {

        List<PolicyCapabilityDeclaration> declarations = new ArrayList<>();
        Set<String>                       keys         = new LinkedHashSet<>();

        for (CapabilityDeclarationNode declaration : declared) {
            keys.add(declaration.getKey());
            declarations.add(declaration.toCapabilityDeclaration(paid.contains(declaration.getKey())));
        }

        for (String key : paid) {
            if (!keys.contains(key)) {
                throw new PolicyParseException(
                        SourceSpanNode.at(this),
                        ("'%s' is marked paid and never declared. Declare it in this block — "
                         + "'gate %s \"…\"' — or the capability that line was meant to close stays "
                         + "wide open, and nothing would have said so").formatted(key, key));
            }
        }

        return declarations;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toCapabilityDeclarations();
    }

    @Override
    protected String describe() {
        return "a capabilities block";
    }

    @Override
    public String toSource() {
        return renderBlock("capabilities");
    }

    @Override
    public String toString() {
        return "capabilities";
    }
}
