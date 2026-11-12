package org.jmouse.cad.kicad;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.mesh.ModelFolders;
import org.jmouse.cad.mesh.ModelSource;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.kicad.footprint.KiCadModel;
import org.jmouse.mesh.MeshFormats;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 🧊 KiCad's installed 3D model libraries — {@code share/kicad/3dmodels} — as a place footprints' models come from. What
 * is KiCad's is which models a footprint names — a footprint converted from KiCad says so in its {@code meta}, and its
 * KiCad footprint is read again for them — and whose these libraries are; finding a model's files and writing its
 * document is any model library's ({@link ModelFolders}), under the conventions KiCad's files keep
 * ({@link KiCadLibraries#modelScales}, {@link KiCadLibraries#modelUps}).
 */
public final class KiCadModels implements ModelSource {

    private final ModelFolders            folders;
    private final KiCadFootprintLibraries footprints;
    private final KiCadLibraries          libraries;

    /**
     * @param folders    folders of {@code *.3dshapes} libraries, the first that holds a model's files giving them
     * @param footprints KiCad's installed footprints, a converted footprint's KiCad one read again from them
     */
    public KiCadModels(List<Path> folders, KiCadFootprintLibraries footprints, MeshFormats formats, KiCadLibraries libraries) {
        this.folders = new ModelFolders(folders, formats, conventionsOf(libraries));
        this.footprints = footprints;
        this.libraries = libraries;
    }

    @Override
    public String id() {
        return "kicad";
    }

    @Override
    public List<FetchedModel> modelsOf(DocumentNode footprint) {
        Optional<KiCadFootprint> found = KiCadSources.footprintIdOf(Provenance.of(footprint).source()).flatMap(footprints::find);
        if (found.isEmpty()) {
            return List.of();
        }
        List<FetchedModel> models = new ArrayList<>();
        for (KiCadModel model : found.get().models()) {
            find(model).ifPresent(models::add);
        }
        return models;
    }

    /** The model a footprint names, with its files; empty when no folder holds a file of it in a format known here. */
    public Optional<FetchedModel> find(KiCadModel model) {
        String libraryPath = model.libraryPath();
        String name = Path.of(libraryPath).getFileName().toString();
        return folders.find(libraryPath, KiCadFootprintDrawing.modelSlugOf(libraryPath), name, metaOf(libraryPath, name))
                .map(found -> found.filedIn(libraries.libraryCategoryDocument(Provenance.of(found.document())).stream().toList()));
    }

    /** What a model of KiCad's libraries says of itself: where it was imported from, where it is filed, whose it is. */
    private MetaNode metaOf(String libraryPath, String name) {
        Provenance provenance = libraries.provenance(KiCadSources.model(libraryPath), true);
        MetaNode meta = MetaNode.filed(name + " — a 3D model imported from KiCad", KiCadFootprintDrawing.modelCodeOf(libraryPath),
                libraries.libraryCategory(provenance),
                libraries.modelTags());
        provenance.writeInto(meta);
        return meta;
    }

    /** The conventions KiCad's own model files keep, as its libraries' table says them. */
    private static ModelFolders.Conventions conventionsOf(KiCadLibraries libraries) {
        Map<String, MeshFormats.Axis> ups = new LinkedHashMap<>();
        libraries.modelUps().forEach((format, axis) -> ups.put(format, MeshFormats.Axis.valueOf(axis.toUpperCase(Locale.ROOT))));
        return new ModelFolders.Conventions(libraries.modelScales(), ups);
    }
}
