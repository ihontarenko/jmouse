package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.RoutingHint;

import java.util.Set;

public final class UrlMatches {

    private UrlMatches() {}

    public static ProcessingRouteTask any() {
        return c -> true;
    }

    public static ProcessingRouteTask hints(RoutingHint... hints) {
        return c -> Set.of(hints).contains(c.hint());
    }

    public static ProcessingRouteTask host(String host) {
        return (c) -> c.task().url() != null && host.equalsIgnoreCase(c.task().url().getHost());
    }

    public static ProcessingRouteTask pathPrefix(String prefix) {
        return (c) -> c.url() != null && c.url().getPath() != null && c.url().getPath().startsWith(prefix);
    }

    public static ProcessingRouteTask allOf(ProcessingRouteTask... ms) {
        return (c) -> {
            for (ProcessingRouteTask t : ms) {
                if (!t.matches(c)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static ProcessingRouteTask anyOf(ProcessingRouteTask... ms) {
        return (c) -> {
            for (ProcessingRouteTask m : ms) {
                if (m.matches(c)) {
                    return true;
                }
            }
            return false;
        };
    }
}
