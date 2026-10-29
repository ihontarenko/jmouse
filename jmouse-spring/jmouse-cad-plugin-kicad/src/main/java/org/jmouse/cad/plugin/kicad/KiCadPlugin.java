package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.kicad.InstalledKiCadParts;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

/**
 * KiCad in jmCAD: designs read, schematics written, and the libraries installed on the server as a place parts are
 * looked for. All of it is brought to the core's points; without this plugin the core simply offers no KiCad.
 */
public final class KiCadPlugin implements Plugin {

    public static final String ID = "cad-kicad";

    /** The format's word on the exchange routes: {@code /exchange/kicad/read}. */
    static final String FORMAT = "kicad";

    static final String TITLE = "KiCad";

    private final KiCadFootprintLibraries installedFootprints;
    private final InstalledKiCadParts     installedParts;

    /**
     * @param installedFootprints the footprints a design's parts are looked up in after its own
     * @param installedParts      the installed libraries as a place parts are looked for
     */
    public KiCadPlugin(KiCadFootprintLibraries installedFootprints, InstalledKiCadParts installedParts) {
        this.installedFootprints = installedFootprints;
        this.installedParts = installedParts;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "KiCad", KiCadPlugin.class).drawnBy(KiCadPlugin.class, "kicad.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentImport.POINT, new KiCadDesignImport(installedFootprints));
        registration.contribute(DocumentExport.POINT, new KiCadSchematicWriting());
        registration.contribute(PartSearchProvider.POINT, installedParts);
    }
}
