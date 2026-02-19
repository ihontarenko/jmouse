package org.jmouse.dom.blueprint;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.dom.Node;
import org.jmouse.dom.blueprint.hooks.RenderingHook;
import org.jmouse.dom.blueprint.hooks.RenderingHookChain;
import org.jmouse.dom.blueprint.hooks.RenderingShortCircuit;
import org.jmouse.dom.blueprint.hooks.RenderingStage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.jmouse.core.Verify.nonNull;

public final class RenderingPipeline {

    private final BlueprintCatalog      catalog;
    private final BlueprintResolver     resolver;
    private final BlueprintMaterializer materializer;
    private final AccessorWrapper       accessorWrapper;
    private final RenderingHookChain    hookChain;

    private RenderingPipeline(Builder builder) {
        this.catalog = builder.catalog;
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

            Blueprint blueprint = resolver.resolve(blueprintKey, execution);

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

        private final List<RenderingHook>       hooks            = new ArrayList<>();
        private       BlueprintCatalog          catalog;
        private       BlueprintTransformerChain transformerChain = new BlueprintTransformerChain();
        private       BlueprintResolver         resolver;
        private       BlueprintMaterializer     materializer;
        private       AccessorWrapper           accessorWrapper;

        public Builder catalog(BlueprintCatalog catalog) {
            this.catalog = catalog;
            return this;
        }

        public BlueprintCatalog catalog() {
            return this.catalog;
        }

        public Builder transformer(int order, BlueprintTransformer transformer) {
            nonNull(transformer, "transformer");
            this.transformerChain.add(order, transformer);
            return this;
        }

        public Builder transformerChain(BlueprintTransformerChain chain) {
            this.transformerChain = nonNull(chain, "chain");
            return this;
        }

        public Builder blueprintResolver(BlueprintResolver resolver) {
            this.resolver = nonNull(resolver, "resolver");
            return this;
        }

        public Builder materializer(BlueprintMaterializer materializer) {
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
            nonNull(catalog, "catalog");
            nonNull(materializer, "materializer");
            nonNull(transformerChain, "transformerChain");

            if (resolver == null) {
                resolver = BlueprintResolver.of(catalog, transformerChain);
            }

            return new RenderingPipeline(this);
        }
    }
}

