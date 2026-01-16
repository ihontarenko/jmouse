package org.jmouse.crawler.smoke.smoke2;

import org.jmouse.crawler.routing.PipelineStep;
import org.jmouse.crawler.routing.PipelineResult;
import org.jmouse.crawler.runtime.ProcessingContext;

public final class VoronProductProcessor implements PipelineStep {

    private final String titleCss;

    public VoronProductProcessor(String titleCss) {
        this.titleCss = titleCss;
    }

    @Override
    public PipelineResult execute(ProcessingContext context) {

        String title = "";
        if (title == null || title.isBlank()) {
            context.decisions().reject("NOT_FOUND", "title not found");
            return PipelineResult.stop("title not found");
        }

        System.out.println("PRODUCT: " + title + " | " + context.task().url());
        context.decisions().accept("TITLE", title);

        return PipelineResult.goon("product");
    }
}
