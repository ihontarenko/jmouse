package org.jmouse.parallax.store.kicad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where a KiCad import reads which catalogue component each KiCad symbol stands for: a jMD library map, the product's
 * own, since it names the product's components.
 *
 * <pre>{@code
 * jmouse.parallax.kicad.library-map: classpath:kicad/library-map.jmd
 * }</pre>
 *
 * @param libraryMap the map's location; without one every symbol is imported as a part without a model
 */
@ConfigurationProperties("jmouse.parallax.kicad")
public record KiCadProperties(String libraryMap) {

    public KiCadProperties {
        libraryMap = libraryMap == null || libraryMap.isBlank() ? "classpath:kicad/library-map.jmd" : libraryMap.strip();
    }
}
