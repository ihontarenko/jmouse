package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.NameNode;
import org.jmouse.el.node.expression.NameSetNode;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The InitializerVisitor is responsible for traversing the template's AST and registering
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
public class InitializerVisitor implements NodeVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitializerVisitor.class);

    private final EvaluationContext context;
    private final Template          template;

    /**
     * Constructs a InitializerVisitor for the specified template and evaluation context.
     *
     * @param template the template to process
     * @param context  the evaluation context containing conversion and other runtime data
     */
    public InitializerVisitor(Template template, EvaluationContext context) {
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
     * Processes a UseNode by importing macro or block definitions from an external template.
     * <p>
     * The method performs the following steps:
     * <ul>
     *   <li>Evaluates the path expression to determine the external template name and retrieves it via the engine.</li>
     *   <li>If the imported template is not initialized, its AST is preprocessed using an {@code InitializerVisitor} and
     *       it is marked as initialized; otherwise, a log entry is recorded indicating it is already initialized.</li>
     *   <li>If a set of specific names is provided, each name in the set is evaluated and used to locally register the
     *       corresponding macro or block definition from the imported template.</li>
     *   <li>If no names are provided but an alias is specified, then the entire set of macros or blocks is copied from
     *       the imported template's registry into the current registry with keys prefixed by the alias.</li>
     * </ul>
     * </p>
     *
     * @param useNode the UseNode containing the external template path, the type (macro or block),
     *                and either a set of names or an alias for importing definitions
     */
    @Override
    public void visit(UseNode useNode) {
        Conversion       conversion = context.getConversion();
        Object           source     = useNode.getPath().evaluate(context);
        String           name       = conversion.convert(source, String.class);
        TemplateRegistry self       = template.getRegistry();
        Template         imported   = self.getEngine().getTemplate(name);
        Token.Type       type       = useNode.getType();

        // Initialize the imported template if it has not been initialized already.
        if (!imported.isInitialized()) {
            imported.getRoot().accept(new InitializerVisitor(imported, context));
            imported.setInitialized();
        } else {
            LOGGER.info("Already initialized: {}", useNode);
        }

        // If specific names are provided, register each corresponding definition.
        if (useNode.getNames() != null) {
            NameSetNode names = useNode.getNames();

            for (NameNode nameNode : names.getSet()) {
                String innerName = conversion.convert(nameNode.evaluate(context), String.class);
                if (type == TemplateToken.T_MACRO) {
                    self.registerMacro(innerName, imported.getMacro(nameNode.getName()));
                } else if (type == TemplateToken.T_BLOCK) {
                    self.registerBlock(innerName, imported.getBlock(nameNode.getName()));
                }
            }
        } else {
            // If no names are provided, use the alias (if any) to copy all definitions.
            String alias = null;

            if (useNode.getAlias() != null) {
                alias = conversion.convert(useNode.getAlias().evaluate(context), String.class);
            }

            if (type == TemplateToken.T_MACRO) {
                self.copyMacros(imported.getRegistry(), alias);
            } else if (type == TemplateToken.T_BLOCK) {
                self.copyBlocks(imported.getRegistry(), alias);
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
        Object           evaluated  = node.getName().evaluate(context);
        Conversion       conversion = context.getConversion();
        String           name       = conversion.convert(evaluated, String.class);
        TemplateRegistry registry   = template.getRegistry();


        if (!registry.hasBlock(name) || node.isOverride()) {
            LOGGER.info("Registering block '{}' into template '{}'", name, template.getName());
            registry.registerBlock(name, new TemplateBlock(name, node, template.getName()));
        } else {
            LOGGER.warn("Template '{}' already has '{}' block", template, name);
        }
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
