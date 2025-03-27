package org.jmouse.template;

import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.rendering.Block;
import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.Template;
import org.jmouse.el.rendering.TemplateStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandardTemplate implements Template {

    private final TokenizableSource  source;
    private final TemplateStack      stack;
    private final Map<String, Block> blocks = new HashMap<>();
    private final Engine             engine;

    public StandardTemplate(TokenizableSource source, Engine engine) {
        this.source = source;
        this.engine = engine;
        this.stack = TemplateStack.empty();
    }

    @Override
    public void evaluate(Content content, EvaluationContext context) {

    }

    @Override
    public EvaluationContext createContext() {
        EvaluationContext context = new DefaultEvaluationContext(engine.getExtensions());

        return context;
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
    public TemplateStack getStack() {
        return stack;
    }

    @Override
    public void setParent(String parent) {
        System.out.println("set parent " + parent);
    }

}
