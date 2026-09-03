package org.jmouse.mapper.el.node;

import org.jmouse.el.node.AbstractExpression;

/**
 * An {@code unmapped fail} or {@code unmapped ignore} line.
 *
 * <p>⚠️ A node rather than a field written straight onto the target, because it arrives through the
 * same dispatch as everything else in a target block. A parser that reached back into its enclosing
 * node to set a field would be the one construction in the language that does not return what it
 * parsed.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class UnmappedNode extends AbstractExpression {

    private TargetNode.Unmapped value = TargetNode.Unmapped.IGNORE;

    /** @return what an unfed target property does */
    public TargetNode.Unmapped getValue() {
        return value;
    }

    public void setValue(TargetNode.Unmapped value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "unmapped " + value.name().toLowerCase();
    }
}
