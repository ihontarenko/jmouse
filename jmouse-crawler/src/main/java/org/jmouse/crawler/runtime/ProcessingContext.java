package org.jmouse.crawler.runtime;

import org.jmouse.crawler.spi.FetchResult;
import org.jmouse.crawler.spi.ParsedDocument;

import java.net.URI;

public interface ProcessingContext {

    ProcessingTask task();

    RunContext run();

    FetchResult fetchResult();

    void setFetchResult(FetchResult result);

    ParsedDocument document();

    void setDocument(ParsedDocument document);

    DecisionLog decisions();

    String routeId();

    void setRouteId(String routeId);

    void enqueue(URI url);

    void enqueue(URI url, RoutingHint hint);

    default <T> T utility(Class<T> type) {
        return run().utilities().get(type);
    }

}
