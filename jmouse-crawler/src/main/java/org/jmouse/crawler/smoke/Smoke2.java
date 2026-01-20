package org.jmouse.crawler.smoke;

import org.jmouse.crawler.dsl.CrawlerBuilder;
import org.jmouse.crawler.dsl.Runners;
import org.jmouse.crawler.content.*;
import org.jmouse.crawler.http.HttpClientFetcher;
import org.jmouse.crawler.http.HttpFetcherConfig;
import org.jmouse.crawler.routing.*;
import org.jmouse.crawler.runtime.*;
import org.jmouse.crawler.smoke.smoke2.VoronHint;
import org.jmouse.crawler.smoke.smoke2.VoronListingProcessor;
import org.jmouse.crawler.smoke.smoke2.VoronProductProcessor;
import org.jmouse.crawler.spi.Fetcher;
import org.jmouse.crawler.spi.ParserRegistry;
import org.jmouse.crawler.spi.PolitenessPolicies;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.jmouse.crawler.routing.UrlMatches.*;

public class Smoke2 {

    public static void main(String[] args) {
        ParserRegistry parserRegistry = new SimpleParserRegistry(
                List.of(new JsoupHtmlParser())
        );

        Fetcher fetcher = new HttpClientFetcher(
                HttpFetcherConfig.builder()
                        .userAgent("jMouse-Crawler/1.0 (+https://example.com/bot)")
                        .build()
        );

        DecisionLog decisionLog = new InMemoryDecisionLog();

        Crawler crawler = CrawlerBuilder.create()

                .seed("https://voron.ua/uk/catalog/active-components/integrated-circuit/logic-gates", VoronHint.LISTING)
                .runtime(runtime -> runtime
                        .fetcher(fetcher)
                        .parsers(parserRegistry)
                        .decisionLog(decisionLog)
                        .politeness(PolitenessPolicies.gentle(80, 100))
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
                                    .step("fetch-parse", new FetchParsePipeline("fetch-parse")::execute)
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
                                    .step("fetch-parse", new FetchParsePipeline("fetch-parse")::execute)
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
