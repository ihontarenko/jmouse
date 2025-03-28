package org.jmouse.template;

import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.rendering.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandardTemplate implements Template {

    private final TokenizableSource  source;
    private final TemplateStack      stack;
    private final Map<String, Block> blocks = new HashMap<>();
    private final Engine             engine;
    private final RenderableNode     root;

    public StandardTemplate(RenderableNode root, TokenizableSource source, Engine engine) {
        this.source = source;
        this.engine = engine;
        this.root = root;
        this.stack = TemplateStack.empty();
    }

    @Override
    public void evaluate(Content content, Map<String, Object> values) {
        EvaluationContext context = createContext();
        values.forEach(context::setValue);
        evaluate(content, context);
    }

    @Override
    public void evaluate(Content content, EvaluationContext context) {
        root.execute(new BlockLinker(this, context));
        root.render(content, this, context);

        TemplateStack stack  = context.getInheritance();
        Template      parent = stack.getParent();

        if (parent != null) {
            stack.ascend();
            parent.evaluate(content, context);
        }
    }

    @Override
    public Content evaluate(EvaluationContext context) {
        Content content = Content.array();
        evaluate(content, context);
        return content;
    }

    @Override
    public EvaluationContext createContext() {
        return new DefaultEvaluationContext(engine.getExtensions());
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
    public void setParent(String parent, EvaluationContext context) {
        setParent(engine.getTemplate(parent), context);
    }

    @Override
    public void setParent(Template parent, EvaluationContext context) {
        context.getInheritance().inherit(parent);
    }

}
