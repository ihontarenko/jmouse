package org.jmouse.parallax.store.kicad;

import org.jmouse.datamap.DataMapReader;
import org.jmouse.parallax.kicad.KiCadLibraryMap;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The library map a KiCad import and export read — which jMX component each KiCad symbol is —, from the resource
 * {@code jmouse.parallax.kicad.library-map} names; an empty map when there is none. Read on every use, so an edited
 * map is taken without a restart.
 */
public class KiCadLibraryMaps {

    private final ResourceLoader resources;
    private final KiCadProperties properties;

    public KiCadLibraryMaps(ResourceLoader resources, KiCadProperties properties) {
        this.resources = resources;
        this.properties = properties;
    }

    public KiCadLibraryMap read() {
        var resource = resources.getResource(properties.libraryMap());
        if (!resource.exists()) {
            return KiCadLibraryMap.EMPTY;
        }
        try {
            String text = resource.getContentAsString(StandardCharsets.UTF_8);
            return KiCadLibraryMap.of(DataMapReader.read(properties.libraryMap(), text));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The KiCad library map " + properties.libraryMap() + " cannot be read", unreadable);
        }
    }
}
