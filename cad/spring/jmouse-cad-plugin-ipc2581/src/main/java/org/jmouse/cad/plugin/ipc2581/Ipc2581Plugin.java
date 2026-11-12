package org.jmouse.cad.plugin.ipc2581;

import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.settings.SettingsOwner;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

/**
 * IPC-2581 in the engine: a pcb written as the one file a pcb house both makes and assembles it from — the copper,
 * mask, paste, silkscreen and holes of the Gerber set, with the stackup, the packages, the parts, the nets and the bill of
 * materials beside them. It brings the format to the core's exports; without it no pcb is written as IPC-2581.
 */
public final class Ipc2581Plugin implements Plugin {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/ipc2581/export. */
    public static final SettingsSection<PcbIpc2581Export.Settings> EXPORT = new SettingsSection<>("plugin/ipc2581/export", "IPC-2581 export", SettingsOwner.ENGINE_DATA, PcbIpc2581Export.Settings.class);

    public static final String ID = "cad-ipc2581";

    private final DocumentExport export;

    public Ipc2581Plugin(DocumentExport export) {
        this.export = export;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "IPC-2581", Ipc2581Plugin.class).drawnBy(Ipc2581Plugin.class, "ipc2581.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(SettingsSection.POINT, EXPORT);
        registration.contribute(DocumentExport.POINT, export);
    }
}
