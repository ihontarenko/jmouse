package org.jmouse.el.renderable;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.node.NodeVisitor;
import org.jmouse.el.renderable.node.MacroNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacroLinker implements NodeVisitor {

    private final static Logger LOGGER = LoggerFactory.getLogger(MacroLinker.class);

    private final Template          template;
    private final EvaluationContext context;

    public MacroLinker(Template template, EvaluationContext context) {
        this.template = template;
        this.context = context;
    }

    @Override
    public void visit(MacroNode node) {
        LOGGER.info("Registering macros '{}' into '{}' template", node.getName(), template.getName());
        template.setMacro(new TemplateMacro(node.getName()));
    }

}
