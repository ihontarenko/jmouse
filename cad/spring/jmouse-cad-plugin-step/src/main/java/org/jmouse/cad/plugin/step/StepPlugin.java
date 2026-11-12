package org.jmouse.cad.plugin.step;

import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.settings.SettingsOwner;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.mesh.ModelMeshes;
import org.jmouse.mesh.MeshReader;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.plugin.PluginSetting;

import java.util.List;
import java.util.Set;

/**
 * STEP in jmCAD: a reader of the format brought to the 3D models plugin, which declares where readers go — so it stands
 * on that plugin —, and a pcb written as a STEP assembly, its parts by their models' own STEP files, brought to the
 * core's exports — the pcb and its parts, or with its copper, mask and silkscreen too. Each runs OpenCascade by a
 * command of its own, a setting of this plugin; while it is empty, that half is off: a model with no other file than its STEP is shown as its
 * footprint's body, and a pcb has no STEP among its formats.
 */
public final class StepPlugin implements Plugin {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/step/export. */
    public static final SettingsSection<PcbStepExport.Settings> EXPORT = new SettingsSection<>("plugin/step/export", "STEP export", SettingsOwner.ENGINE_DATA, PcbStepExport.Settings.class);

    public static final String ID = "cad-step";

    /** The 3D models plugin's id: the point this one contributes to is its. */
    static final String MESH = "cad-mesh";

    private final MeshReader           reader;
    private final List<DocumentExport> exports;
    private final List<PluginSetting>  settings;

    public StepPlugin(MeshReader reader, List<DocumentExport> exports, List<PluginSetting> settings) {
        this.reader = reader;
        this.exports = List.copyOf(exports);
        this.settings = List.copyOf(settings);
    }

    @Override
    public List<PluginSetting> settings() {
        return settings;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "STEP", StepPlugin.class).drawnBy(StepPlugin.class, "step.svg")
                .requiring(Set.of(MESH));
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(SettingsSection.POINT, EXPORT);
        registration.contribute(ModelMeshes.READERS, reader);
        exports.forEach(export -> registration.contribute(DocumentExport.POINT, export));
    }
}
