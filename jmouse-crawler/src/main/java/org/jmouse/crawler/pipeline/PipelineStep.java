package org.jmouse.crawler.pipeline;

import org.jmouse.crawler.api.ProcessingContext;

/**
 * Single processing step within a {@link ProcessingPipeline}. 🧩
 *
 * <p>A {@code PipelineStep} represents one unit of work in a pipeline.
 * Steps are executed sequentially by a pipeline implementation
 * (for example, {@link StepsPipeline}).</p>
 *
 * <p>Steps may:</p>
 * <ul>
 *   <li>inspect or mutate the {@link ProcessingContext}</li>
 *   <li>produce a {@link PipelineResult} to influence pipeline control flow</li>
 *   <li>throw an exception to signal an unrecoverable error</li>
 * </ul>
 *
 * <p>Returning {@code null} is permitted and is interpreted by the pipeline
 * as a {@link PipelineResult.Kind#GOON} (continue execution).</p>
 *
 * <p>This interface is a {@link FunctionalInterface}, allowing steps to be
 * implemented as lambdas or method references.</p>
 */
@FunctionalInterface
public interface PipelineStep {

    /**
     * Execute this step using the provided processing context.
     *
     * @param context processing context for the current task
     * @return pipeline result controlling the next pipeline action,
     *         or {@code null} to indicate "continue"
     * @throws Exception if the step fails and the error should abort the pipeline
     */
    PipelineResult execute(ProcessingContext context) throws Exception;
}
