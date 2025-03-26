package org.jmouse.template;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.rendering.Block;
import org.jmouse.el.rendering.EntityStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandardTemplate implements Template {

    private final TokenizableSource  source;
    private final EntityStack        stack;
    private final Map<String, Block> blocks = new HashMap<>();

    public StandardTemplate(TokenizableSource source) {
        this.source = source;
        this.stack = EntityStack.empty();
    }

    @Override
    public TokenizableSource getSource() {
        return source;
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public void setBlock(Block block) {
        blocks.put(block.getName(), block);
    }

    @Override
    public Block getBlock(String name) {
        return blocks.get(name);
    }

    @Override
    public List<Block> getBlocks() {
        return new ArrayList<>(blocks.values());
    }

    @Override
    public EntityStack getStack() {
        return stack;
    }

    @Override
    public void setParent(String parent) {

    }

}
