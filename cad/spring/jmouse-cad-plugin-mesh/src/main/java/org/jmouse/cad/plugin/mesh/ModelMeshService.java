package org.jmouse.cad.plugin.mesh;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.mesh.ModelMeshes;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.core.cache.LRUCache;
import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshFormats;
import org.jmouse.mesh.MeshReadException;
import org.jmouse.mesh.MeshReaders;
import org.jmouse.plugin.Extensions;

import java.io.UncheckedIOException;
import java.util.function.Supplier;

/**
 * A model document as GLB, for whoever may read it ({@link ModelDocuments}). A model is read again only when it changes —
 * the GLB is kept by the document's identity and version, so a pcb of many parts that share a package asks the disk
 * once.
 */
public class ModelMeshService {

    /** How many models are kept read: a pcb's packages, several times over. */
    private static final int KEPT = 256;

    private final ModelDocuments models;
    private final Extensions     extensions;
    private final MeshFormats    formats = MeshFormats.standard();
    private final LRUCache<String, byte[]> read = new LRUCache<>(KEPT);

    public ModelMeshService(ModelDocuments models, Extensions extensions) {
        this.models = models;
        this.extensions = extensions;
    }

    /**
     * @param address the model's slug or identifier
     * @param owner   the place it is asked from, whose folders are looked in first; null for the store alone
     * @throws CadProblem when the reader may not see it, it is not a model, or none of its files reads
     */
    public byte[] glb(String address, DocumentOwner owner) {
        ModelDocuments.OpenedModel model = models.open(address, owner);
        synchronized (read) {
            byte[] kept = read.get(model.version());
            if (kept != null) {
                return kept;
            }
        }
        byte[] glb = meshOf(model);
        synchronized (read) {
            read.set(model.version(), glb);
        }
        return glb;
    }

    /**
     * The model's body in millimetres with Z up, read each time it is asked for: what a pcb written in 3D places once
     * for all the parts that show it.
     *
     * @param address the model's slug or identifier
     * @throws CadProblem when the reader may not see it, it is not a model, or none of its files reads
     */
    public Mesh mesh(String address, CatalogueDocuments place) {
        ModelDocuments.OpenedModel model = models.open(address, place);
        return read(() -> meshes().mesh(model.document(), model.files()));
    }

    private byte[] meshOf(ModelDocuments.OpenedModel model) {
        return read(() -> meshes().glb(model.document(), model.files()));
    }

    private ModelMeshes meshes() {
        return new ModelMeshes(formats, MeshReaders.standard().with(extensions.contributions(ModelMeshes.READERS)));
    }

    /** A model read, a file that is not read refused as the model not read, with why. */
    private static <T> T read(Supplier<T> reading) {
        try {
            return reading.get();
        } catch (IllegalArgumentException | MeshReadException refused) {
            throw new CadProblem.Unprocessable("model-not-read", refused.getMessage());
        } catch (UncheckedIOException failed) {
            throw new CadProblem.Unprocessable("model-not-read", failed.getMessage() + ": " + failed.getCause().getMessage());
        }
    }
}
