package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.Node;

public class DefaultTemplate implements Template {

    private final TokenizableSource         source;
    private final TemplateRegistry          registry;
    private final Engine                    engine;
    private final Node                      root;
    private final Cache<Cache.Key, Content> cache;
    private       boolean                   initialized = false;

    public DefaultTemplate(Node root, TokenizableSource source, Engine engine) {
        this.source = source;
        this.engine = engine;
        this.root = root;
        this.registry = new TemplateRegistry(engine);
        this.cache = new Cache.Memory<>();
    }

    @Override
    public void setInitialized() {
        initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
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
    public Cache<Cache.Key, Content> getCache() {
        return cache;
    }

    @Override
    public String toString() {
        return "TEMPLATE: " + getName();
    }
}
