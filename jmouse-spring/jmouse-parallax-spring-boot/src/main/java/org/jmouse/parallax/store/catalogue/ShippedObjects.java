package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.store.catalogue.CatalogueProperties;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The text each object has in the shipped catalogue, by slug: what a curator who edited an object takes when its file
 * has a newer text ({@code shippedUpdatePending}). Read once, from the same files the start-up step writes from.
 */
public class ShippedObjects {

    private final CatalogueProperties properties;
    private volatile Map<String, String> sourcesBySlug;

    public ShippedObjects(CatalogueProperties properties) {
        this.properties = properties;
    }

    /** The shipped text of the object at this address, if the catalogue ships one. */
    public Optional<String> sourceOf(String slug) {
        Map<String, String> sources = sourcesBySlug;
        if (sources == null) {
            sources = ShippedCatalogue.load(properties.locations()).objects().stream().collect(Collectors.toUnmodifiableMap(
                    shipped -> shipped.document().slug(), ShippedCatalogue.ShippedObject::source, (first, second) -> first));
            sourcesBySlug = sources;
        }
        return Optional.ofNullable(sources.get(slug));
    }
}
