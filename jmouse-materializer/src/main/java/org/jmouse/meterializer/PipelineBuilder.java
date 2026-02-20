package org.jmouse.meterializer;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.meterializer.hooks.RenderingHook;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.jmouse.core.Verify.nonNull;

public final class PipelineBuilder<T, R extends Rendering<T>> {

    private final List<RenderingHook<T>>             hooks            = new ArrayList<>();
    private       TemplateRegistry                   templateRegistry;
    private       TransformerChain                   transformerChain = new TransformerChain();
    private       NodeTemplateResolver               resolver;
    private       TemplateMaterializer<T>            materializer;
    private       AccessorWrapper                    accessorWrapper;
    private       Function<PipelineBuilder<T, R>, R> instanceFactory;

    public PipelineBuilder<T, R> templateRegistry(TemplateRegistry catalog) {
        this.templateRegistry = catalog;
        return this;
    }

    public TemplateRegistry templateRegistry() {
        return templateRegistry;
    }

    public PipelineBuilder<T, R> transformer(int order, TemplateTransformer transformer) {
        nonNull(transformer, "transformer");
        this.transformerChain.add(order, transformer);
        return this;
    }

    public PipelineBuilder<T, R> transformerChain(TransformerChain chain) {
        this.transformerChain = nonNull(chain, "chain");
        return this;
    }

    public TransformerChain transformerChain() {
        return transformerChain;
    }

    public PipelineBuilder<T, R> templateResolver(NodeTemplateResolver resolver) {
        this.resolver = nonNull(resolver, "resolver");
        return this;
    }

    public NodeTemplateResolver templateResolver() {
        return resolver;
    }

    public PipelineBuilder<T, R> materializer(TemplateMaterializer<T> materializer) {
        this.materializer = nonNull(materializer, "materializer");
        return this;
    }

    public TemplateMaterializer<T> materializer() {
        return materializer;
    }

    public PipelineBuilder<T, R> accessorWrapper(AccessorWrapper accessorWrapper) {
        this.accessorWrapper = nonNull(accessorWrapper, "accessorWrapper");
        return this;
    }

    public AccessorWrapper accessorWrapper() {
        return accessorWrapper;
    }

    public PipelineBuilder<T, R> hook(RenderingHook<T> hook) {
        nonNull(hook, "hook");
        this.hooks.add(hook);
        return this;
    }

    public List<RenderingHook<T>> hooks() {
        return hooks;
    }

    public PipelineBuilder<T, R> instanceFactory(Function<PipelineBuilder<T, R>, R> instanceFactory) {
        this.instanceFactory = nonNull(instanceFactory, "instanceFactory");
        return this;
    }

    public R build() {
        nonNull(accessorWrapper, "accessorWrapper");
        nonNull(templateRegistry, "catalog");
        nonNull(materializer, "materializer");
        nonNull(transformerChain, "transformerChain");
        nonNull(instanceFactory, "instanceFactory");

        if (resolver == null) {
            resolver = NodeTemplateResolver.of(templateRegistry, transformerChain);
        }

        return instanceFactory.apply(this);
    }
}
