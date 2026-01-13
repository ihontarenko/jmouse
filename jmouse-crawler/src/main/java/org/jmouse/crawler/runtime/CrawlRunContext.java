package org.jmouse.crawler.runtime;

import org.jmouse.crawler.routing.CrawlRouteResolver;
import org.jmouse.crawler.spi.Fetcher;
import org.jmouse.crawler.spi.ParserRegistry;
import org.jmouse.crawler.spi.RetryPolicy;
import org.jmouse.crawler.spi.ScopePolicy;
import org.jmouse.crawler.spi.SeenStore;

import java.time.Clock;

public interface CrawlRunContext {
    Frontier frontier();
    RetryBuffer retryBuffer();
    DeadLetterQueue deadLetterQueue();

    CrawlRouteResolver routes();

    Fetcher fetcher();
    ParserRegistry parsers();

    SeenStore seen();
    ScopePolicy scope();
    RetryPolicy retry();

    Clock clock();
}
