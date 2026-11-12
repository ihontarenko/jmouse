package org.jmouse.cad.kicad;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Which KiCad footprint each of ours is, by its slug — the one answer a schematic's {@code Footprint} field and a board's
 * footprint give, so the two agree: the KiCad footprint the library map pairs its code with, else the library id it was
 * imported from, else the slug in the own library.
 */
final class KiCadFootprintIds {

    private final CatalogueDocuments catalogue;
    private final KiCadLibraryMap map;
    private final KiCadLibraries libraries;
    private final Map<String, String> ids = new HashMap<>();

    KiCadFootprintIds(CatalogueDocuments catalogue, KiCadLibraryMap map, KiCadLibraries libraries) {
        this.catalogue = catalogue;
        this.map = map;
        this.libraries = libraries;
    }

    /** The {@code LIBRARY:NAME} KiCad knows a footprint by. */
    String of(String footprint) {
        return ids.computeIfAbsent(footprint, slug -> {
            Optional<MetaNode> meta = catalogue.find(slug).filter(found -> found.kind().equals(PcbModule.FOOTPRINT))
                    .flatMap(document -> document.block(MetaNode.class));
            return meta.flatMap(found -> found.text(MetaNode.CODE)).flatMap(map::footprintIdOf)
                    .or(() -> meta.flatMap(found -> found.text(MetaNode.SOURCE)).flatMap(KiCadSources::footprintIdOf))
                    .orElse(libraries.ownLibrary() + ":" + slug);
        });
    }

    /** Whether a footprint came from KiCad — its own drawing there is KiCad's, and needs not be carried. */
    boolean fromKiCad(String footprint) {
        return !of(footprint).startsWith(libraries.ownLibrary() + ":");
    }

    /** The footprint's document, as the reader may see it. */
    Optional<DocumentNode> document(String footprint) {
        return catalogue.find(footprint).filter(found -> found.kind().equals(PcbModule.FOOTPRINT));
    }
}
