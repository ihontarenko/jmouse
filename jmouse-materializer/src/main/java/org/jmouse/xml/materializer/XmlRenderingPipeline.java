package org.jmouse.xml.materializer;

import org.jmouse.meterializer.AbstractRenderingPipeline;
import org.jmouse.meterializer.PipelineBuilder;
import org.w3c.dom.Node;

public class XmlRenderingPipeline extends AbstractRenderingPipeline<Node, XmlRenderingPipeline> {

    public XmlRenderingPipeline(PipelineBuilder<Node, XmlRenderingPipeline> builder) {
        super(builder);
    }

    public static PipelineBuilder<Node, XmlRenderingPipeline> builder() {
        PipelineBuilder<Node, XmlRenderingPipeline> builder = new PipelineBuilder<>();
        builder.instanceFactory(XmlRenderingPipeline::new);
        return builder;
    }

}
