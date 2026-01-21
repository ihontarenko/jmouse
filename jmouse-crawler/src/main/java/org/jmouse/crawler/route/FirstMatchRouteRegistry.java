package org.jmouse.crawler.route;

import org.jmouse.crawler.api.RunContext;
import org.jmouse.crawler.api.ProcessingTask;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FirstMatchRouteRegistry implements ProcessingRouteRegistry {

    private final List<ProcessingRoute>        routes;
    private final Map<String, ProcessingRoute> byId;

    public FirstMatchRouteRegistry(List<ProcessingRoute> routes) {
        this.routes = List.copyOf(routes);

        Map<String, ProcessingRoute> index = new LinkedHashMap<>();

        for (ProcessingRoute route : this.routes) {
            index.put(route.id(), route);
        }

        this.byId = Collections.unmodifiableMap(index);
    }

    @Override
    public ProcessingRoute resolve(ProcessingTask task, RunContext runContext) {
        ProcessingRoute matched = null;

        for (ProcessingRoute route : routes) {
            if (route.matches(task, runContext)) {
                matched = route;
                break;
            }
        }

        return matched;
    }

    @Override
    public ProcessingRoute byId(String routeId) {
        return byId.get(routeId);
    }
}

