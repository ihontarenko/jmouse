package org.jmouse.crawler.examples.smoke.smoke2;

import org.jmouse.crawler.selector.CssSelector;
import org.jmouse.crawler.pipeline.PipelineStep;
import org.jmouse.crawler.pipeline.PipelineResult;
import org.jmouse.crawler.api.ProcessingContext;

import java.util.List;

public final class VoronProductProcessor implements PipelineStep {

    private final String titleCss;

    public VoronProductProcessor(String titleCss) {
        this.titleCss = titleCss;
    }

    @Override
    public PipelineResult execute(ProcessingContext context) {

        CssSelector cssSelector = context.utility(CssSelector.class);

        List<String> results = cssSelector.texts(context.document(), titleCss);

        if (results.isEmpty()) {
            context.decisions().reject("NOT_FOUND", "title not found");
            return PipelineResult.stop("title not found");
        }

        String title = results.getFirst();

        System.out.println("PRODUCT: " + title + " | " + context.task().url());
        context.decisions().accept("TITLE", title);

        return PipelineResult.goon("product");
    }
}
