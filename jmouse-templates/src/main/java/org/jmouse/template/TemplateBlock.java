package org.jmouse.template;

import org.jmouse.el.rendering.Block;
import org.jmouse.el.rendering.RenderableNode;

public class TemplateBlock implements Block {

    private final String         name;
    private final RenderableNode block;

    public TemplateBlock(String name, RenderableNode block) {
        this.name = name;
        this.block = block;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RenderableNode getBlockNode() {
        return block;
    }

}
