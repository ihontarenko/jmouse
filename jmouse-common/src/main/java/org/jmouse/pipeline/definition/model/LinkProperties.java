package org.jmouse.pipeline.definition.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record LinkProperties(
        Map<String, String> transitions,
        Map<String, String> configuration,
        String fallback
) {
    public LinkProperties {
        transitions = (transitions != null ? new LinkedHashMap<>(transitions) : new LinkedHashMap<>());
        configuration = (configuration != null ? new LinkedHashMap<>(configuration) : new LinkedHashMap<>());
    }

    public static LinkProperties empty() {
        return new LinkProperties(Map.of(), Map.of(), null);
    }
}