package org.jmouse.crawler.runtime.state;

import org.jmouse.crawler.api.SeenStore;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ConcurrentSeenStore implements SeenStore {

    private final Set<String> discovered = ConcurrentHashMap.newKeySet();
    private final Set<String> processed  = ConcurrentHashMap.newKeySet();

    private static String normalize(URI uri) {
        return uri.toString();
    }

    @Override
    public boolean markDiscovered(URI url) {
        return discovered.add(normalize(url));
    }

    @Override
    public boolean markProcessed(URI url) {
        return processed.add(normalize(url));
    }

    @Override
    public boolean isDiscovered(URI url) {
        return discovered.contains(normalize(url));
    }

    @Override
    public boolean isProcessed(URI url) {
        return processed.contains(normalize(url));
    }

}

