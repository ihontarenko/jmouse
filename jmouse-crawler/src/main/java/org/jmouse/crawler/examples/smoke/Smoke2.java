package org.jmouse.crawler.examples.smoke;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.errors.ErrorAction;
import org.jmouse.core.mapping.errors.ErrorsPolicy;
import org.jmouse.core.mapping.errors.ErrorCodes;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.trace.TraceContext;
import org.jmouse.crawler.adapter.jsonpath.JaywayJsonPathSelector;
import org.jmouse.crawler.adapter.jsoup.*;
import org.jmouse.crawler.api.*;
import org.jmouse.crawler.dsl.builder.FacadeBuilder;
import org.jmouse.crawler.dsl.factory.Runners;
import org.jmouse.crawler.runtime.queue.FifoFrontier;
import org.jmouse.crawler.runtime.state.checkpoint.FileSnapshotStore;
import org.jmouse.crawler.runtime.state.checkpoint.JacksonSnapshotCodec;
import org.jmouse.crawler.runtime.state.checkpoint.SnapshotCodec;
import org.jmouse.crawler.runtime.state.checkpoint.StoreBackedSnapshotRepository;
import org.jmouse.crawler.runtime.state.checkpoint.frontier.CheckpointingFrontier;
import org.jmouse.crawler.runtime.state.checkpoint.frontier.FrontierSnapshot;
import org.jmouse.crawler.selector.*;
import org.jmouse.crawler.adapter.http.HttpClientFetcher;
import org.jmouse.crawler.adapter.http.HttpFetcherConfig;
import org.jmouse.crawler.runtime.state.InMemoryDecisionLog;
import org.jmouse.crawler.route.*;
import org.jmouse.crawler.runtime.*;
import org.jmouse.crawler.examples.smoke.smoke2.VoronHint;
import org.jmouse.crawler.examples.smoke.smoke2.VoronListingProcessor;
import org.jmouse.crawler.examples.smoke.smoke2.VoronProductProcessor;
import org.jmouse.crawler.runtime.PolitenessPolicies;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jmouse.crawler.route.URLMatches.*;

public class Smoke2 {

    public static Mapper mapper() {
//        TypeMappingRegistry registry = TypeMappingRegistry.builder()
//                .mapping(Map.class, ProcessingTask.class, m -> m
//                        .compute("origin", (source, context) -> {
//                            if (source.get("origin") instanceof Map<?,?> origin) {
//                                String kind = String.valueOf(origin.get("kind"));
//
//                                if (kind.equals("seed")) {
//                                    return TaskOrigin.seed(
//                                            String.valueOf(origin.get("publisher"))
//                                    );
//                                }
//                            }
//
//                            return TaskOrigin.retry("default!");
//                        })
//                        .compute("hint", (source, context) -> VoronHint.valueOf(String.valueOf(source.get("hint")).toUpperCase().trim()))
//                        .reference("id", "task_id")
//                )
//                .mapping(ProcessingTask.class, Map.class, m -> m
//                        .reference("task_id", "id")
//                )
//                .build();

        return Mappers.builder()
//                .registry(registry)
                .config(MappingConfig.builder()
                                .errorsPolicy(
                                        ErrorsPolicy.builder()
                                                .onCode(ErrorCodes.STRATEGY_NO_CONTRIBUTOR, ErrorAction.THROW)
                                                .onPrefix("map.", ErrorAction.WARNING)
                                                .onPrefix("scalar.", ErrorAction.THROW)
                                                .defaultAction(ErrorAction.THROW)
                                                .build()
                                )
                                .build())
                .build();
    }

    public static class DataObject {

        private String url;
        private String parent;
        private int    depth;

        public DataObject(String parent) {
            this.parent = parent;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }
    }

