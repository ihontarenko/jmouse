package org.jmouse.crawler.smoke;

import org.jmouse.crawler.dsl.CrawlerBuilder;
import org.jmouse.crawler.dsl.Runners;
import org.jmouse.crawler.html.JsoupHtmlParser;
import org.jmouse.crawler.html.SimpleParserRegistry;
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

import java.util.List;

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

        ParserRegistry parsers = new SimpleParserRegistry(
                List.of(new JsoupHtmlParser())
        );

        Crawler crawler = CrawlerBuilder.create()

                .seed("https://voron.ua/uk/catalog/active-components/integrated-circuit/logic-gates", VoronHint.LISTING)

                .runtime(runtime -> runtime
                        .fetcher(fetcher)
                        .parsers(parserRegistry)
                        .politeness(PolitenessPolicies.gentle(800, 2.0))
                )

                .routes(routes -> {

                    routes.route("VORON_LISTING")
                            .hints(VoronHint.LISTING, VoronHint.PAGINATION)
                            .match(UrlMatches.all(
                                    UrlMatches.host("voron.ua"),
                                    UrlMatches.pathPrefix("/uk/catalog/")
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
                            .hints(VoronHint.PRODUCT)
                            .match(UrlMatches.all(
                                    UrlMatches.host("voron.ua"),
                                    UrlMatches.pathPrefix("/uk/product/")
                            ))
                            .pipeline(pipeline -> pipeline
                                    .id("voron-product")
                                    .step("fetch-parse", new FetchParsePipeline("fetch-parse")::execute)
                                    .step("product", new VoronProductProcessor("h1"))
                            )
                            .register();
                })

                .runner(Runners.executor(8, 64))
                .build();

        crawler.runUntilDrained();
    }

}
