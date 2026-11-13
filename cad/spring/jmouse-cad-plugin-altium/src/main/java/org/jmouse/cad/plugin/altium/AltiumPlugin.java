package org.jmouse.cad.plugin.altium;

import org.jmouse.cad.altium.AltiumLibraries;
import org.jmouse.cad.table.ShippedTable;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

/**
 * Altium in the engine: a schematic read as jM documents — the symbols and components its parts are drawn with, and the
 * sheet as a circuit, its nets traced from its wires —, and a PCB library as its footprints and the 3D models it embeds,
 * each named by its code in the library's namespace. Without it no Altium file is read.
 */
public final class AltiumPlugin implements Plugin {

    public static final String ID = "cad-altium";

    /** The format's word on the exchange routes: {@code /exchange/altium/read}. */
    static final String FORMAT = "altium";

    static final String TITLE = "Altium";

    private final DocumentImport schematics;

    public AltiumPlugin(DocumentImport schematics) {
        this.schematics = schematics;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, TITLE, AltiumPlugin.class).markedBy(AltiumPlugin.class, "altium.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        // The tables its module reads, as pages of the library.
        registration.contribute(ShippedTable.POINT, AltiumLibraries.SHIPPED.shipping(AltiumLibraries.TABLE));
        registration.contribute(DocumentImport.POINT, schematics);
    }
}
