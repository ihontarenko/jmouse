package org.jmouse.template.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.rendering.AbstractRenderableNode;
import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.RenderableNode;
import org.jmouse.el.rendering.Template;

import java.util.List;

public class MacroNode extends AbstractRenderableNode {

    private String         name;
    private List<String>   arguments;
    private RenderableNode body;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public RenderableNode getBody() {
        return body;
    }

    public void setBody(RenderableNode body) {
        this.body = body;
    }

    @Override
    public void render(Content content, Template self, EvaluationContext context) {
        content.append("");
    }

    @Override
    public String toString() {
        return "MACRO: %s(%s)".formatted(name, arguments);
    }
}
