package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.Node;

/**
 * Represents a view in the view engine.
 * <p>
 * A {@code Template} encapsulates the source, abstract syntax tree, and associated definitions
 * (such as macros and blocks) for a view. It is also responsible for managing the evaluation
 * context and inheritance hierarchy.
 * </p>
 */
public interface Template {

    /**
     * Marks this view as initialized.
     */
    void setInitialized();

    /**
     * Indicates whether this view has been initialized.
     *
     * @return {@code true} if the view is initialized; {@code false} otherwise
     */
    boolean isInitialized();

    /**
     * Checks if the view is not yet initialized.
     *
     * @return {@code true} if the view is uninitialized; {@code false} if it is initialized
     */
    default boolean isUninitialized() {
        return !isInitialized();
    }

    /**
     * Creates a new evaluation context for rendering this view.
     * <p>
     * The evaluation context typically contains variable bindings, scope information, and may include
     * inheritance and extension details.
     * </p>
     *
     * @return a new {@link EvaluationContext} instance configured for this view
     */
    EvaluationContext newContext();

    /**
     * Returns the tokenizable source of this view.
     *
     * @return the {@link TokenizableSource} representing the raw source of the view
     */
    TokenizableSource getSource();

    /**
     * Returns the root node of this view's abstract syntax tree.
     *
     * @return the root {@link Node} of the view
     */
    Node getRoot();

    /**
     * Returns the name of this view.
     *
     * @return a {@link String} representing the view's name
     */
    String getName();

    /**
     * Registers a macro definition with this view.
     *
     * @param macro the macro to register
     */
    void setMacro(Macro macro);

    /**
     * Retrieves the macro defined in this view with the given name.
     *
     * @param name the name of the macro
     * @return the {@link Macro} associated with the specified name, or {@code null} if not found
     */
    Macro getMacro(String name);

    /**
     * Registers a block definition with this view.
     *
     * @param block the block to register
     */
    void setBlock(Block block);

    /**
     * Retrieves the block definition with the given name from this view.
     *
     * @param name the name of the block
     * @return the {@link Block} associated with the specified name, or {@code null} if not found
     */
    Block getBlock(String name);

    /**
     * Retrieves the parent view from the current evaluation context's inheritance stack.
     *
     * @param context the evaluation context that holds the inheritance information
     * @return the parent {@link Template} if present, or {@code null} otherwise
     */
    Template getParent(EvaluationContext context);

    /**
     * Sets the parent view using its name.
     * <p>
     * The parent view is identified by its name, which is used to load the view via the engine.
     * </p>
     *
     * @param parent  the name of the parent view
     * @param context the evaluation context used during the parent setting operation
     */
    void setParent(String parent, EvaluationContext context);

    /**
     * Sets the parent view directly.
     *
     * @param parent  the parent {@link Template} to set
     * @param context the evaluation context used during the parent setting operation
     */
    void setParent(Template parent, EvaluationContext context);

    /**
     * Retrieves the registry that holds the definitions (macros and blocks) for this view.
     *
     * @return the {@link TemplateRegistry} associated with this view
     */
    TemplateRegistry getRegistry();

    /**
     * Cached parts of current view
     */
    Cache<Cache.Key, Content> getCache();
}
