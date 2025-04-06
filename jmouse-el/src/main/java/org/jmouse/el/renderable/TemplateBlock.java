package org.jmouse.el.renderable;

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
