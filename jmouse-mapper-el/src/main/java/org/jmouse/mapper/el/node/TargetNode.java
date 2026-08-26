package org.jmouse.mapper.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything about building one type: {@code target Order { … }}.
 *
 * <p>Grouping by target rather than by pair is deliberate. One target is commonly built from two or
 * three sources — a request, a database row, a message — and those belong on one screen, because the
 * interesting thing about them is how they differ. ⚠️ The pair remains the unit a rule applies to; the
 * target is only how the file is organised, and nothing here may quietly become per-target behaviour.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class TargetNode extends AbstractExpression {

    private final List<FromNode>  sources  = new ArrayList<>();
    private final List<RefuseNode> refusals = new ArrayList<>();

    private String        targetType;
    private RuleBlockNode always;
    private Unmapped      unmapped = Unmapped.IGNORE;

    /**
     * What happens to a writable target property fed by nothing — no rule, no {@code always} rule, and
     * no same-named source property.
     *
     * <p>The default is {@link #IGNORE}, which is how a mapping with no file at all behaves, so adding
     * the switch changes nothing that already works.</p>
     */
    public enum Unmapped { IGNORE, FAIL }

    /** @return the target type, as the file names it */
    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /** @return the rules that hold whatever the source is, or {@code null} where there are none */
    public RuleBlockNode getAlways() {
        return always;
    }

    public void setAlways(RuleBlockNode always) {
        this.always = always;
    }

    /** @return whether an unfed property refuses the file */
    public Unmapped getUnmapped() {
        return unmapped;
    }

    public void setUnmapped(Unmapped unmapped) {
        this.unmapped = unmapped;
    }

    /**
     * Adds a source this target may be built from.
     *
     * @param source the source block
     */
    public void add(FromNode source) {
        sources.add(source);
    }

    /** @return the sources, in the order written */
    public List<FromNode> getSources() {
        return sources;
    }

    /**
     * Adds a target-level refusal.
     *
     * <p>⚠️ Target refusals sit here rather than inside a {@code from} because a target invariant holds
     * whatever the source is. A source refusal sits inside its {@code from}, because it names that
     * source type.</p>
     *
     * @param refusal the block to add
     */
    public void add(RefuseNode refusal) {
        refusals.add(refusal);
    }

    /** @return the target-level refusals, in the order written */
    public List<RefuseNode> getRefusals() {
        return refusals;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "target %s [%d sources]".formatted(targetType, sources.size());
    }
}
