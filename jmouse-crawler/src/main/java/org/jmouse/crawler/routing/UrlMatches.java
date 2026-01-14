package org.jmouse.crawler.routing;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.runtime.CrawlRunContext;

import java.util.Locale;

public final class UrlMatches {

    private UrlMatches() {}

    public static UrlMatch any() {
        return (t, r) -> true;
    }

    public static UrlMatch host(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        return (t, r) -> t.url() != null && h.equalsIgnoreCase(t.url().getHost());
    }

    public static UrlMatch pathPrefix(String prefix) {
        return (t, r) -> t.url() != null && t.url().getPath() != null && t.url().getPath().startsWith(prefix);
    }

    public static UrlMatch all(UrlMatch... ms) {
        return (t, r) -> {
            for (UrlMatch m : ms) if (!m.test(t, r)) return false;
            return true;
        };
    }

    public static UrlMatch anyOf(UrlMatch... ms) {
        return (t, r) -> {
            for (UrlMatch m : ms) if (m.test(t, r)) return true;
            return false;
        };
    }
}
