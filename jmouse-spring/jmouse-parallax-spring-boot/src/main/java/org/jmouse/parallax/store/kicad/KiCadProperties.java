package org.jmouse.parallax.store.kicad;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Where a KiCad import reads which catalogue component each KiCad symbol stands for — a jMD library map, the product's
 * own, since it names the product's components —, where it looks up the footprints a design's parts name, and where
 * the part search finds KiCad's installed symbols.
 *
 * <pre>{@code
 * jmouse.parallax.kicad.library-map: classpath:kicad/library-map.jmd
 * jmouse.parallax.kicad.footprint-libraries: C:/Program Files/KiCad/8.0/share/kicad/footprints
 * jmouse.parallax.kicad.symbol-libraries: C:/Program Files/KiCad/8.0/share/kicad/symbols
 * }</pre>
 *
 * @param libraryMap         the map's location; without one every symbol is imported as a part without a model
 * @param footprintLibraries folders of KiCad footprint libraries ({@code *.pretty}), searched after the libraries a
 *                           design sends with it; without any, only those are
 * @param symbolLibraries    folders of KiCad symbol libraries ({@code *.kicad_sym}) the part search looks in; without
 *                           any, it finds no KiCad symbol
 */
@ConfigurationProperties("jmouse.parallax.kicad")
public record KiCadProperties(String libraryMap, List<String> footprintLibraries, List<String> symbolLibraries) {

    public KiCadProperties {
        libraryMap = libraryMap == null || libraryMap.isBlank() ? "classpath:kicad/library-map.jmd" : libraryMap.strip();
        footprintLibraries = footprintLibraries == null ? List.of()
                : footprintLibraries.stream().filter(folder -> !folder.isBlank()).map(String::strip).toList();
        symbolLibraries = symbolLibraries == null ? List.of()
                : symbolLibraries.stream().filter(folder -> !folder.isBlank()).map(String::strip).toList();
    }
}
