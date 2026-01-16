package org.jmouse.crawler.routing;

import org.jmouse.core.Verify;
import org.jmouse.crawler.runtime.CrawlProcessingContext;
import org.jmouse.crawler.runtime.DecisionCodes;
import org.jmouse.crawler.runtime.DecisionLog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StepsPipeline implements CrawlPipeline {

    private final String           id;
    private final List<StepHolder> steps;

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

    @Override
    public PipelineResult execute(CrawlProcessingContext context) throws Exception {
        Verify.nonNull(context, "context");

        DecisionLog    decisions  = context.decisions();
        PipelineResult lastResult = null;

        for (StepHolder stepHolder : steps) {
            String    stepId = stepHolder.id();
            CrawlStep step   = stepHolder.step();

            decisions.accept(
                    DecisionCodes.PIPELINE_STEP_START,
                    "%s:%s".formatted(id, stepId)
            );

            try {
                PipelineResult result = step.execute(context);
                if (result == null) {
                    result = PipelineResult.goon(stepId);
                }

                lastResult = result;

                decisions.accept(
                        DecisionCodes.PIPELINE_STEP_OK,
                        "%s:%s -> %s (%s)".formatted(id, stepId, result.kind(), result.stageId())
                );

                if (result instanceof PipelineResult.Stop || result instanceof PipelineResult.Route) {
                    decisions.accept(
                            DecisionCodes.PIPELINE_STOP,
                            "%s:%s -> %s".formatted(id, stepId, result.kind())
                    );
                    return result;
                }

            } catch (Exception exception) {
                decisions.reject(
                        DecisionCodes.PIPELINE_STEP_ERROR,
                        "%s:%s -> %s: %s".formatted(
                                id,
                                stepId,
                                exception.getClass().getSimpleName(),
                                safeMessage(exception)
                        )
                );
                throw exception;
            }
        }

        return (lastResult != null) ? lastResult : PipelineResult.goon("pipeline");
    }


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

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return (message != null && !message.isBlank()) ? message : "(no message)";
    }

    public record StepHolder(String id, CrawlStep step) {
        public StepHolder {
            Verify.nonNull(id, "id");
            Verify.state(!id.isBlank(), "id must be non-blank");
            Verify.nonNull(step, "step");
        }
    }
}
