package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.CrawlRunContext;
import org.jmouse.crawler.runtime.CrawlTask;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FirstMatchRouteRegistry implements CrawlRouteRegistry {

    private final List<CrawlRoute>        routes;
    private final Map<String, CrawlRoute> byId;

    public FirstMatchRouteRegistry(List<CrawlRoute> routes) {
        this.routes = List.copyOf(routes);

        Map<String, CrawlRoute> index = new LinkedHashMap<>();

        for (CrawlRoute route : this.routes) {
            index.put(route.id(), route);
        }

        this.byId = Collections.unmodifiableMap(index);
    }

    @Override
    public CrawlRoute resolve(CrawlTask task, CrawlRunContext runContext) {
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
                if (route.matches(task, runContext)) {
                    matched = route;
                    break;
                }
            }
        }

        return matched;
    }

    @Override
    public CrawlRoute byId(String routeId) {
        return byId.get(routeId);
    }
}

