package org.jmouse.crawler.smoke.smoke2;

import org.jmouse.crawler.content.CssSelector;
import org.jmouse.crawler.routing.PipelineStep;
import org.jmouse.crawler.routing.PipelineResult;
import org.jmouse.crawler.runtime.ProcessingContext;

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
