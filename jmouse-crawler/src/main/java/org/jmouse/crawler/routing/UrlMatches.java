package org.jmouse.crawler.routing;

import java.util.Locale;

public final class UrlMatches {

    private UrlMatches() {}

    public static ProcessingRouteTask any() {
        return c -> true;
    }

    public static ProcessingRouteTask host(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        return (c) -> c.task().url() != null && h.equalsIgnoreCase(c.task().url().getHost());
    }

    public static ProcessingRouteTask pathPrefix(String prefix) {
        return (t, r) -> t.url() != null && t.url().getPath() != null && t.url().getPath().startsWith(prefix);
    }

    public static ProcessingRouteTask all(ProcessingRouteTask... ms) {
        return (t, r) -> {
            for (ProcessingRouteTask m : ms) if (!m.test(t, r)) return false;
            return true;
        };
    }

    public static ProcessingRouteTask anyOf(ProcessingRouteTask... ms) {
        return (t, r) -> {
            for (ProcessingRouteTask m : ms) if (m.test(t, r)) return true;
            return false;
        };
    }
}
