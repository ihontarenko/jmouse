package org.jmouse.meterializer;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.meterializer.hooks.RenderingHookChain;
import org.jmouse.meterializer.hooks.RenderingStage;

import java.util.function.UnaryOperator;

import static org.jmouse.core.Verify.nonNull;

abstract public class AbstractRenderingPipeline<T, R extends AbstractRenderingPipeline<T, R>> implements Rendering<T> {

    private final NodeTemplateResolver    resolver;
    private final TemplateMaterializer<T> materializer;
    private final AccessorWrapper         accessorWrapper;
    private final RenderingHookChain<T>   hookChain;

    protected AbstractRenderingPipeline(PipelineBuilder<T, R> builder) {
        this.resolver = builder.templateResolver();
        this.materializer = builder.materializer();
        this.accessorWrapper = builder.accessorWrapper();
        this.hookChain = new RenderingHookChain<>(builder.hooks());
    }

    public T render(String templateKey, Object data) {
        return render(templateKey, data, request -> request);
    }

    public T render(String templateKey, Object data, UnaryOperator<RenderingRequest> requestCustomizer) {
        nonNull(templateKey, "blueprintKey");
        nonNull(requestCustomizer, "requestCustomizer");

        RenderingRequest request      = requestCustomizer.apply(new RenderingRequest());
        ObjectAccessor   rootAccessor = accessorWrapper.wrap(data);

        RenderingExecution execution = new RenderingExecution(
                accessorWrapper,
                rootAccessor,
                request,
                resolver
        );

        try {
            hookChain.beforeTemplateResolve(templateKey, data, request, execution);

            NodeTemplate blueprint = resolver.resolve(templateKey, execution);

            hookChain.afterTemplateResolve(templateKey, blueprint, execution);
            hookChain.beforeMaterialize(blueprint, execution);

            T node = materializer.materialize(blueprint, execution);

            hookChain.afterMaterialize(node, execution);

            return node;
        } catch (Throwable exception) {
            try {
                hookChain.onFailure(exception, guessStage(exception), execution);
            } catch (Throwable ignored) {}
            throw exception;
        }
    }

    private RenderingStage guessStage(Throwable exception) {
        return RenderingStage.BEFORE_TEMPLATE_RESOLVE;
    }

}
