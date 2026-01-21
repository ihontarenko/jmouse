package org.jmouse.crawler.examples.smoke.smoke2;

import org.jmouse.crawler.selector.CssSelector;
import org.jmouse.crawler.pipeline.PipelineStep;
import org.jmouse.crawler.pipeline.PipelineResult;
import org.jmouse.crawler.api.ProcessingContext;
import org.jmouse.crawler.api.RunContext;

import java.net.URI;

public final class VoronListingProcessor implements PipelineStep {

    // selectors — конфігні, підправиш під DOM
    private final String productLinkCss;
    private final String nextPageCss;

    public VoronListingProcessor(String productLinkCss, String nextPageCss) {
        this.productLinkCss = productLinkCss;
        this.nextPageCss = nextPageCss;
    }

    @Override
    public PipelineResult execute(ProcessingContext ctx) {
        RunContext runContext = ctx.run();

        URI base = ctx.fetchResult() != null && ctx.fetchResult().finalUrl() != null
                ? ctx.fetchResult().finalUrl()
                : ctx.task().url();

        CssSelector cssSelector = ctx.utility(CssSelector.class);
//        cssSelector.links(ctx.document(), "a[href^='/uk/catalog/']");

//        // 1) product pages
        for (URI u : cssSelector.links(ctx.document(), "a[href^='/uk/catalog/']")) {
            URI site = runContext.attributes().getAttribute("site");
            ctx.enqueue(site.resolve(u), VoronHint.PRODUCT);
        }
//
//        // 2) pagination
//        URI next = css.firstLink(ctx.document(), nextPageCss, "href", base);
//        if (next != null) {
//            ctx.enqueue(next, VoronHint.PAGINATION); // або LISTING — як тобі зручніше
//        }

        return PipelineResult.goon("listing");
    }
}

