package org.jmouse.crawler.routing;

import org.jmouse.core.Verify;
import org.jmouse.crawler.runtime.DecisionCodes;
import org.jmouse.crawler.runtime.DecisionLog;
import org.jmouse.crawler.runtime.ProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ProcessingPipeline} implementation that executes a fixed, ordered list of steps. 🧩
 *
 * <p>Each {@link PipelineStep} is invoked in order. For every step, the pipeline:</p>
 * <ul>
 *   <li>logs step start and completion into {@link DecisionLog}</li>
 *   <li>normalizes {@code null} step results into {@link PipelineResult#goon(String)}</li>
 *   <li>short-circuits on terminal results such as {@link PipelineResult.Stop} or {@link PipelineResult.Route}</li>
 *   <li>logs step failures and rethrows the exception</li>
 * </ul>
 *
 * <p>Step identifiers must be unique within a pipeline to keep decision logs unambiguous.</p>
 */
public final class StepsPipeline implements ProcessingPipeline {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProcessingPipeline.class);

    private final String           id;
    private final List<StepHolder> steps;

    /**
     * @param id    pipeline id (non-blank)
     * @param steps ordered list of steps (non-null, step ids unique)
     */
    public StepsPipeline(String id, List<StepHolder> steps) {
        this.id = Verify.nonNull(id, "id");
        Verify.state(!id.isBlank(), "id must be non-blank");
        this.steps = List.copyOf(Verify.nonNull(steps, "steps"));
        validateSteps(this.steps);
    }

    @Override
    public String id() {
        return id;
    }

    /**
     * Execute all configured steps in order until completion or an explicit terminal result.
     *
     * <p>If a step returns {@code null}, it is treated as {@link PipelineResult.Kind#GOON}
     * using the step id as the stage id.</p>
     *
     * <p>If no steps are configured, or no step produces a non-null result, the pipeline returns
     * {@code goon("pipeline")}.</p>
     */
    @Override
    public PipelineResult execute(ProcessingContext context) throws Exception {
        Verify.nonNull(context, "context");

        DecisionLog decisions = context.decisions();
        PipelineResult lastResult = null;

        for (StepHolder stepHolder : steps) {
            String stepId = stepHolder.id();
            PipelineStep step = stepHolder.step();

            decisions.accept(
                    DecisionCodes.PIPELINE_STEP_START,
                    "%s:%s".formatted(id, stepId)
            );

            try {
                PipelineResult result = step.execute(context);

                // Normalize null result to "continue".
                if (result == null) {
                    result = PipelineResult.goon(stepId);
                }

                lastResult = result;

                String accepted = "%s:%s -> %s (%s)".formatted(id, stepId, result.kind(), result.stageId());

                decisions.accept(
                        DecisionCodes.PIPELINE_STEP_OK,
                        accepted
                );

                LOGGER.debug("[{}]: {}", DecisionCodes.PIPELINE_STEP_OK, accepted);

                // Terminal results short-circuit the pipeline.
                if (result instanceof PipelineResult.Stop || result instanceof PipelineResult.Route) {
                    String formatted = "%s:%s -> %s".formatted(id, stepId, result.kind());
                    decisions.accept(
                            DecisionCodes.PIPELINE_STOP,
                            formatted
                    );
                    LOGGER.debug("[{}]: {}", DecisionCodes.PIPELINE_STOP, formatted);
                    return result;
                }

            } catch (Exception exception) {
                String failed = "%s:%s -> %s: %s".formatted(
                        id,
                        stepId,
                        exception.getClass().getSimpleName(),
                        safeMessage(exception)
                );
                decisions.reject(
                        DecisionCodes.PIPELINE_STEP_ERROR,
                        failed
                );
                LOGGER.warn("[{}]: {}", DecisionCodes.PIPELINE_STEP_ERROR, failed);
                throw exception;
            }
        }

        return (lastResult != null) ? lastResult : PipelineResult.goon("pipeline");
    }

    /**
     * Validate step holder list:
     * <ul>
     *   <li>no null holders</li>
     *   <li>non-blank ids</li>
     *   <li>non-null steps</li>
     *   <li>unique ids</li>
     * </ul>
     */
    private static void validateSteps(List<StepHolder> steps) {
        Set<String> stepIds = new HashSet<>();

        for (StepHolder stepHolder : steps) {
            Verify.nonNull(stepHolder, "stepHolder");

            String stepId = Verify.nonNull(stepHolder.id(), "stepHolder.id");
            Verify.state(!stepId.isBlank(), "stepHolder.id must be non-blank");

            Verify.nonNull(stepHolder.step(), "stepHolder.step");

            Verify.state(stepIds.add(stepId), "duplicate step id: " + stepId);
        }
    }

    /**
     * Render a safe, compact error message for logging.
     */
    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return (message != null && !message.isBlank()) ? message : "(no message)";
    }

    /**
     * Step descriptor for {@link StepsPipeline}.
     *
     * <p>The {@code id} must be unique within a pipeline.</p>
     *
     * @param id   step id (non-blank)
     * @param step step implementation
     */
    public record StepHolder(String id, PipelineStep step) {

        public StepHolder {
            Verify.nonNull(id, "id");
            Verify.state(!id.isBlank(), "id must be non-blank");
            Verify.nonNull(step, "step");
        }
    }
}
