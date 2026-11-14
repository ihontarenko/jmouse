package org.jmouse.cad.mesh;

import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.part.FetchCompletion;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.document.part.FetchedPart;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A fetched part completed with the 3D models of its footprints: every place models come from ({@link ModelSource}) is
 * asked about each footprint, and each model it holds is stored before the footprint that names it, its files with it.
 * A model two footprints name is brought once.
 */
public final class ModelCompletion implements FetchCompletion {

    private final Supplier<List<ModelSource>> sources;

    /** @param sources the places models come from, read anew on each fetch */
    public ModelCompletion(Supplier<List<ModelSource>> sources) {
        this.sources = sources;
    }

    @Override
    public FetchedPart completed(FetchedPart part) {
        List<ModelSource> places = sources.get();
        if (places.isEmpty()) {
            return part;
        }
        Set<String> brought = new LinkedHashSet<>();
        List<DocumentNode> documents = new ArrayList<>();
        List<FetchedFile> files = new ArrayList<>(part.files());
        for (DocumentNode document : part.documents()) {
            if (document.kind().word().equals(PcbModule.FOOTPRINT.word())) {
                for (ModelSource place : places) {
                    for (ModelSource.FetchedModel model : place.modelsOf(document)) {
                        if (brought.add(model.document().slug())) {
                            documents.add(model.document());
                            files.addAll(model.files());
                        }
                    }
                }
            }
            documents.add(document);
        }
        return brought.isEmpty() ? part : new FetchedPart(documents, files, part.open(), part.notes());
    }
}
