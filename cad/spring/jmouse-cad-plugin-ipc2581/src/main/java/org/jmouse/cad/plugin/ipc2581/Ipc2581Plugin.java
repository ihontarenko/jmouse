package org.jmouse.cad.plugin.ipc2581;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.settings.Section;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;

/**
 * IPC-2581 in the engine: a pcb written as the one file a pcb house both makes and assembles it from — the copper,
 * mask, paste, silkscreen and holes of the Gerber set, with the stackup, the packages, the parts, the nets and the bill of
 * materials beside them. It brings the format to the core's exports; without it no pcb is written as IPC-2581.
 */
public final class Ipc2581Plugin implements Plugin {

    public static final String ID = "cad-ipc2581";

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-ipc2581/export. */
    public static final SettingsSection<PcbIpc2581Export.Settings> EXPORT = SettingsSection.ofPlugin(ID, "export", "IPC-2581 export", PcbIpc2581Export.Settings.class);

    private final DocumentExport export;

    public Ipc2581Plugin(DocumentExport export) {
        this.export = export;
    }

    @Override
    public List<Section> settings() {
        return List.of(CadSettings.declarationOf(EXPORT));
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "IPC-2581", Ipc2581Plugin.class).withIcon(Ipc2581Plugin.class, "ipc2581.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentExport.POINT, export);
    }
}
