package org.jmouse.crawler.dsl.builder;

import org.jmouse.crawler.pipeline.PipelineStep;
import org.jmouse.crawler.pipeline.ProcessingPipeline;
import org.jmouse.crawler.pipeline.StepsPipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PipelineBuilder {

    private String                               id    = "pipeline";
    private final List<StepsPipeline.StepHolder> steps = new ArrayList<>();

    public PipelineBuilder id(String id) {
        this.id = Objects.requireNonNull(id, "id");
        return this;
    }

    public PipelineBuilder step(String stepId, PipelineStep step) {
        steps.add(new StepsPipeline.StepHolder(stepId, step));
        return this;
    }

    public ProcessingPipeline build() {
        return new StepsPipeline(id, steps);
    }
}
