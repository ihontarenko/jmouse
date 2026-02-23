package org.jmouse.dom.meterializer;

import org.jmouse.dom.Node;
import org.jmouse.meterializer.AbstractRenderingPipeline;
import org.jmouse.meterializer.PipelineBuilder;

/**
 * Rendering pipeline specialization for DOM output. 🌳
 *
 * <p>
 * {@code DOMRenderingPipeline} binds the generic
 * {@link AbstractRenderingPipeline} to {@link Node} as the rendering result type.
 * It orchestrates:
 * </p>
 *
 * <ul>
 *     <li>Template resolution</li>
 *     <li>Hook execution</li>
 *     <li>Materialization into DOM {@link Node} trees</li>
 * </ul>
 *
 * <p>
 * Typical flow:
 * </p>
 *
 * <pre>{@code
 * templateKey → resolve → before hooks → materialize → after hooks → Node
 * }</pre>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * DOMRenderingPipeline pipeline = DOMRenderingPipeline.builder()
 *     .templateResolver(resolver)
 *     .materializer(materializer)
 *     .accessorWrapper(wrapper)
 *     .addHook(new SubmissionDecorationHook())
 *     .build();
 *
 * Node root = pipeline.render("form", model);
 * }</pre>
 *
 * <p>
 * The returned result is a fully materialized DOM tree ready for further
 * processing or rendering via {@code RenderingProcessor}.
 * </p>
 */
public final class DOMRenderingPipeline extends AbstractRenderingPipeline<Node, DOMRenderingPipeline> {

    /**
     * Creates a DOM rendering pipeline using the provided builder configuration.
     *
     * @param builder pipeline builder
     */
    public DOMRenderingPipeline(PipelineBuilder<Node, DOMRenderingPipeline> builder) {
        super(builder);
    }

    /**
     * Creates a preconfigured {@link PipelineBuilder} for DOM pipelines.
     *
     * <p>
     * The builder is initialized with an instance factory that produces
     * {@link DOMRenderingPipeline} instances.
     * </p>
     *
     * @return pipeline builder
     */
    public static PipelineBuilder<Node, DOMRenderingPipeline> builder() {
        PipelineBuilder<Node, DOMRenderingPipeline> builder = new PipelineBuilder<>();
        builder.instanceFactory(DOMRenderingPipeline::new);
        return builder;
    }

}