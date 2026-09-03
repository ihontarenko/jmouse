package org.jmouse.mapper.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.ExpressionsNode;

/**
 * The rules for one source of the enclosing target: {@code from OrderRequest { … }}.
 *
 * <p>Or, where a source becomes a target in one step rather than property by property, the expression
 * that does it: {@code from BigDecimal : via("money")}. ⚠️ A {@code from} is one or the other, never
 * both — a block of rules beside a whole-object conversion would leave no answer to what the rules were
 * supposed to apply to.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FromNode extends ExpressionsNode {

    private String        sourceType;
    private RuleBlockNode rules;
    private RefuseNode    refusal;
    private String        conversion;

    /** @return the source type, as the file names it */
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /** @return the rules, or {@code null} when this is a whole-pair conversion */
    public RuleBlockNode getRules() {
        return rules;
    }

    public void setRules(RuleBlockNode rules) {
        this.rules = rules;
    }

    /**
     * @return the {@code refuse source before} block for this source, or {@code null} where it carries
     *         none
     */
    public RefuseNode getRefusal() {
        return refusal;
    }

    public void setRefusal(RefuseNode refusal) {
        this.refusal = refusal;
    }

    /**
     * @return the expression converting the whole object, exactly as typed, or {@code null} when this
     *         is a block of rules
     */
    public String getConversion() {
        return conversion;
    }

    public void setConversion(String conversion) {
        this.conversion = conversion;
    }

    /** @return {@code true} when the pair converts whole rather than property by property */
    public boolean isConverted() {
        return conversion != null;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return isConverted()
                ? "from %s : %s".formatted(sourceType, conversion)
                : "from %s { … }".formatted(sourceType);
    }
}
