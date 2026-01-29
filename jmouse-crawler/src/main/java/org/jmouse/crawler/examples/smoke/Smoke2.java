package org.jmouse.crawler.examples.smoke;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.crawler.adapter.jsonpath.JaywayJsonPathSelector;
import org.jmouse.crawler.adapter.jsoup.*;
import org.jmouse.crawler.api.*;
import org.jmouse.crawler.dsl.builder.FacadeBuilder;
import org.jmouse.crawler.dsl.factory.Runners;
import org.jmouse.crawler.runtime.queue.FifoFrontier;
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
import java.util.List;
import java.util.Map;

import static org.jmouse.crawler.route.URLMatches.*;

public class Smoke2 {

    public static Mapper mapper() {
        TypeMappingRegistry registry = TypeMappingRegistry.builder()
                .mapping(Map.class, ProcessingTask.class, m -> m
                    .compute("origin", (source, context) -> {
                        return TaskOrigin.retry("default-reason");
                    })
                    .compute("hint", (source, context)
                            -> VoronHint.valueOf(String.valueOf(source.get("hint")).toUpperCase().trim()))
                )
                .build();

        return Mappers.builder()
                .registry(registry)
                .build();
    }

    public static void main(String[] args) {

        Path stateDir = Path.of("build", "crawler-state", "smoke2");


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

        Crawler crawler = FacadeBuilder.create()

                .seed("https://voron.ua/uk/catalog/active-components/integrated-circuit/logic-gates", VoronHint.LISTING)
                .runtime(runtime -> runtime
                        .fetcher(fetcher)
                        .parsers(parserRegistry)
                        .decisionLog(decisionLog)

                        .frontier(new FifoFrontier())

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
