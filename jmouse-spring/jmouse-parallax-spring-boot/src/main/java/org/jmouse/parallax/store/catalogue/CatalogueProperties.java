package org.jmouse.parallax.store.catalogue;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Where the shipped catalogue is read from: resource patterns, each {@code <scheme>:<root>/<wildcards>}, a file named
 * by its path under the root.
 *
 * @param locations                the {@code .jmx} files the catalogue import reads — objects, sections, collections and
 *                                 the documents a {@code ShippedDocumentHandler} takes; empty ships nothing
 * @param sharedDocumentLocations  the logic families and function libraries components share; empty shares none
 */
@ConfigurationProperties("jmouse.parallax.catalogue")
public record CatalogueProperties(List<String> locations, List<String> sharedDocumentLocations) {

    public CatalogueProperties {
        locations = locations == null ? List.of() : List.copyOf(locations);
        sharedDocumentLocations = sharedDocumentLocations == null ? List.of() : List.copyOf(sharedDocumentLocations);
    }
}
