package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.spi.FetchResult;
import org.jmouse.crawler.spi.ParsedDocument;

public interface CrawlProcessingContext {

    CrawlTask task();
    CrawlRunContext run();

    FetchResult fetchResult();
    void setFetchResult(FetchResult result);

    ParsedDocument document();
    void setDocument(ParsedDocument document);

    DecisionLog decisions();

    String routeId();
    void setRouteId(String routeId);
}
