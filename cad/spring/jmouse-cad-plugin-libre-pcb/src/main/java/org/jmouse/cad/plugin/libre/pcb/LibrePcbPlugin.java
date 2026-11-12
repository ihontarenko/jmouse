package org.jmouse.cad.plugin.libre.pcb;

import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

/** LibrePCB in jmCAD: a project read with its sheets and its board, brought to the core's point. */
public final class LibrePcbPlugin implements Plugin {

    public static final String ID = "cad-libre-pcb";

    /** The format's word on the exchange routes: {@code /exchange/libre-pcb/read}. */
    static final String FORMAT = "libre-pcb";

    static final String TITLE = "LibrePCB";

    private final DocumentImport designs;

    public LibrePcbPlugin(DocumentImport designs) {
        this.designs = designs;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, TITLE, LibrePcbPlugin.class).drawnBy(LibrePcbPlugin.class, "libre-pcb.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentImport.POINT, designs);
    }
}
