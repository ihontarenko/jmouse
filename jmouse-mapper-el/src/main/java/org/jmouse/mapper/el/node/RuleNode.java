package org.jmouse.mapper.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * One rule: {@code targetProperty : expression [when condition]}, or {@code targetProperty : ignore}.
 *
 * <h2>⚠️ The value is raw text, on purpose</h2>
 *
 * <p>Nothing here looks inside it. Modelling the expression would be a second implementation of a
 * grammar jMouse EL already owns, and the day the two disagree is the day a mapping writes something
 * nobody asked for. The reader slices it out of the source and the binder compiles it — see
 * {@link org.jmouse.mapper.el.parser.RuleValueReader} for why it cannot be parsed where it is read.</p>
 *
 * <p>Keeping it verbatim also means the text shown back to a person is the text they wrote, spacing and
 * parentheses included, rather than <em>a</em> spelling of it rendered from a tree.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RuleNode extends AbstractExpression {

    private String  property;
    private String  value;
    private String  condition;
    private boolean ignored;

    /** @return the target property this rule fills */
    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * @return the value expression exactly as it was typed, or {@code null} when the rule is an
     *         {@code ignore}
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * The condition after {@code when}, exactly as it was typed.
     *
     * <p>⚠️ A false condition writes <strong>nothing</strong> — the target property keeps whatever it
     * held. That is not what a ternary does, and the difference is the reason this field exists rather
     * than the value carrying its own conditional.</p>
     *
     * @return the condition as written, or {@code null} where the rule carries none
     */
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Whether this rule says the property is deliberately not carried.
     *
     * <p>⚠️ An ignore is unconditional and never combines with a condition: a conditional ignore is
     * spelled as a condition on the rule that would otherwise have written.</p>
     *
     * @return {@code true} for {@code property : ignore}
     */
    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        if (ignored) {
            return "%s : ignore".formatted(property);
        }

        return condition == null
                ? "%s : %s".formatted(property, value)
                : "%s : %s when %s".formatted(property, value, condition);
    }
}
