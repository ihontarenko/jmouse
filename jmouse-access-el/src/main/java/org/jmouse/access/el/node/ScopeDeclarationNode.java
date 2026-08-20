package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyScopeDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * One line of a {@code scopes} block: {@code @SPACE place parameter=spaceId}.
 *
 * <p>The nature is one of {@code everything}, {@code place} or {@code own-rows}. The parser does not
 * know what any of those mean — it reports the word it read, and binding maps it onto a nature the
 * engine understands.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScopeDeclarationNode extends AbstractExpression {

    private String name;
    private String nature;
    private String parameter;

    /** The place this one sits inside, from {@code inside=@X}. */
    private String inside;

    /**
     * A scope this one sits beside, from {@code beside=@X}.
     *
     * <p>⚠️ Sugar over {@link #inside}, not a third relation: "beside X" is "inside whatever X is inside".
     * It exists because sibling-by-omission is invisible in a generated file — a projection that writes
     * nothing where two places are siblings tells a reader nothing about whether that was meant.</p>
     */
    private String beside;

    /**
     * A place a target naming this one must also name, from {@code requires=@X}.
     *
     * <p>⚠️ A different question from {@link #inside}: that one is about a grant reaching, this one is
     * about an address being complete.</p>
     */
    private String requires;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNature() {
        return nature;
    }

    public void setNature(String nature) {
        this.nature = nature;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    /**
     * Returns this declaration as the record stage 2 receives.
     *
     * @return the name, nature, optional parameter, and where it was written
     */
    public String getInside() {
        return inside;
    }

    public void setInside(String inside) {
        this.inside = inside;
    }

    public String getBeside() {
        return beside;
    }

    public void setBeside(String beside) {
        this.beside = beside;
    }

    public String getRequires() {
        return requires;
    }

    public void setRequires(String requires) {
        this.requires = requires;
    }

    public PolicyScopeDeclaration toScopeDeclaration() {
        return new PolicyScopeDeclaration(getName(), getNature(), getParameter(), getInside(),
                                          getBeside(), getRequires(), SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toScopeDeclaration();
    }

    /**
     * ⚠️ {@code beside=} is rendered even though omitting it would parse to the same thing.
     *
     * <p>Siblings are the default, and a default is invisible: a generated file that says nothing where
     * two places sit side by side leaves a reader unable to tell a deliberate arrangement from a
     * forgotten {@code inside=}. Writing it costs one attribute and removes the guess.</p>
     */
    @Override
    public String toSource() {
        StringBuilder declaration = new StringBuilder(
            "@%s %s".formatted(getName(), SourceWriter.name(getNature())));

        if (parameter != null) {
            declaration.append(" parameter=").append(SourceWriter.name(getParameter()));
        }

        if (inside != null) {
            declaration.append(" inside=@").append(inside);
        }

        if (beside != null) {
            declaration.append(" beside=@").append(beside);
        }

        if (requires != null) {
            declaration.append(" requires=@").append(requires);
        }

        return declaration.toString();
    }

    @Override
    public String toString() {
        return toSource();
    }
}
