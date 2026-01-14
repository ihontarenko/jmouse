package org.jmouse.crawler.dsl;

import org.jmouse.crawler.routing.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PipelineBuilder {

    private String id = "pipeline";
    private final List<StepsPipeline.NamedStep> steps = new ArrayList<>();

    public PipelineBuilder id(String id) {
        this.id = Objects.requireNonNull(id, "id");
        return this;
    }

    public PipelineBuilder step(String stepId, CrawlStep step) {
        steps.add(new StepsPipeline.NamedStep(stepId, step));
        return this;
    }

    public CrawlPipeline build() {
        return new StepsPipeline(id, steps);
    }
}
