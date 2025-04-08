package org.jmouse.el.renderable;

import org.jmouse.el.node.Node;
import org.jmouse.el.renderable.node.ContainerNode;
import org.jmouse.el.renderable.node.MacroNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacroLinker implements NodeVisitor {

    private final static Logger LOGGER = LoggerFactory.getLogger(MacroLinker.class);

    private final Template template;

    public MacroLinker(Template template) {
        this.template = template;
    }

    @Override
    public void visit(MacroNode node) {
        LOGGER.info("Registering macros '{}' into '{}' template", node.getName(), template.getName());
        template.setMacro(new TemplateMacro(node.getName(), node, template.getName()));
    }

    @Override
    public void visit(ContainerNode container) {
        for (Node child : container.getChildren()) {
            child.accept(this);
        }
    }

}
