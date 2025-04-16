package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.Node;

/**
 * Represents a template in the template engine.
 * <p>
 * A {@code Template} encapsulates the source, abstract syntax tree, and associated definitions
 * (such as macros and blocks) for a template. It is also responsible for managing the evaluation
 * context and inheritance hierarchy.
 * </p>
 */
public interface Template {

    /**
     * Marks this template as initialized.
     */
    void setInitialized();

    /**
     * Indicates whether this template has been initialized.
     *
     * @return {@code true} if the template is initialized; {@code false} otherwise
     */
    boolean isInitialized();

    /**
     * Checks if the template is not yet initialized.
     *
     * @return {@code true} if the template is uninitialized; {@code false} if it is initialized
     */
    default boolean isUninitialized() {
        return !isInitialized();
    }

    /**
     * Creates a new evaluation context for rendering this template.
     * <p>
     * The evaluation context typically contains variable bindings, scope information, and may include
     * inheritance and extension details.
     * </p>
     *
     * @return a new {@link EvaluationContext} instance configured for this template
     */
    EvaluationContext newContext();

    /**
     * Returns the tokenizable source of this template.
     *
     * @return the {@link TokenizableSource} representing the raw source of the template
     */
    TokenizableSource getSource();

    /**
     * Returns the root node of this template's abstract syntax tree.
     *
     * @return the root {@link Node} of the template
     */
    Node getRoot();

    /**
     * Returns the name of this template.
     *
     * @return a {@link String} representing the template's name
     */
    String getName();

    /**
     * Registers a macro definition with this template.
     *
     * @param macro the macro to register
     */
    void setMacro(Macro macro);

    /**
     * Retrieves the macro defined in this template with the given name.
     *
     * @param name the name of the macro
     * @return the {@link Macro} associated with the specified name, or {@code null} if not found
     */
    Macro getMacro(String name);

    /**
     * Registers a block definition with this template.
     *
     * @param block the block to register
     */
    void setBlock(Block block);

    /**
     * Retrieves the block definition with the given name from this template.
     *
     * @param name the name of the block
     * @return the {@link Block} associated with the specified name, or {@code null} if not found
     */
    Block getBlock(String name);

    /**
     * Retrieves the parent template from the current evaluation context's inheritance stack.
     *
     * @param context the evaluation context that holds the inheritance information
     * @return the parent {@link Template} if present, or {@code null} otherwise
     */
    Template getParent(EvaluationContext context);

    /**
     * Sets the parent template using its name.
     * <p>
     * The parent template is identified by its name, which is used to load the template via the engine.
     * </p>
     *
     * @param parent  the name of the parent template
     * @param context the evaluation context used during the parent setting operation
     */
    void setParent(String parent, EvaluationContext context);

    /**
     * Sets the parent template directly.
     *
     * @param parent  the parent {@link Template} to set
     * @param context the evaluation context used during the parent setting operation
     */
    void setParent(Template parent, EvaluationContext context);

    /**
     * Retrieves the registry that holds the definitions (macros and blocks) for this template.
     *
     * @return the {@link TemplateRegistry} associated with this template
     */
    TemplateRegistry getRegistry();
}
