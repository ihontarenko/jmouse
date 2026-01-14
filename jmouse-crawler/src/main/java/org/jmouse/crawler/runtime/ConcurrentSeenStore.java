package org.jmouse.crawler.runtime;

import org.jmouse.crawler.spi.SeenStore;
import org.jmouse.core.Verify;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ConcurrentSeenStore implements SeenStore {

    private final Set<String> seen;

    public ConcurrentSeenStore() {
        this.seen = ConcurrentHashMap.newKeySet();
    }

    @Override
    public boolean firstTime(URI url) {
        if (url == null) {
            return false;
        }
        return seen.add(normalize(url));
    }

    private static String normalize(URI uri) {
        // Minimal normalization. Extend later (strip fragments, normalize trailing slash, etc.)
        String value = uri.toString();
        Verify.nonNull(value, "uri");
        return value;
    }
}
