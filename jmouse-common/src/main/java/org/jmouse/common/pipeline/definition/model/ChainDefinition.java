package org.jmouse.common.pipeline.definition.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record ChainDefinition(
        String name,
        String initial,
        Map<String, LinkDefinition> links
) {
    public ChainDefinition {
        links = (links != null ? new LinkedHashMap<>(links) : new LinkedHashMap<>());
    }

    public LinkDefinition link(String name) {
        return links.get(name);
    }
}