package org.jmouse.el.renderable;

import org.jmouse.el.node.Node;

/**
 * Represents a view block.
 * <p>
 * This record binds a block name to its corresponding content node.
 * It provides an immutable representation of a block defined within a view,
 * where the {@code name} identifies the block and the {@code node} contains its content.
 * </p>
 *
 * @param name the name of the block
 * @param node the {@link Node} representing the content of the block
 */
public record TemplateBlock(String name, Node node, String source) implements Block {

}
