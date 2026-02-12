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

    ProcessingRouteResolver routeResolver();

    Fetcher fetcher();

    ParserRegistry parserRegistry();

    SeenStore seenStore();

    ScopePolicy scopePolicy();

    RetryPolicy retryPolicy();

    Clock clock();

    UtilityRegistry utilityRegistry();

    PolitenessPolicy politenessPolicy();

    TaskFactory taskFactory();

    InFlightBuffer inFlight();

}
