package org.jmouse.template;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.dom.Node;
import org.jmouse.template.hooks.RenderingHookChain;
import org.jmouse.template.hooks.RenderingShortCircuit;
import org.jmouse.template.hooks.RenderingStage;

import java.util.function.UnaryOperator;

import static org.jmouse.core.Verify.nonNull;

public final class RenderingPipeline implements Rendering<Node> {

    private final NodeTemplateResolver       resolver;
    private final TemplateMaterializer<Node> materializer;
    private final AccessorWrapper            accessorWrapper;
    private final RenderingHookChain<Node>   hookChain;

    private RenderingPipeline(PipelineBuilder<Node, RenderingPipeline> builder) {
        this.resolver = builder.templateResolver();
        this.materializer = builder.materializer();
        this.accessorWrapper = builder.accessorWrapper();
        this.hookChain = new RenderingHookChain<>(builder.hooks());
    }

    public Node render(String templateKey, Object data) {
        return render(templateKey, data, request -> request);
    }

    public Node render(String templateKey, Object data, UnaryOperator<RenderingRequest> requestCustomizer) {
        nonNull(templateKey, "blueprintKey");
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
            hookChain.beforeTemplateResolve(templateKey, data, request, execution);

            NodeTemplate blueprint = resolver.resolve(templateKey, execution);

            hookChain.afterTemplateResolve(templateKey, blueprint, execution);
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
        return RenderingStage.BEFORE_TEMPLATE_RESOLVE;
    }

    public static PipelineBuilder<Node, RenderingPipeline> builder() {
        PipelineBuilder<Node, RenderingPipeline> builder = new PipelineBuilder<>();
        builder.instanceFactory(RenderingPipeline::new);
        return builder;
    }


}

