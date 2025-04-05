package org.jmouse.template;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.rendering.Template;
import org.jmouse.template.node.BlockNode;
import org.jmouse.template.node.ExtendsNode;
import org.jmouse.template.node.ImportNode;
import org.jmouse.template.node.MacroNode;

import java.util.Map;

public class PreprocessingVisitor implements NodeVisitor {

    private final TemplateRegistry  registry;
    private final EvaluationContext context;
    private final Template          template;

    public PreprocessingVisitor(TemplateRegistry registry, EvaluationContext context, Template template) {
        this.registry = registry;
        this.context = context;
        this.template = template;
    }

    @Override
    public void visit(BlockNode blockNode) {
        registry.registerBlock(blockNode.getName(), new TemplateBlock(blockNode.getName(), blockNode));
    }

    @Override
    public void visit(MacroNode macroNode) {
        registry.registerMacro(macroNode.getMacroName(), macroNode);
    }

    @Override
    public void visit(ExtendsNode extendNode) {
        Template parentTemplate = registry.getEngine().getTemplate(extendNode.getParentTemplateName());
        template.setParent(parentTemplate);
    }

    @Override
    public void visit(ImportNode importNode) {
        Template imported = registry.getEngine().getTemplate(importNode.getTemplate());

        if (imported != null) {
            TemplateRegistry importedRegistry = imported.getRegistry();
            for (Map.Entry<String, MacroNode> entry : importedRegistry.getMacroDefinitions().entrySet()) {
                String aliasedMacroName = importNode.getAlias() + "." + entry.getKey();
                MacroNode originalMacro = entry.getValue();
                MacroNode aliasedMacro = new MacroNode(aliasedMacroName, originalMacro.getMacroBody());
                registry.registerMacro(aliasedMacroName, aliasedMacro);
            }
        }
    }

}