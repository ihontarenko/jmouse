package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.Node;

public class StandardTemplate implements Template {

    private final TokenizableSource source;
    private final TemplateRegistry  registry;
    private final Engine            engine;
    private final Node    root;

    public StandardTemplate(Node root, TokenizableSource source, Engine engine) {
        this.source = source;
        this.engine = engine;
        this.root = root;
        this.registry = new TemplateRegistry(engine);
    }

    @Override
    public EvaluationContext newContext() {
        EvaluationContext context = new DefaultEvaluationContext(engine.getExtensions());
        context.getInheritance().inherit(this);
        return context;
    }

    @Override
    public TokenizableSource getSource() {
        return source;
    }

    @Override
    public Node getRoot() {
        return root;
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public void setMacro(Macro macro) {
        registry.registerMacro(macro.name(), macro);
    }

    @Override
    public Macro getMacro(String name) {
        return registry.getMacro(name);
    }

    @Override
    public void setBlock(Block block) {
        registry.registerBlock(block.name(), block);
    }

    @Override
    public Block getBlock(String name) {
        return registry.getBlock(name);
    }

    @Override
    public Block getBlock(String name, EvaluationContext context) {
        Block       block       = getBlock(name);
        Inheritance inheritance = context.getInheritance();
        Template    child       = inheritance.getChild();

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
    public Template getParent(EvaluationContext context) {
        return context.getInheritance().getParent();
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
    public TemplateRegistry getRegistry() {
        return registry;
    }

    @Override
    public String toString() {
        return "TEMPLATE: " + getName();
    }
}
