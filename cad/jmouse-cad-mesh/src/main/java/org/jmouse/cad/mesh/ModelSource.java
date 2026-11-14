package org.jmouse.cad.mesh;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;

import java.util.List;

/**
 * 🧊 A place the 3D models of footprints come from: a library installed on the server, a supplier's service. Asked about
 * every footprint a part is fetched with; it answers for the footprints it knows — by where the footprint came from,
 * which its {@code meta} says — and for no other.
 */
public interface ModelSource extends Identified {

    Contributions<ModelSource> POINT = new Contributions<>("cad.models.sources", ModelSource.class);

    /**
     * A model, as a document that names its files, and those files, each kept as it came.
     *
     * @param document the model document: whose it is in its {@code meta}, its files by name, filed by what it is in a category that
     *                 stands
     */
    record FetchedModel(DocumentNode document, List<FetchedFile> files) {

        public FetchedModel {
            files = List.copyOf(files);
        }
    }

    /**
     * The models a footprint names that this place holds; none for a footprint it does not know.
     *
     * @param footprint the footprint document, its model lines naming each model by slug
     */
    List<FetchedModel> modelsOf(DocumentNode footprint);
}
