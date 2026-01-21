package org.jmouse.crawler.api;

import org.jmouse.crawler.route.ProcessingRouteResolver;
import org.jmouse.crawler.runtime.core.TaskFactory;

import java.time.Clock;

public interface RunContext {

    Frontier frontier();

    RetryBuffer retryBuffer();

    DeadLetterQueue deadLetterQueue();

    DecisionLog decisionLog();

    DynamicAttributes attributes();

    ProcessingRouteResolver routes();

    Fetcher fetcher();

    ParserRegistry parsers();

    SeenStore seen();

    ScopePolicy scope();

    RetryPolicy retry();

    Clock clock();

    UtilityRegistry utilities();

    PolitenessPolicy politeness();

    TaskFactory tasks();

    InFlightBuffer inFlight();

}
