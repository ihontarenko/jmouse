package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.table.ShippedTable;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.table.ReadTable;
import org.jmouse.settings.Parameter;
import org.jmouse.settings.Section;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.mesh.ModelSource;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.List;

/**
 * KiCad in jmCAD: designs read, schematics written, and the libraries installed on the server as a place parts are
 * looked for. All of it is brought to the core's points; without this plugin the core simply offers no KiCad.
 *
 * <p>Where the 3D models plugin runs, KiCad's installed model libraries are one more place a fetched footprint's
 * models come from — added to that plugin's sources when it runs, and nothing when it does not: KiCad stands on no
 * other plugin.</p>
 */
public final class KiCadPlugin implements Plugin {

    public static final String ID = "cad-kicad";

    /** The format's word on the exchange routes: {@code /exchange/kicad/read}. */
    static final String FORMAT = "kicad";

    static final String TITLE = "KiCad";

    /** What a reader finds the folders under. */
    private static final String LIBRARIES = "Installed libraries";
    /** Its section of installed libraries. */
    static final String LIBRARIES_SECTION = "libraries";

    private final InstalledKiCad installed;
    private final Supplier<PcbArtwork.Settings> artwork;
    private final ReadTable<KiCadLibraryMap>    maps;
    private final ReadTable<KiCadLibraries>     filings;
    private final CadStandards                  standards;
    private final List<Path>     usualSymbolFolders;
    private final List<Path>     usualFootprintFolders;
    private final List<Path>     usualModelFolders;

    /**
     * @param installed the libraries installed on the server, in the folders the settings declared here name
     * @param artwork   how a board written for KiCad draws what no document says, as every format draws it, as the installation has it now
     */
    KiCadPlugin(InstalledKiCad installed, List<Path> usualSymbolFolders, List<Path> usualFootprintFolders,
                List<Path> usualModelFolders, Supplier<PcbArtwork.Settings> artwork,
                ReadTable<KiCadLibraryMap> maps, ReadTable<KiCadLibraries> filings, CadStandards standards) {
        this.installed = installed;
        this.maps = maps;
        this.filings = filings;
        this.standards = standards;
        this.artwork = artwork;
        this.usualSymbolFolders = List.copyOf(usualSymbolFolders);
        this.usualFootprintFolders = List.copyOf(usualFootprintFolders);
        this.usualModelFolders = List.copyOf(usualModelFolders);
    }

    @Override
    public List<Section> settings() {
        return List.of(Section.fixed(LIBRARIES_SECTION, LIBRARIES, null, List.of(
                Parameter.folders(InstalledKiCad.SYMBOL_FOLDERS, "Symbol libraries",
                        "Folders of .kicad_sym files: the part search reads them.", usualSymbolFolders),
                Parameter.folders(InstalledKiCad.FOOTPRINT_FOLDERS, "Footprint libraries",
                        "Folders of .pretty libraries: the part search reads them, and an import looks there after a design's own.",
                        usualFootprintFolders),
                Parameter.folders(InstalledKiCad.MODEL_FOLDERS, "3D model libraries",
                        "Folders of .3dshapes libraries: a fetched footprint's models are taken from them.", usualModelFolders))));
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "KiCad", KiCadPlugin.class).withIcon(KiCadPlugin.class, "kicad.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        // The tables its module reads, as pages of the library.
        registration.contribute(ShippedTable.POINT, KiCadLibraries.SHIPPED.shipping(KiCadLibraries.TABLE));
        registration.contribute(ShippedTable.POINT, KiCadLibraryMap.SHIPPED.shipping(KiCadLibraryMap.TABLE));
        registration.contribute(DocumentImport.POINT, new KiCadDesignImport(installed::libraries, installed::models, maps, filings));
        registration.contribute(DocumentExport.POINT, new KiCadDesignWriting(artwork, maps, filings, standards));
        registration.contribute(PartSearchProvider.POINT, installed.parts());
        registration.contributeWhenDeclared(ModelSource.POINT, installed.models());
    }
}
