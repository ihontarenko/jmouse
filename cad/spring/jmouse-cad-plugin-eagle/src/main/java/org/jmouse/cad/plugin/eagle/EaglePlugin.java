package org.jmouse.cad.plugin.eagle;

import org.jmouse.cad.eagle.EagleLibraries;
import org.jmouse.cad.table.ShippedTable;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

/**
 * Eagle in the engine: a schematic read as jM documents — the footprints, symbols and components its libraries carry,
 * and its sheets as one circuit — and a pcb read with its schematic, laid out from that circuit. Without it no Eagle
 * file is read.
 */
public final class EaglePlugin implements Plugin {

    public static final String ID = "cad-eagle";

    /** The format's word on the exchange routes: {@code /exchange/eagle/read}. */
    static final String FORMAT = "eagle";

    static final String TITLE = "Eagle";

    private final DocumentImport designs;

    public EaglePlugin(DocumentImport designs) {
        this.designs = designs;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, TITLE, EaglePlugin.class).withIcon(EaglePlugin.class, "eagle.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        // The tables its module reads, as pages of the library.
        registration.contribute(ShippedTable.POINT, EagleLibraries.SHIPPED.shipping(EagleLibraries.TABLE));
        registration.contribute(DocumentImport.POINT, designs);
    }
}
