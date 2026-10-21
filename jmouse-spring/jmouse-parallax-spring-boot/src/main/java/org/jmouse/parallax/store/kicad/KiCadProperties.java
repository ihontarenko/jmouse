package org.jmouse.parallax.store.kicad;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Where a KiCad import reads which catalogue component each KiCad symbol stands for — a jMD library map, the product's
 * own, since it names the product's components —, and where it looks up the footprints a design's parts name.
 *
 * <pre>{@code
 * jmouse.parallax.kicad.library-map: classpath:kicad/library-map.jmd
 * jmouse.parallax.kicad.footprint-libraries: C:/Program Files/KiCad/8.0/share/kicad/footprints
 * }</pre>
 *
 * @param libraryMap         the map's location; without one every symbol is imported as a part without a model
 * @param footprintLibraries folders of KiCad footprint libraries ({@code *.pretty}), searched after the libraries a
 *                           design sends with it; without any, only those are
 */
@ConfigurationProperties("jmouse.parallax.kicad")
public record KiCadProperties(String libraryMap, List<String> footprintLibraries) {

    public KiCadProperties {
        libraryMap = libraryMap == null || libraryMap.isBlank() ? "classpath:kicad/library-map.jmd" : libraryMap.strip();
        footprintLibraries = footprintLibraries == null ? List.of()
                : footprintLibraries.stream().filter(folder -> !folder.isBlank()).map(String::strip).toList();
    }
}
