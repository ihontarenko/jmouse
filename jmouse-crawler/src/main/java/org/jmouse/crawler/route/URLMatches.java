package org.jmouse.crawler.route;

import org.jmouse.crawler.api.RoutingHint;

import java.util.Set;

public final class URLMatches {

    private URLMatches() {}

    public static ProcessingTaskMatcher any() {
        return c -> true;
    }

    public static ProcessingTaskMatcher hints(RoutingHint... hints) {
        return c -> Set.of(hints).contains(c.hint());
    }

    public static ProcessingTaskMatcher host(String host) {
        return (c) -> c.task().url() != null && host.equalsIgnoreCase(c.task().url().getHost());
    }

    public static ProcessingTaskMatcher pathPrefix(String prefix) {
        return (c) -> c.url() != null && c.url().getPath() != null && c.url().getPath().startsWith(prefix);
    }

    public static ProcessingTaskMatcher allOf(ProcessingTaskMatcher... ms) {
        return (c) -> {
            for (ProcessingTaskMatcher t : ms) {
                if (!t.matches(c)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static ProcessingTaskMatcher anyOf(ProcessingTaskMatcher... ms) {
        return (c) -> {
            for (ProcessingTaskMatcher m : ms) {
                if (m.matches(c)) {
                    return true;
                }
            }
            return false;
        };
    }
}
