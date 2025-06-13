package org.jmouse.el.renderable;

import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.node.*;

/**
 * Defines a visitor for processing renderable nodes.
 * <p>
 * Provides default implementations for various node types so that
 * implementors can override only the methods they need.
 * </p>
 */
public interface NodeVisitor extends Visitor {

    /**
     * Visits a URLNode.
     *
     * @param urlNode the url node to process
     */
    default void visit(URLNode urlNode) {
    }

    /**
     * Visits a CacheNode.
     *
     * @param cacheNode the set node to process
     */
    default void visit(CacheNode cacheNode) {
    }

    /**
     * Visits a UseNode.
     *
     * @param useNode the set node to process
     */
    default void visit(UseNode useNode) {
    }

    /**
     * Visits a DoNode.
     *
     * @param doNode the set node to process
     */
    default void visit(DoNode doNode) {
    }

    /**
     * Visits a RenderNode.
     *
     * @param renderNode the set node to process
     */
    default void visit(RenderNode renderNode) {
    }

    /**
     * Visits a ParentNode.
     *
     * @param parentNode the set node to process
     */
    default void visit(ParentNode parentNode) {
    }

    /**
     * Visits a ScopeNode.
     *
     * @param scopeNode the set node to process
     */
    default void visit(ScopeNode scopeNode) {
    }

    /**
     * Visits a ApplyNode.
     *
     * @param applyNode the set node to process
     */
    default void visit(ApplyNode applyNode) {
    }

    /**
     * Visits a SetNode.
     *
     * @param setNode the set node to process
     */
    default void visit(SetNode setNode) {
    }

    /**
     * Visits a BlockNode.
     *
     * @param blockNode the block node to process
     */
    default void visit(BlockNode blockNode) {
    }

    /**
     * Visits a ContainerNode.
     *
     * @param containerNode the container node to process
     */
    default void visit(ContainerNode containerNode) {
    }

    /**
     * Visits an ExtendsNode.
     *
     * @param extendsNode the extends node to process
     */
    default void visit(ExtendsNode extendsNode) {
    }

    /**
     * Visits an EmbedNode.
     *
     * @param embedNode the extends node to process
     */
    default void visit(EmbedNode embedNode) {
    }

    /**
     * Visits an IfNode.
     *
     * @param ifNode the if node to process
     */
    default void visit(IfNode ifNode) {
    }

    /**
     * Visits an ForNode.
     *
     * @param forNode the for node to process
     */
    default void visit(ForNode forNode) {
    }

    /**
     * Visits an IncludeNode.
     *
     * @param includeNode the include node to process
     */
    default void visit(IncludeNode includeNode) {
    }

    /**
     * Visits a MacroNode.
     *
     * @param macroNode the macro node to process
     */
    default void visit(MacroNode macroNode) {
    }

    /**
     * Visits a PrintNode.
     *
     * @param printNode the print node to process
     */
    default void visit(PrintNode printNode) {
    }

    /**
     * Visits a TextNode.
     *
     * @param textNode the text node to process
     */
    default void visit(TextNode textNode) {
    }
}
