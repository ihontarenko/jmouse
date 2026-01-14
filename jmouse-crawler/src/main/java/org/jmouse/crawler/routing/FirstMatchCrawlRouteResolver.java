package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.CrawlRouteResolver;
import org.jmouse.crawler.runtime.CrawlTask;
import org.jmouse.crawler.runtime.CrawlRunContext;

import java.util.List;

public final class FirstMatchCrawlRouteResolver implements CrawlRouteResolver {

    private final List<CrawlRoute> routes;

    public FirstMatchCrawlRouteResolver(List<CrawlRoute> routes) {
        this.routes = List.copyOf(routes);
    }

    @Override
    public CrawlRoute resolve(CrawlTask task, CrawlRunContext run) {
        CrawlRoute matched = null;

        if (task.hint() != null) {
            for (CrawlRoute route : routes) {
                if (route.supportsHint(task.hint())) {
                    matched = route;
                    break;
                }
            }
        }

        if (matched == null) {
            for (CrawlRoute route : routes) {
                if (route.matches(task, run)) {
                    matched = route;
                    break;
                }
            }
        }

        return matched;
    }
}
