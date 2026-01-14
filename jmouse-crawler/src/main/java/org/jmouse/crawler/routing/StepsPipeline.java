package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.CrawlProcessingContext;

import java.util.List;

public final class StepsPipeline implements CrawlPipeline {

    private final String id;
    private final List<NamedStep> steps;

    public StepsPipeline(String id, List<NamedStep> steps) {
        this.id = id;
        this.steps = List.copyOf(steps);
    }

    @Override public String id() { return id; }

    @Override
    public PipelineResult execute(CrawlProcessingContext ctx) throws Exception {
        PipelineResult last = null;
        for (NamedStep step : steps) {
            last = step.step().execute(ctx);
            // ETAP B: без branching. Пізніше додамо onReturn/route-by-code.
            if (last != null && "STOP".equalsIgnoreCase(last.code())) {
                return last;
            }
        }
        return last != null ? last : PipelineResult.ok("pipeline");
    }

    public record NamedStep(String id, CrawlStep step) {}
}