    public static void main(String[] args) {

        ProcessingTask processingTask = new ProcessingTask(
                TaskId.random(),
                TraceContext.root(),
                URI.create("https://google.com/"),
                100,
                URI.create("https://jmouse.org/"),
                TaskOrigin.seed("pub-processor"),
                0,
                Instant.now(),
                2, VoronHint.PRODUCT
        );

        Map<String, Object> target = new ConcurrentHashMap<>();

        mapper().map(processingTask, InferredType.forParametrizedClass(
                Map.class, String.class, Object.class
        ), target);

        DataObject dataObject = new DataObject("http://site.com/");

        mapper().map(target, dataObject);

        System.out.println(target);

        System.exit(1);

        var           objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        SnapshotCodec codec        = new JacksonSnapshotCodec(objectMapper);

        var store = new FileSnapshotStore(Path.of("var/state/frontier.snapshot.json"));
        var repo  = new StoreBackedSnapshotRepository<>(
                store,
                codec,
                FrontierSnapshot.class,
                FrontierSnapshot.empty()
        );

        Frontier frontier = new FifoFrontier();

        frontier = new CheckpointingFrontier(
                frontier,
                repo,
                mapper(), // твій jMouse Mapper
                () -> true, // або policy
                true
        );


        /// //////////////////////

        ParserRegistry parserRegistry = new SimpleParserRegistry(
                List.of(new JsoupHtmlParser())
        );

        Fetcher fetcher = new HttpClientFetcher(
                HttpFetcherConfig.builder()
                        .userAgent("jMouse-Crawler/1.0 (+https://example.com/bot)")
                        .build()
        );

        DecisionLog decisionLog = new InMemoryDecisionLog();

        Frontier finalFrontier = frontier;

        Crawler crawler = FacadeBuilder.create()

                .seed("https://voron.ua/uk/catalog/active-components/integrated-circuit/logic-gates", VoronHint.LISTING)
                .runtime(runtime -> runtime
                        .fetcher(fetcher)
                        .parsers(parserRegistry)
                        .decisionLog(decisionLog)

                        .frontier(finalFrontier)

                        .politeness(PolitenessPolicies.gentle(80, 100))
                        .politeness(p -> p
                                .keyResolver(new DefaultPolitenessKeyResolver())
                                .lane("html", Duration.ofMillis(200))
                                .lane("page", Duration.ofMillis(500))
                                .lane("media", Duration.ofMillis(1200))
                        )
                        .retry(RetryPolicies.simple(2, Duration.ofMillis(100)))
                        .dynamicAttributes(DynamicAttributes.withInitial(
                                Map.of("site", URI.create("https://voron.ua/"))
                        ))
                )

                .routes(routes -> {

                    routes.route("VORON_LISTING")
                            .match(allOf(
                                    hints(VoronHint.LISTING, VoronHint.PAGINATION),
                                    host("voron.ua"),
                                    pathPrefix("/uk/catalog/")
                            ))
                            .pipeline(pipeline -> pipeline
                                    .id("voron-listing")
                                    .step("fetch-parse", new FetchParsePipeline("fetch-parse"))
                                    .step("listing", new VoronListingProcessor(
                                            "a[href*='/uk/product/']",
                                            "a[rel='next'], .pagination a.next"
                                    ))
                            )
                            .register();

                    routes.route("VORON_PRODUCT")
                            .match(anyOf(
                                    hints(VoronHint.PRODUCT),
                                    host("voron.ua"),
                                    pathPrefix("/uk/product/")
                            ))
                            .pipeline(pipeline -> pipeline
                                    .id("voron-product")
                                    .step("fetch-parse", new FetchParsePipeline("fetch-parse"))
                                    .step("product", new VoronProductProcessor("body > table > tbody > tr:nth-child(2) > td > table > tbody > tr > td > table > tbody > tr:nth-child(2) > td.center_col > div.product-block > h1"))
                            )
                            .register();
                })

//                .runner(Runners.executor(8, 64))
                .runner(Runners.singleThread())

                .utilities(u -> u
                        .register(XPathSelector.class, new JsoupXPathSelector())
                        .register(CssSelector.class, new JsoupCssSelector())
                        .register(JsonPathSelector.class, new JaywayJsonPathSelector())
                )

                .build();

        crawler.runUntilDrained();

        System.out.println(decisionLog);

    }

}
