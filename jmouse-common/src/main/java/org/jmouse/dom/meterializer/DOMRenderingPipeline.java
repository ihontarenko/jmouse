package org.jmouse.dom.meterializer;

import org.jmouse.dom.Node;
import org.jmouse.meterializer.AbstractRenderingPipeline;
import org.jmouse.meterializer.PipelineBuilder;

public final class DOMRenderingPipeline extends AbstractRenderingPipeline<Node, DOMRenderingPipeline> {

    public DOMRenderingPipeline(PipelineBuilder<Node, DOMRenderingPipeline> builder) {
        super(builder);
    }

    public static PipelineBuilder<Node, DOMRenderingPipeline> builder() {
        PipelineBuilder<Node, DOMRenderingPipeline> builder = new PipelineBuilder<>();
        builder.instanceFactory(DOMRenderingPipeline::new);
        return builder;
    }

}

