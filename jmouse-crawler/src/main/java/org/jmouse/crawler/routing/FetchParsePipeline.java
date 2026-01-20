package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.ProcessingContext;
import org.jmouse.crawler.runtime.RunContext;
import org.jmouse.crawler.spi.FetchRequest;
import org.jmouse.crawler.spi.FetchResult;
import org.jmouse.crawler.spi.Parser;

import java.util.Map;

public record FetchParsePipeline(String id) implements CrawlPipeline {

    @Override
    public PipelineResult execute(ProcessingContext context) throws Exception {
        RunContext  run     = context.run();
        FetchResult fetched = run.fetcher().fetch(new FetchRequest(context.task().url(), Map.of()));
        Parser      parser  = run.parsers().resolve(fetched.contentType());

        context.setFetchResult(fetched);

        if (parser != null) {
            context.setDocument(parser.parse(fetched));
        }

        return PipelineResult.goon("fetch-parse");
    }

}
