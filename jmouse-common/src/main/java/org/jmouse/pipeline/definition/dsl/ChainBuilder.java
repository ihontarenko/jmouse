package org.jmouse.pipeline.definition.dsl;

import org.jmouse.pipeline.definition.model.ChainDefinition;
import org.jmouse.pipeline.definition.model.LinkDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ChainBuilder {

    private       String                   initial;
    private final String                   name;
    private final Map<String, LinkBuilder> links = new LinkedHashMap<>();

    ChainBuilder(String name) {
        this.name = name;
    }

    public ChainBuilder initial(String linkName) {
        this.initial = linkName;
        return this;
    }

    public LinkBuilder link(String name) {
        return links.computeIfAbsent(name, LinkBuilder::new);
    }

    ChainDefinition build() {
        Map<String, LinkDefinition> built = new LinkedHashMap<>();
        for (LinkBuilder linkBuilder : links.values()) {
            LinkDefinition linkDefinition = linkBuilder.build();
            built.put(linkDefinition.name(), linkDefinition);
        }
        return new ChainDefinition(name, initial, built);
    }
}
