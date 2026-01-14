package org.jmouse.crawler.runtime;

import org.jmouse.crawler.spi.*;

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

    UtilityRegistry utilities();

    PolitenessPolicy politeness();

}
