package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.table.ReadTable;
import org.jmouse.kicad.symbol.KiCadSymbolLibraries;
import org.jmouse.cad.kicad.InstalledKiCadLibraries;
import org.jmouse.cad.document.DocumentNode;

import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.document.part.InstalledLibraryParts;
import org.jmouse.cad.kicad.InstalledKiCadParts;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadModels;
import org.jmouse.cad.mesh.ModelSource;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.mesh.MeshFormats;
import org.jmouse.settings.ParameterValues;

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

    private record Libraries(Folders folders, KiCadLibraries filing, InstalledKiCadLibraries installed, PartSearchProvider parts, KiCadModels models) {
    }

    private final ParameterValues settings;
    /** KiCad's filing — whose its libraries are —, as the engine's table says it now. */
    private final ReadTable<KiCadLibraries> filings;
    private final Executor            indexing;
    private Libraries                 current;

    /** @param indexing where the libraries are read, as the engine comes up or the first time they are asked about */
    InstalledKiCad(ParameterValues settings, ReadTable<KiCadLibraries> filings, Executor indexing) {
        this.settings = settings;
        this.filings = filings;
        this.indexing = indexing;
    }

    /** The footprints a design's parts are looked up in after its own, and the symbols that say which of its symbols are KiCad's. */
    InstalledKiCadLibraries libraries() {
        return current().installed();
    }

    /** The installed libraries as a place parts are looked for: the one the settings name now. */
    PartSearchProvider parts() {
        return PartSearchProvider.following(InstalledKiCadParts.ID, InstalledKiCadParts.NAME, () -> current().parts());
    }

    /** The installed 3D model libraries, a source of the 3D models plugin's where it runs. */
    ModelSource models() {
        return new ModelSource() {
            @Override
            public String id() {
                return InstalledKiCadParts.ID;
            }

            @Override
            public List<FetchedModel> modelsOf(DocumentNode footprint) {
                return current().models().modelsOf(footprint);
            }
        };
    }

    private synchronized Libraries current() {
        Folders folders = new Folders(settings.folders(SYMBOL_FOLDERS), settings.folders(FOOTPRINT_FOLDERS), settings.folders(MODEL_FOLDERS));
        KiCadLibraries filing = filings.current();
        if (current == null || !current.folders().equals(folders) || current.filing() != filing) {
            current = librariesIn(folders, filing);
        }
        return current;
    }

    private Libraries librariesIn(Folders folders, KiCadLibraries libraries) {
        KiCadFootprintLibraries footprints = folders.footprints().isEmpty() ? KiCadFootprintLibraries.NONE
                : KiCadFootprintLibraries.in(folders.footprints());
        KiCadSymbolLibraries symbols = folders.symbols().isEmpty() ? KiCadSymbolLibraries.NONE : KiCadSymbolLibraries.in(folders.symbols());
        return new Libraries(folders, libraries, new InstalledKiCadLibraries(footprints, symbols),
                new InstalledLibraryParts(new InstalledKiCadParts(folders.symbols(), folders.footprints(), footprints, libraries), indexing),
                new KiCadModels(folders.models(), footprints, MeshFormats.standard(), libraries));
    }
}
