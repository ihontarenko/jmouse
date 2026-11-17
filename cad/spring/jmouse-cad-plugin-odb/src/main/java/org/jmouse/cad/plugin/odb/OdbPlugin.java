package org.jmouse.cad.plugin.odb;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.settings.Section;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;

/**
 * ODB++ in the engine: a pcb written as the job a pcb house's CAM system reads — its layers in their order, its
 * profile, the copper, mask, paste, silkscreen and holes of the Gerber set. It brings the format to the core's exports;
 * without it no pcb is written as ODB++.
 */
public final class OdbPlugin implements Plugin {

    public static final String ID = "cad-odb";

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-odb/export. */
    public static final SettingsSection<PcbOdbExport.Settings> EXPORT = SettingsSection.ofPlugin(ID, "export", "ODB++ export", PcbOdbExport.Settings.class);

    private final DocumentExport export;

    public OdbPlugin(DocumentExport export) {
        this.export = export;
    }

    @Override
    public List<Section> settings() {
        return List.of(CadSettings.declarationOf(EXPORT));
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "ODB++", OdbPlugin.class).withIcon(OdbPlugin.class, "odb.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentExport.POINT, export);
    }
}
