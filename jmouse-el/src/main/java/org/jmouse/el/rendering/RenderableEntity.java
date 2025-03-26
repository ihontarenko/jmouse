package org.jmouse.el.rendering;

import org.jmouse.el.lexer.TokenizableSource;

import java.util.List;

public interface RenderableEntity {

    TokenizableSource getSource();

    String getName();

    void setBlock(Block block);

    Block getBlock(String name);

    List<Block> getBlocks();

    EntityStack getStack();

    void setParent(String parent);

    default void setParent(RenderableEntity parent) {
        getStack().inherit(parent);
    }

    default RenderableEntity getParent() {
        return getStack().getParent();
    }

    default boolean hasParent() {
        return getParent() != null;
    }

}
