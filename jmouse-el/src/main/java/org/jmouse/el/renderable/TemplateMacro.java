package org.jmouse.el.renderable;

import org.jmouse.el.renderable.node.MacroNode;

/**
 * Represents a template macro, which binds a macro name to its corresponding macro definition.
 * <p>
 * This record implements the {@link Macro} interface and encapsulates an immutable mapping
 * between a macro name and a {@link MacroNode} that contains the macro's content.
 * </p>
 *
 * @param name the name of the macro
 * @param node the {@link MacroNode} representing the macro's definition and body
 */
public record TemplateMacro(String name, MacroNode node, String source) implements Macro {

}
