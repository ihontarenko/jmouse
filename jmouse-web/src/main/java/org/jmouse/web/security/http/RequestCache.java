package org.jmouse.web.security.http;

import org.jmouse.security.Envelope;

import java.util.Map;
import java.util.Optional;

public interface RequestCache {

    void save(Envelope request);                 // на CHALLENGE

    Optional<SavedRequest> get(Envelope request);

    void clear(Envelope request);

    interface SavedRequest {

        String path();

        String method();

        Map<String, String> parameters();

    }

}