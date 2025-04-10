package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.NameNode;
import org.jmouse.el.renderable.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PreProcessingVisitor is responsible for traversing the template's AST and registering
 * macros, blocks, and handling template extension directives before the rendering process.
 * <p>
 * This visitor processes container nodes by recursively visiting their children.
 * For {@code MacroNode} instances, it registers the macro definition with the associated template.
 * For {@code BlockNode} instances, it evaluates the block name, converts it to a String,
 * creates a new template block, and registers it in the template's registry.
 * For {@code ExtendsNode} instances, it evaluates the parent template location, loads the parent template,
 * and sets up the inheritance relationship.
 * </p>
 */
public class PreProcessingVisitor implements NodeVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreProcessingVisitor.class);

    private final EvaluationContext context;
    private final Template          template;

    /**
     * Constructs a PreProcessingVisitor for the specified template and evaluation context.
     *
     * @param template the template to process
     * @param context  the evaluation context containing conversion and other runtime data
     */
    public PreProcessingVisitor(Template template, EvaluationContext context) {
        this.context = context;
        this.template = template;
    }

    /**
     * Visits a ContainerNode by recursively processing all its child nodes.
     *
     * @param container the container node whose children are to be processed
     */
    @Override
    public void visit(ContainerNode container) {
        for (Node child : container.getChildren()) {
            child.accept(this);
        }
    }

    /**
     * Visits a SetNode.
     *
     * @param setNode the block node to process
     */
    @Override
    public void visit(SetNode setNode) {
        context.setValue(setNode.getVariable(), setNode.getValue().evaluate(context));
    }

    /**
     * Visits an ImportNode.
     *
     * @param importNode the import node to process
     */
    @Override
    public void visit(ImportNode importNode) {
        Conversion       conversion = context.getConversion();
        Object           source     = importNode.getPath().evaluate(context);
        String           name       = conversion.convert(source, String.class);
        TemplateRegistry self       = template.getRegistry();
        Template         imported   = self.getEngine().getTemplate(name);

        Object evaluated = importNode.getScope().evaluate(context);
        String scope     = conversion.convert(evaluated, String.class);

        imported.getRoot().accept(new PreProcessingVisitor(imported, context));

        self.importFrom(imported.getRegistry(), scope);
    }

    /**
     * Visits an FromNode.
     *
     * @param from the import node to process
     */
    @Override
    public void visit(FromNode from) {
        Conversion       conversion = context.getConversion();
        Object           path       = from.getPath().evaluate(context);
        String           name       = conversion.convert(path, String.class);
        TemplateRegistry self       = template.getRegistry();
        Template         imported   = self.getEngine().getTemplate(name);

        imported.getRoot().accept(new PreProcessingVisitor(imported, context));

        for (NameNode nameNode : from.getNameSet().getSet()) {
            Macro macro = imported.getMacro(nameNode.getName());
            if (macro != null) {
                String macroName = nameNode.getAlias() == null ? nameNode.getName() : nameNode.getAlias();
                self.registerMacro(macroName, imported.getMacro(nameNode.getName()));
            }
        }
    }

    /**
     * Visits a MacroNode by registering the macro with the template.
     *
     * @param node the macro node to register
     */
    @Override
    public void visit(MacroNode node) {
        LOGGER.info("Registering macro '{}' into template '{}'", node.getName(), template.getName());
        template.setMacro(new TemplateMacro(node.getName(), node, template.getName()));
    }

    /**
     * Processes a BlockNode by evaluating its name, creating a template block, and registering it.
     * <p>
     * It uses the current EvaluationContext to evaluate and convert the block name to a String,
     * then creates a TemplateBlock and registers it in the template's registry.
     * </p>
     *
     * @param node the BlockNode to process
     */
    @Override
    public void visit(BlockNode node) {
        Conversion conversion = context.getConversion();
        Object     evaluated  = node.getName().evaluate(context);
        String     name       = conversion.convert(evaluated, String.class);

        LOGGER.info("Registered block '{}' into template '{}'", name, template.getName());
        template.setBlock(new TemplateBlock(name, node, template.getName()));
    }

    /**
     * Processes an ExtendsNode by evaluating the parent template location, loading the parent template,
     * and setting it as the parent for the current template.
     * <p>
     * The parent's location is evaluated using the EvaluationContext and converted to a String.
     * The parent template is then retrieved from the Engine, and the inheritance relationship is set.
     * </p>
     *
     * @param node the ExtendsNode containing the parent template information
     */
    @Override
    public void visit(ExtendsNode node) {
        Object     value      = node.getPath().evaluate(context);
        Conversion conversion = context.getConversion();
        String     location   = conversion.convert(value, String.class);
        Template   parent     = template.getRegistry().getEngine().getTemplate(location);

        LOGGER.info("Inherited parent template '{}' for template '{}'", location, template.getName());

        template.setParent(parent, context);
    }
}
