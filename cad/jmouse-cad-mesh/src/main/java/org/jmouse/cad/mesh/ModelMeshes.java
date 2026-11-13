package org.jmouse.cad.mesh;

import org.jmouse.cad.table.TableSource;
import org.jmouse.cad.table.ReadTable;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.source.DocumentResources;
import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshFormats;
import org.jmouse.mesh.MeshReader;
import org.jmouse.mesh.MeshReaders;
import org.jmouse.mesh.MeshScene;
import org.jmouse.mesh.gltf.GlbWriter;
import org.jmouse.plugin.Contributions;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A model document as the one form a viewer draws — with where its body can be marked ({@link ModelMarkings}) beside it —:
 * the first of its files a reader here reads — a mesh before exact
 * geometry, which needs a kernel to be cut into triangles —, placed in millimetres with Z up and written as GLB.
 */
public final class ModelMeshes {

    /** Where a reader of another format is brought — STEP, by a plugin that has a geometry kernel. */
    public static final Contributions<MeshReader> READERS = new Contributions<>("cad.mesh.readers", MeshReader.class);

    private final MeshFormats formats;
    private final MeshReaders readers;
    private final GlbWriter writer = new GlbWriter();
    /** What a drawn model is made of, as the table says it now. */
    private final ReadTable<ModelMaterials> materials;

    /** @param tables where the materials table is read: the engine's catalogue, or the tables as they ship */
    public ModelMeshes(MeshFormats formats, MeshReaders readers, TableSource tables) {
        this.formats = formats;
        this.readers = readers;
        this.materials = ModelMaterials.readFrom(tables);
    }

    /**
     * The model's body as GLB, in millimetres with Z up.
     *
     * @throws IllegalArgumentException when none of its files is in a format read here, saying which formats it holds
     */
    public byte[] glb(DocumentNode model, DocumentResources files) {
        return writer.write(MeshScene.of(mesh(model, files)).withExtras(ModelMarkings.extrasOf(ModelMarkings.of(model), materials.current())));
    }

    /** The model's body, in millimetres with Z up: drawn from its shapes, or read from the first of its files read here. */
    public Mesh mesh(DocumentNode model, DocumentResources files) {
        List<ModelShapes.Shape> shapes = ModelShapes.of(model);
        if (!shapes.isEmpty()) {
            return ModelShapes.mesh(shapes, materials.current());
        }
        List<ModelFile> written = ModelFile.of(model);
        ModelFile chosen = written.stream()
                .filter(file -> readers.of(file.format()).isPresent() && formats.byId(file.format()).isPresent())
                .min(Comparator.comparing(file -> formats.byId(file.format()).orElseThrow().geometry() == MeshFormats.Geometry.EXACT))
                .orElseThrow(() -> new IllegalArgumentException("The model " + model.slug() + " holds no file in a format read here"
                        + (written.isEmpty() ? "" : ": it holds " + written.stream().map(ModelFile::format).distinct()
                        .collect(Collectors.joining(", ")))));
        MeshFormats.MeshFormat format = formats.byId(chosen.format()).orElseThrow();
        MeshReader reader = readers.of(chosen.format()).orElseThrow();
        try (InputStream input = files.open(chosen.name())) {
            return reader.read(input, chosen.name()).placed(Optional.ofNullable(chosen.scale()).orElse(format.unit()),
                    Optional.ofNullable(chosen.up()).orElse(format.up()));
        } catch (IOException failure) {
            throw new UncheckedIOException("The model file " + chosen.name() + " could not be read", failure);
        }
    }
}
