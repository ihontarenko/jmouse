package org.jmouse.mapper.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * One import: {@code use shop.api.OrderRequest}.
 *
 * <p>The simple name is what the rest of the file writes; the qualified name is what it resolves to.
 * ⚠️ Two {@code use} lines importing the same simple name are refused when the file is read — the
 * alternative is a file where a type name means whichever line came last.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class UseNode extends AbstractExpression {

    private String qualifiedName;

    /** @return the fully qualified type name as written */
    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    /**
     * The name the rest of the file refers to this type by.
     *
     * <p>⚠️ Split on the last dot <em>or dollar</em>, whichever comes later. A nested class is written
     * {@code shop.api.Checkout$OrderRequest}, and taking everything after the last dot would leave
     * {@code Checkout$OrderRequest} — a name nobody would write in a rule, so every reference to the
     * type would fail to resolve while the import itself looked perfectly correct.</p>
     *
     * @return the name the file refers to this type by
     */
    public String getSimpleName() {
        if (qualifiedName == null) {
            return null;
        }

        int boundary = Math.max(qualifiedName.lastIndexOf('.'), qualifiedName.lastIndexOf('$'));

        return boundary < 0 ? qualifiedName : qualifiedName.substring(boundary + 1);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "use %s".formatted(qualifiedName);
    }
}
