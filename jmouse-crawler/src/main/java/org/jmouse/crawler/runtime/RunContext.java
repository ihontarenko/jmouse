package org.jmouse.crawler.runtime;

import org.jmouse.crawler.routing.ProcessingRouteResolver;
import org.jmouse.crawler.spi.*;

import java.time.Clock;

public interface RunContext {

    Frontier frontier();

    RetryBuffer retryBuffer();

    DeadLetterQueue deadLetterQueue();

    DecisionLog decisionLog();

    ProcessingRouteResolver routes();

    Fetcher fetcher();

    ParserRegistry parsers();

    SeenStore seen();

    ScopePolicy scope();

    RetryPolicy retry();

    Clock clock();

    UtilityRegistry utilities();

    PolitenessPolicy politeness();

}
