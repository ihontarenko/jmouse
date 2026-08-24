package org.jmouse.query.el.node;

import org.jmouse.el.node.Expression;

/**
 * {@code where <expression>} — which rows.
 *
 * <p>The condition is an ordinary jME expression and nothing about it is special to this language. That
 * is the design: a {@code where} written in a document and the same condition written into a URL
 * parameter are parsed by one parser in one pass, so they cannot come to mean different things.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class WhereNode extends ClauseNode {

    public static final String KEYWORD = "where";

    private Expression condition;

    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    @Override
    public String keyword() {
        return KEYWORD;
    }

    @Override
    protected String bodyToSource() {
        return condition == null ? "" : condition.toSource();
    }
}
