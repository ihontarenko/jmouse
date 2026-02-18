package org.jmouse.dom.blueprint;

import org.jmouse.core.Verify;
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

/**
 * Pipeline that resolves a blueprint by key, applies transformers, then materializes into a node tree.
 */
public final class RenderingPipeline {

    private final BlueprintCatalog          catalog;
    private final BlueprintTransformerChain transformerChain;
    private final BlueprintMaterializer     materializer;
    private final AccessorWrapper           accessorWrapper;
    private final RenderingHookChain        hookChain;

    private RenderingPipeline(Builder builder) {
        this.catalog = builder.catalog;
        this.transformerChain = builder.transformerChain;
        this.materializer = builder.materializer;
        this.accessorWrapper = builder.accessorWrapper;
        this.hookChain = new RenderingHookChain(builder.hooks);
    }

    public Node render(String blueprintKey, Object data) {
        return render(blueprintKey, data, request -> request);
    }

    public Node render(String blueprintKey, Object data, UnaryOperator<RenderingRequest> requestCustomizer) {
        Verify.nonNull(blueprintKey, "blueprintKey");
        Verify.nonNull(requestCustomizer, "requestCustomizer");

        RenderingRequest request = requestCustomizer.apply(new RenderingRequest());

        ObjectAccessor rootAccessor = accessorWrapper.wrap(data);
        RenderingExecution execution = new RenderingExecution(accessorWrapper, rootAccessor, request);

        try {
            hookChain.beforeBlueprintResolve(blueprintKey, data, request, execution);

            Blueprint blueprint = catalog.resolve(blueprintKey);

            hookChain.afterBlueprintResolve(blueprintKey, blueprint, execution);
            hookChain.beforeTransform(blueprint, execution);

            Blueprint transformed = transformerChain.apply(blueprint, execution);

            hookChain.afterTransform(transformed, execution);
            hookChain.beforeMaterialize(transformed, execution);

            Node node = materializer.materialize(transformed, execution);

            hookChain.afterMaterialize(node, execution);

            return node;
        } catch (RenderingShortCircuit shortCircuit) {
            return shortCircuit.result();
        } catch (Throwable exception) {
            // We try to notify hooks, but do not hide the original exception.
            try {
                hookChain.onFailure(exception, guessStage(exception), execution);
            } catch (Throwable ignored) {
                // ignored by design
            }
            throw exception;
        }
    }

    private RenderingStage guessStage(Throwable exception) {
        // Minimal: no stage tracking yet.
        // If you want exact stage tracking, add a variable in execution diagnostics like "stage".
        return RenderingStage.BEFORE_BLUEPRINT_RESOLVE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.catalog = this.catalog;
        builder.transformerChain = this.transformerChain;
        builder.materializer = this.materializer;
        builder.accessorWrapper = this.accessorWrapper;
        builder.hooks = new ArrayList<>(this.hookChain.hooks());
        return builder;
    }

    public static final class Builder {

        private BlueprintCatalog          catalog;
        private BlueprintTransformerChain transformerChain = new BlueprintTransformerChain();
        private BlueprintMaterializer     materializer;
        private AccessorWrapper           accessorWrapper;
        private List<RenderingHook>       hooks = new ArrayList<>();

        public Builder catalog(BlueprintCatalog catalog) {
            this.catalog = catalog;
            return this;
        }

        public BlueprintCatalog catalog() {
            return this.catalog;
        }

        public Builder addTransformer(int order, BlueprintTransformer transformer) {
            Verify.nonNull(transformer, "transformer");
            this.transformerChain.add(order, transformer);
            return this;
        }

        public Builder transformerChain(BlueprintTransformerChain chain) {
            this.transformerChain = Verify.nonNull(chain, "chain");
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

        public Builder addHook(RenderingHook hook) {
            Verify.nonNull(hook, "hook");
            this.hooks.add(hook);
            return this;
        }

        public RenderingPipeline build() {
            Verify.nonNull(accessorWrapper, "accessorWrapper");
            Verify.nonNull(catalog, "catalog");
            Verify.nonNull(materializer, "materializer");
            Verify.nonNull(transformerChain, "transformerChain");
            return new RenderingPipeline(this);
        }
    }
}
