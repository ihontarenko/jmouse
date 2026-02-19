package org.jmouse.dom.template;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.dom.Node;
import org.jmouse.dom.template.hooks.RenderingHook;
import org.jmouse.dom.template.hooks.RenderingHookChain;
import org.jmouse.dom.template.hooks.RenderingShortCircuit;
import org.jmouse.dom.template.hooks.RenderingStage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.jmouse.core.Verify.nonNull;

public final class RenderingPipeline {

    private final TemplateRegistry     catalog;
    private final NodeTemplateResolver resolver;
    private final TemplateMaterializer materializer;
    private final AccessorWrapper      accessorWrapper;
    private final RenderingHookChain    hookChain;

    private RenderingPipeline(Builder builder) {
        this.catalog = builder.registry;
        this.resolver = builder.resolver;
        this.materializer = builder.materializer;
        this.accessorWrapper = builder.accessorWrapper;
        this.hookChain = new RenderingHookChain(builder.hooks);
    }

    public Node render(String blueprintKey, Object data) {
        return render(blueprintKey, data, request -> request);
    }

    public Node render(String blueprintKey, Object data, UnaryOperator<RenderingRequest> requestCustomizer) {
        nonNull(blueprintKey, "blueprintKey");
        nonNull(requestCustomizer, "requestCustomizer");

        RenderingRequest request = requestCustomizer.apply(new RenderingRequest());
        ObjectAccessor   rootAccessor = accessorWrapper.wrap(data);

        RenderingExecution execution = new RenderingExecution(
                accessorWrapper,
                rootAccessor,
                request,
                resolver
        );

        try {
            hookChain.beforeBlueprintResolve(blueprintKey, data, request, execution);

            NodeTemplate blueprint = resolver.resolve(blueprintKey, execution);

            hookChain.afterBlueprintResolve(blueprintKey, blueprint, execution);
            hookChain.beforeMaterialize(blueprint, execution);

            Node node = materializer.materialize(blueprint, execution);

            hookChain.afterMaterialize(node, execution);

            return node;
        } catch (RenderingShortCircuit shortCircuit) {
            return shortCircuit.result();
        } catch (Throwable exception) {
            try {
                hookChain.onFailure(exception, guessStage(exception), execution);
            } catch (Throwable ignored) {}
            throw exception;
        }
    }

    private RenderingStage guessStage(Throwable exception) {
        return RenderingStage.BEFORE_BLUEPRINT_RESOLVE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<RenderingHook>   hooks            = new ArrayList<>();
        private       TemplateRegistry      registry;
        private TransformerChain     transformerChain = new TransformerChain();
        private NodeTemplateResolver resolver;
        private TemplateMaterializer materializer;
        private AccessorWrapper      accessorWrapper;

        public Builder catalog(TemplateRegistry catalog) {
            this.registry = catalog;
            return this;
        }

        public TemplateRegistry catalog() {
            return this.registry;
        }

        public Builder transformer(int order, TemplateTransformer transformer) {
            nonNull(transformer, "transformer");
            this.transformerChain.add(order, transformer);
            return this;
        }

        public Builder transformerChain(TransformerChain chain) {
            this.transformerChain = nonNull(chain, "chain");
            return this;
        }

        public Builder blueprintResolver(NodeTemplateResolver resolver) {
            this.resolver = nonNull(resolver, "resolver");
            return this;
        }

        public Builder materializer(TemplateMaterializer materializer) {
            this.materializer = materializer;
            return this;
        }

        public Builder accessorWrapper(AccessorWrapper accessorWrapper) {
            this.accessorWrapper = accessorWrapper;
            return this;
        }

        public Builder hook(RenderingHook hook) {
            nonNull(hook, "hook");
            this.hooks.add(hook);
            return this;
        }

        public RenderingPipeline build() {
            nonNull(accessorWrapper, "accessorWrapper");
            nonNull(registry, "catalog");
            nonNull(materializer, "materializer");
            nonNull(transformerChain, "transformerChain");

            if (resolver == null) {
                resolver = NodeTemplateResolver.of(registry, transformerChain);
            }

            return new RenderingPipeline(this);
        }
    }
}

