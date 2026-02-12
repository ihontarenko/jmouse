package org.jmouse.crawler.route;

import org.jmouse.core.MediaTypeHelper;
import org.jmouse.crawler.api.*;
import org.jmouse.crawler.pipeline.PipelineResult;
import org.jmouse.crawler.pipeline.PipelineStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

public record ImageDownloaderStep(String id) implements PipelineStep {

    private final static Logger LOGGER = LoggerFactory.getLogger(ImageDownloaderStep.class);

    @Override
    public PipelineResult execute(ProcessingContext context) throws Exception {
        RunContext     runContext     = context.run();
        ProcessingTask processingTask = context.processingTask();
        URI            targetURI      = processingTask.url();
        FetchRequest   request        = new FetchRequest(targetURI, Map.of());
        FetchResult    result         = runContext.fetcher().fetch(request);
        String         extension      = MediaTypeHelper.getExtensionFor(result.mediaType());

        System.out.println(extension);

        return PipelineResult.goon("image-downloader");
    }
}
