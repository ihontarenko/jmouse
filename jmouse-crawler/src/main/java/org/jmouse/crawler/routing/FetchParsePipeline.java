package org.jmouse.crawler.routing;

import org.jmouse.core.reflection.Reflections;
import org.jmouse.crawler.runtime.ProcessingContext;
import org.jmouse.crawler.runtime.RunContext;
import org.jmouse.crawler.spi.FetchRequest;
import org.jmouse.crawler.spi.FetchResult;
import org.jmouse.crawler.spi.ParsedDocument;
import org.jmouse.crawler.spi.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Pipeline step that fetches the current task URL and optionally parses the response. 🌐📄
 *
 * <p>This step performs the "fetch + parse" portion of the crawler lifecycle:</p>
 * <ul>
 *   <li>builds a {@link FetchRequest} from the current {@link org.jmouse.crawler.runtime.ProcessingTask}</li>
 *   <li>invokes the configured fetcher</li>
 *   <li>stores the {@link FetchResult} into the {@link ProcessingContext}</li>
 *   <li>resolves a {@link Parser} by response content type and, if found, parses the document</li>
 * </ul>
 *
 * <p>Parsing is optional: if no parser matches the content type, only the fetch result is stored.</p>
 *
 * <p>Note: this step currently uses an empty header map ({@code Map.of()}).
 * If you plan to support custom headers (Accept, cookies, auth), consider injecting
 * a header strategy into the {@link RunContext} or passing headers via the task/context.</p>
 *
 * @param id step identifier (used for diagnostics when embedded into a larger pipeline)
 */
public record FetchParsePipeline(String id) implements PipelineStep {

    private final static Logger LOGGER = LoggerFactory.getLogger(FetchParsePipeline.class);

    /**
     * Execute the fetch+parse step.
     *
     * @param context processing context
     * @return {@link PipelineResult.Kind#GOON} to continue the pipeline
     * @throws Exception if fetching or parsing fails
     */
    @Override
    public PipelineResult execute(ProcessingContext context) throws Exception {
        RunContext run = context.run();

        // Fetch the resource for the current task.
        FetchResult fetched = run.fetcher()
                .fetch(new FetchRequest(context.task().url(), Map.of()));

        LOGGER.debug("Fetched: {}", fetched);

        // Make the fetch result available to downstream steps.
        context.setFetchResult(fetched);

        // Resolve and run a content-type-aware parser (optional).
        Parser parser = run.parsers().resolve(fetched.contentType());
        if (parser != null) {
            ParsedDocument parsed = parser.parse(fetched);
            LOGGER.debug("Parsed: {}", Reflections.getUserClass(parsed));
            context.setDocument(parsed);
        }

        // Stage id is stable and useful for decision logs / debugging.
        return PipelineResult.goon("fetch-parse");
    }
}
