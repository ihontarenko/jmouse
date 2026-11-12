package org.jmouse.cad.plugin.mesh;

import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.settings.SettingsOwner;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.document.part.FetchCompletion;
import org.jmouse.cad.exchange.AcceptedFiles;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.mesh.ModelCompletion;
import org.jmouse.cad.mesh.ModelMeshes;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.mesh.ModelSource;
import org.jmouse.mesh.MeshFormats;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;
import java.util.function.Supplier;

/**
 * 🧊 3D models in jmCAD: the {@code model} document in the language, the models of every footprint a part is fetched
 * with — taken from each place a plugin brings ({@link ModelSource#POINT}) —, and a model served as the one mesh a
 * viewer draws, read by the library's readers and by any a plugin brings ({@link ModelMeshes#READERS}); and a pcb
 * written as GLB, its parts by those models. Without it the engine has no 3D: a footprint's model line names a model nothing reads, and the interface shows no 3D view.
 */
public final class MeshPlugin implements Plugin {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/mesh/glb. */
    public static final SettingsSection<PcbGlbExport.Settings> GLB = new SettingsSection<>("plugin/mesh/glb", "PCB as GLB", SettingsOwner.ENGINE_DATA, PcbGlbExport.Settings.class);

    public static final String ID = "cad-mesh";

    private final Supplier<List<ModelSource>> sources;
    private final List<DocumentExport>        exports;

    /**
     * @param sources the places models come from, as the plugins brought them, read on each fetch
     * @param exports the pcb written in 3D
     */
    public MeshPlugin(Supplier<List<ModelSource>> sources, List<DocumentExport> exports) {
        this.sources = sources;
        this.exports = List.copyOf(exports);
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "3D models", MeshPlugin.class).drawnBy(MeshPlugin.class, "mesh.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(SettingsSection.POINT, GLB);
        registration.declare(ModelSource.POINT);
        registration.declare(ModelMeshes.READERS);
        registration.contribute(DocumentModule.POINT, new ModelModule());
        registration.contribute(FetchCompletion.POINT, new ModelCompletion(sources));
        exports.forEach(export -> registration.contribute(DocumentExport.POINT, export));
        // A model's files are kept as they came, in any format a model may be in — read here or not yet, as STEP.
        MeshFormats formats = MeshFormats.standard();
        registration.contribute(DocumentImport.POINT, new ModelFilesImport(formats));
        registration.contribute(AcceptedFiles.POINT, new AcceptedFiles("3D models",
                formats.extensions().stream().map(extension -> "*." + extension).toList(),
                formats.formats().stream().map(MeshFormats.MeshFormat::mediaType).toList()));
    }
}
