package org.jmouse.crawler.smoke.smoke2;

import org.jmouse.crawler.routing.CrawlStep;
import org.jmouse.crawler.routing.PipelineResult;
import org.jmouse.crawler.runtime.CrawlProcessingContext;

public final class VoronProductProcessor implements CrawlStep {

    private final String titleCss;

    public VoronProductProcessor(String titleCss) {
        this.titleCss = titleCss;
    }

    @Override
    public PipelineResult execute(CrawlProcessingContext context) {

        String title = "";
        if (title == null || title.isBlank()) {
            context.decisions().reject("NOT_FOUND", "title not found");
            return new PipelineResult("OK", "product");
        }

        System.out.println("PRODUCT: " + title + " | " + context.task().url());
        context.decisions().accept("TITLE", title);

        return PipelineResult.ok("product");
    }
}
