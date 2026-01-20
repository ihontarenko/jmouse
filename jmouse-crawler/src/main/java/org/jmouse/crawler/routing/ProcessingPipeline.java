package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.ProcessingContext;

/**
 * Processing pipeline that orchestrates a sequence of {@link PipelineStep}s for a single
 * {@link ProcessingContext}. 🧭
 *
 * <p>A pipeline is the core routing/processing unit of the crawler. It receives a context
 * (task + fetched data + shared services) and produces a {@link PipelineResult} describing
 * what should happen next (continue, stop, reroute, etc.).</p>
 *
 * <p>Implementations should be stateless or thread-safe unless documented otherwise.</p>
 */
public interface ProcessingPipeline {

    /**
     * Stable pipeline identifier used for logging, diagnostics, and routing decisions.
     *
     * @return non-blank pipeline id
     */
    String id();

    /**
     * Execute the pipeline for the given processing context.
     *
     * @param ctx processing context
     * @return pipeline result describing the next action
     * @throws Exception if a step fails and the error is not handled inside the pipeline
     */
    PipelineResult execute(ProcessingContext ctx) throws Exception;
}
