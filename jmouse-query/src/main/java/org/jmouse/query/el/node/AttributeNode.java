package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.query.el.SourceWriter;

/**
 * {@code attribute entry[quantity] from "f-quantity" unknown in bag} — one thing a query may write.
 *
 * <p>Four facts: how a query <em>writes</em> it, what the <em>store</em> calls it, what kind of value it
 * holds, and how it is reached.</p>
 *
 * <h2>⚠️ Two names, not one</h2>
 *
 * <p>A query writes {@code issue.points} for a column the table calls {@code story_points}, and
 * {@code entry[quantity]} for a bag row keyed by the id {@code f-quantity}. Keeping both means nothing
 * downstream has to take a written path apart to recover a stored name — which is what a product used to
 * do, re-implementing this language's own spelling rules to do it.</p>
 *
 * <h2>⚠️ {@code unknown} is a real answer, and the most important one</h2>
 *
 * <p>It says <em>nobody has promised what this holds</em>, and that is what makes an ordered comparison
 * over it refuse until a converter is given. Declaring {@code text} instead is a <strong>promise</strong>
 * — believed, and then {@code >} compares words. A bag full of numbers stored as text is exactly where
 * the difference decides whether {@code "900" > "1000"} is answered wrongly.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AttributeNode extends AbstractExpression {

    private String  name;
    private String  source;
    private String  type;
    private String  access;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /** {@code text}, {@code number}, {@code boolean}, {@code temporal} or {@code unknown}. */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * How it is reached — {@code column}, {@code bag}, {@code join} or {@code collection}.
     *
     * <p>⚠️ A word rather than a boolean, and it was a boolean until there were more than two ways. The
     * flag had to be read as "bag, or else a column", which is exactly the shape that silently answers
     * "column" for anything new.</p>
     */
    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    @Override
    public String toSource() {
        return "attribute %s from %s %s in %s".formatted(
                SourceWriter.name(name), SourceWriter.name(source), type, access);
    }

    @Override
    public String toString() {
        return toSource();
    }
}
