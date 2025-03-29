package org.jmouse.template;

import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.rendering.*;
import org.jmouse.template.node.BlockNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandardTemplate implements Template {

    private final TokenizableSource  source;
    private final Map<String, Block> blocks = new HashMap<>();
    private final Map<String, Macro> macros = new HashMap<>();
    private final Engine             engine;
    private final RenderableNode     root;

    public StandardTemplate(RenderableNode root, TokenizableSource source, Engine engine) {
        this.source = source;
        this.engine = engine;
        this.root = root;
    }

    @Override
    public void renderBlock(String name, Content content, EvaluationContext context) {
        root.execute(new BlockLinker(this, context));
        root.execute(new MacroLinker(this, context));

        if (getBlock(name, context).getBlockNode() instanceof BlockNode node) {
            node.getBody().render(content, this, context);
        }
    }

    @Override
    public void renderBlock(String name, Content content) {
        renderBlock(name, content, createContext());
    }

    @Override
    public Content renderBlock(String name) {
        Content content = Content.array();
        renderBlock(name, content);
        return content;
    }

    @Override
    public void render(Content content, EvaluationContext context) {
        root.execute(new BlockLinker(this, context));
        root.execute(new MacroLinker(this, context));

        root.render(content, this, context);

        TemplateStack stack  = context.getInheritance();
        Template      parent = stack.getParent();

        if (parent != null) {
            stack.ascend();
            parent.render(content, context);
            stack.descend();
        }
    }

    @Override
    public Content render(Map<String, Object> values) {
        Content content = Content.array();
        render(content, values);
        return content;
    }

    @Override
    public Content render(EvaluationContext context) {
        Content content = Content.array();
        render(content, context);
        return content;
    }

    @Override
    public void render(Content content, Map<String, Object> values) {
        EvaluationContext context = createContext();
        values.forEach(context::setValue);
        render(content, context);
    }

    @Override
    public EvaluationContext createContext() {
        EvaluationContext context = new DefaultEvaluationContext(engine.getExtensions());
        context.getInheritance().inherit(this);
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
    public void setMacro(Macro macro) {
        macros.put(macro.getName(), macro);
    }

    @Override
    public Macro getMacro(String name) {
        return macros.get(name);
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
    public Block getBlock(String name, EvaluationContext context) {
        Block         block       = getBlock(name);
        TemplateStack inheritance = context.getInheritance();
        Template      child       = inheritance.getChild();

        if (child != null) {
            inheritance.descend();
            Block overridden = child.getBlock(name, context);
            if (overridden != null) {
                block = overridden;
            }
            inheritance.ascend();
        }

        return block;
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

    @Override
    public String toString() {
        return "TEMPLATE: " + getName();
    }
}
