package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.document.DocumentNode;

import org.jmouse.cad.document.part.FetchedPart;
import org.jmouse.cad.document.part.PartCandidate;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.document.part.ProviderState;
import org.jmouse.cad.kicad.InstalledKiCadParts;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadModels;
import org.jmouse.cad.mesh.ModelSource;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.mesh.MeshFormats;
import org.jmouse.plugin.PluginSettingValues;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * KiCad's libraries installed on the server, as the plugin's settings name their folders at the moment they are asked
 * about: changing a folder leaves what was read of the others behind and reads again, without a restart. Reading stands
 * apart from what the plugin brings to the core's points, which stay the same whatever the folders are.
 */
final class InstalledKiCad {

    static final String SYMBOL_FOLDERS    = "symbol-folders";
    static final String FOOTPRINT_FOLDERS = "footprint-folders";
    static final String MODEL_FOLDERS     = "model-folders";

    private record Folders(List<Path> symbols, List<Path> footprints, List<Path> models) {
    }

    private record Libraries(Folders folders, KiCadFootprintLibraries footprints, InstalledKiCadParts parts, KiCadModels models) {
    }

    private final PluginSettingValues settings;
    private final KiCadLibraries      libraries;
    private final Executor            indexing;
    private Libraries                 current;

    /** @param indexing where the libraries are read the first time they are asked about */
    InstalledKiCad(PluginSettingValues settings, KiCadLibraries libraries, Executor indexing) {
        this.settings = settings;
        this.libraries = libraries;
        this.indexing = indexing;
    }

    /** The footprints a design's parts are looked up in after its own. */
    KiCadFootprintLibraries footprints() {
        return current().footprints();
    }

    /** The installed libraries as a place parts are looked for. */
    PartSearchProvider parts() {
        return new PartSearchProvider() {
            @Override
            public String id() {
                return current().parts().id();
            }

            @Override
            public String name() {
                return current().parts().name();
            }

            @Override
            public ProviderState state() {
                return current().parts().state();
            }

            @Override
            public List<PartCandidate> search(String query, int limit) {
                return current().parts().search(query, limit);
            }

            @Override
            public FetchedPart fetch(String identifier) {
                return current().parts().fetch(identifier);
            }
        };
    }

    /** The installed 3D model libraries, a source of the 3D models plugin's where it runs. */
    ModelSource models() {
        return new ModelSource() {
            @Override
            public String id() {
                return current().models().id();
            }

            @Override
            public List<FetchedModel> modelsOf(DocumentNode footprint) {
                return current().models().modelsOf(footprint);
            }
        };
    }

    private synchronized Libraries current() {
        Folders folders = new Folders(settings.folders(SYMBOL_FOLDERS), settings.folders(FOOTPRINT_FOLDERS), settings.folders(MODEL_FOLDERS));
        if (current == null || !current.folders().equals(folders)) {
            current = librariesIn(folders);
        }
        return current;
    }

    private Libraries librariesIn(Folders folders) {
        KiCadFootprintLibraries footprints = folders.footprints().isEmpty() ? KiCadFootprintLibraries.NONE
                : KiCadFootprintLibraries.in(folders.footprints());
        return new Libraries(folders, footprints,
                new InstalledKiCadParts(folders.symbols(), folders.footprints(), footprints, indexing, libraries),
                new KiCadModels(folders.models(), footprints, MeshFormats.standard(), libraries));
    }
}
