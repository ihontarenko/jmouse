package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.mesh.ModelSource;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.plugin.PluginSetting;

import java.nio.file.Path;
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

    private final InstalledKiCad installed;
    private final PcbArtwork.Settings artwork;
    private final List<Path>     usualSymbolFolders;
    private final List<Path>     usualFootprintFolders;
    private final List<Path>     usualModelFolders;

    /**
     * @param installed the libraries installed on the server, in the folders the settings declared here name
     * @param artwork   how a board written for KiCad draws what no document says, as every format draws it
     */
    KiCadPlugin(InstalledKiCad installed, List<Path> usualSymbolFolders, List<Path> usualFootprintFolders,
                List<Path> usualModelFolders, PcbArtwork.Settings artwork) {
        this.installed = installed;
        this.artwork = artwork;
        this.usualSymbolFolders = List.copyOf(usualSymbolFolders);
        this.usualFootprintFolders = List.copyOf(usualFootprintFolders);
        this.usualModelFolders = List.copyOf(usualModelFolders);
    }

    @Override
    public List<PluginSetting> settings() {
        return List.of(
                PluginSetting.folders(InstalledKiCad.SYMBOL_FOLDERS, "Symbol libraries",
                        "Folders of .kicad_sym files: the part search reads them.", usualSymbolFolders).inGroup(LIBRARIES),
                PluginSetting.folders(InstalledKiCad.FOOTPRINT_FOLDERS, "Footprint libraries",
                        "Folders of .pretty libraries: the part search reads them, and an import looks there after a design's own.",
                        usualFootprintFolders).inGroup(LIBRARIES),
                PluginSetting.folders(InstalledKiCad.MODEL_FOLDERS, "3D model libraries",
                        "Folders of .3dshapes libraries: a fetched footprint's models are taken from them.", usualModelFolders)
                        .inGroup(LIBRARIES));
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "KiCad", KiCadPlugin.class).markedBy(KiCadPlugin.class, "kicad.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentImport.POINT, new KiCadDesignImport(installed::footprints, installed::models));
        registration.contribute(DocumentExport.POINT, new KiCadDesignWriting(artwork));
        registration.contribute(PartSearchProvider.POINT, installed.parts());
        registration.contributeWhenDeclared(ModelSource.POINT, installed.models());
    }
}
