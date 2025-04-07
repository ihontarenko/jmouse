package org.jmouse.el.renderable;

import org.jmouse.el.core.node.Node;

public class TemplateBlock implements Block {

    private final String name;
    private final Node   block;

    public TemplateBlock(String name, Node block) {
        this.name = name;
        this.block = block;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Node getBlockNode() {
        return block;
    }

}
