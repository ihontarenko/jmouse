package org.jmouse.el.renderable.node;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.renderable.AbstractRenderableNode;
import org.jmouse.el.renderable.Content;
import org.jmouse.el.renderable.Template;

/**
 * Represents an "extends" node in a template.
 * <p>
 * This node is used to specify that a template extends a parent template.
 * It evaluates an expression that should resolve to the parent template's name,
 * converts the result to a String, and then sets the parent template in the current template.
 * </p>
 */
public class ExtendsNode extends AbstractRenderableNode {

    private ExpressionNode parent;

    /**
     * Returns the expression node that evaluates to the parent template name.
     *
     * @return the parent expression node
     */
    public ExpressionNode getParent() {
        return parent;
    }

    /**
     * Sets the expression node that represents the parent template.
     *
     * @param parent the parent expression node
     */
    public void setParent(ExpressionNode parent) {
        this.parent = parent;
    }

    /**
     * Renders the "extends" directive.
     * <p>
     * Evaluates the parent expression, converts the result to a String using the conversion service,
     * and sets the parent template in the current template context.
     * </p>
     *
     * @param content the content to render into (unused in this node)
     * @param self    the current template
     * @param context the evaluation context providing conversion and evaluation services
     */
    @Override
    public void render(Content content, Template self, EvaluationContext context) {
        Object     value      = getParent().evaluate(context);
        Conversion conversion = context.getConversion();
        String     parent     = conversion.convert(value, String.class);

        self.setParent(parent, context);
    }
}
