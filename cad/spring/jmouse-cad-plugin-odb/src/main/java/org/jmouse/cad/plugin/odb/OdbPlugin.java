package org.jmouse.cad.plugin.odb;

import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.settings.SettingsOwner;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

/**
 * ODB++ in the engine: a pcb written as the job a pcb house's CAM system reads — its layers in their order, its
 * profile, the copper, mask, paste, silkscreen and holes of the Gerber set. It brings the format to the core's exports;
 * without it no pcb is written as ODB++.
 */
public final class OdbPlugin implements Plugin {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/odb/export. */
    public static final SettingsSection<PcbOdbExport.Settings> EXPORT = new SettingsSection<>("plugin/odb/export", "ODB++ export", SettingsOwner.ENGINE_DATA, PcbOdbExport.Settings.class);

    public static final String ID = "cad-odb";

    private final DocumentExport export;

    public OdbPlugin(DocumentExport export) {
        this.export = export;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "ODB++", OdbPlugin.class).drawnBy(OdbPlugin.class, "odb.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(SettingsSection.POINT, EXPORT);
        registration.contribute(DocumentExport.POINT, export);
    }
}
