package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.node.NodeVisitor;
import org.jmouse.el.renderable.node.ExtendsNode;
import org.jmouse.el.renderable.node.ImportNode;

import java.util.Map;

public class PreProcessingVisitor implements NodeVisitor {

    private final TemplateRegistry  registry;
    private final EvaluationContext context;
    private final Template          template;

    public PreProcessingVisitor(TemplateRegistry registry, Template template, EvaluationContext context) {
        this.registry = registry;
        this.context = context;
        this.template = template;
    }

    @Override
    public void visit(ExtendsNode node) {
        Object     value      = node.getParent().evaluate(context);
        Conversion conversion = context.getConversion();
        String     location     = conversion.convert(value, String.class);
        Template parent = registry.getEngine().getTemplate(location);

        template.setParent(parent, context);
    }

    @Override
    public void visit(ImportNode importNode) {
        Template imported = registry.getEngine().getTemplate(importNode.getSource());

        if (imported != null) {
           /* TemplateRegistry importedRegistry = imported.getRegistry();
            for (Map.Entry<String, MacroNode> entry : importedRegistry.getMacroDefinitions().entrySet()) {
                String aliasedMacroName = importNode.getAlias() + "." + entry.getKey();
                MacroNode originalMacro = entry.getValue();
                MacroNode aliasedMacro = new MacroNode(aliasedMacroName, originalMacro.getMacroBody());
                registry.registerMacro(aliasedMacroName, aliasedMacro);
            }*/
        }
    }

}