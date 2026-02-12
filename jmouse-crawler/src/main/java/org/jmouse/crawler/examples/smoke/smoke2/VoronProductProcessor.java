package org.jmouse.crawler.examples.smoke.smoke2;

import org.jmouse.crawler.api.RunContext;
import org.jmouse.crawler.route.ImageDownloaderHint;
import org.jmouse.crawler.selector.CssSelector;
import org.jmouse.crawler.pipeline.PipelineStep;
import org.jmouse.crawler.pipeline.PipelineResult;
import org.jmouse.crawler.api.ProcessingContext;

import java.net.URI;
import java.util.List;

public final class VoronProductProcessor implements PipelineStep {

    private final String titleCss;

    public VoronProductProcessor(String titleCss) {
        this.titleCss = titleCss;
    }

    @Override
    public PipelineResult execute(ProcessingContext context) {
        RunContext  runContext  = context.run();
        CssSelector cssSelector = context.utility(CssSelector.class);

        List<String> results = cssSelector.texts(context.document(), titleCss);
        List<String> attributes = cssSelector.attributes(context.document(), "#big_picture", "src");

        if (results.isEmpty()) {
            context.decisionLog().reject("NOT_FOUND", "title not found");
            return PipelineResult.stop("title not found");
        }

        if (!attributes.isEmpty()) {
            String image    = attributes.getFirst();
            URI    site     = runContext.attributes().getAttribute("site");
            URI    imageURI = site.resolve(image);
            context.enqueue(imageURI, ImageDownloaderHint.IMAGE_DOWNLOADER);
            return PipelineResult.goon("image-downloader");
        }

        String title = results.getFirst();

        System.out.println("PRODUCT: " + title + " | " + context.processingTask().url());
        context.decisionLog().accept("TITLE", title);

        return PipelineResult.goon("product");
    }
}
