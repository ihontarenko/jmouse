package org.jmouse.cad.plugin.step;

import org.jmouse.settings.Section;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.mesh.ModelMeshes;
import org.jmouse.mesh.MeshReader;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.Set;

/**
 * STEP in jmCAD: a reader of the format brought to the 3D models plugin, which declares where readers go — so it stands
 * on that plugin —, and a pcb written as a STEP assembly, its parts by their models' own STEP files, brought to the
 * core's exports — the pcb and its parts, or with its copper, mask and silkscreen too. Each runs OpenCascade by a
 * command of its own, a setting of this plugin; while it is empty, that half is off: a model with no other file than its STEP is shown as its
 * footprint's body, and a pcb has no STEP among its formats.
 */
public final class StepPlugin implements Plugin {

    public static final String ID = "cad-step";

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-step/export. */
    public static final SettingsSection<PcbStepExport.Settings> EXPORT = SettingsSection.ofPlugin(ID, "export", "STEP export", PcbStepExport.Settings.class);

    /** The 3D models plugin's id: the point this one contributes to is its. */
    static final String MESH = "cad-mesh";

    private final MeshReader                                      reader;
    private final Function<PcbStepExport.Variant, DocumentExport> exports;
    private final CadSettings                                     sections;
    private final List<Section>                             settings;

    /**
     * @param exports  the pcb's export of one variant
     * @param sections the installation's settings: the variants offered are its export's, as they stand at the start
     * @param settings its programs' settings; its export's section is added to them
     */
    public StepPlugin(MeshReader reader, Function<PcbStepExport.Variant, DocumentExport> exports, CadSettings sections,
                      List<Section> settings) {
        this.reader = reader;
        this.exports = exports;
        this.sections = sections;
        this.settings = Stream.concat(settings.stream(), Stream.of(CadSettings.declarationOf(EXPORT))).toList();
    }

    @Override
    public List<Section> settings() {
        return settings;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "STEP", StepPlugin.class).drawnBy(StepPlugin.class, "step.svg")
                .requiring(Set.of(MESH));
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(ModelMeshes.READERS, reader);
        sections.read(EXPORT).exports().forEach(variant -> registration.contribute(DocumentExport.POINT, exports.apply(variant)));
    }
}
