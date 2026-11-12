package org.jmouse.cad.store.showcase;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.component.Seat;
import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.library.PartLibrary;
import org.jmouse.cad.store.library.PartLibrary.DrawnDocument;
import org.jmouse.cad.store.problem.CadProblem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 🪟 What a featured design shows to anybody: a published, featured circuit of the catalogue, the published pcb of the
 * catalogue that lays it out, the footprints its parts sit on and the 3D models those footprints name. Nothing else is
 * shown this way — the one rule a host may open to whoever it likes, kept here so every route that shows a design asks
 * it alike.
 */
public class FeaturedDesigns {

    private final DocumentStore documents;
    private final CadCatalogue  catalogue;
    private final PartLibrary   library;

    public FeaturedDesigns(DocumentStore documents, CadCatalogue catalogue, PartLibrary library) {
        this.documents = documents;
        this.catalogue = catalogue;
        this.library = library;
    }

    /** A featured design's circuit document. */
    public StoredDocument requireCircuit(String slug) {
        return documents.findBySlug(slug).filter(FeaturedDesigns::published)
                .filter(StoredDocument::isFeatured)
                .filter(found -> found.getKind().equals(SimulationModule.CIRCUIT.word()))
                .orElseThrow(() -> CadProblem.notFound("The featured circuit"));
    }

    /** A featured design's circuit, as it reads. */
    public Circuit requireCircuitModel(String slug) {
        StoredDocument circuit = requireCircuit(slug);
        if (!(catalogue.scope(null).head(circuit).model().orElse(null) instanceof Circuit read)) {
            throw CadProblem.notFound("The featured circuit");
        }
        return read;
    }

    /** The pcb that lays out a featured design's circuit: a published one of the catalogue; empty where it has none. */
    public Optional<StoredDocument> pcbOf(String slug) {
        requireCircuit(slug);
        return documents.findBySlug(PcbDrawing.slugOf(slug)).filter(FeaturedDesigns::published)
                .filter(found -> found.getKind().equals(PcbModule.PCB.word()));
    }

    /** The footprints a featured design's parts sit on: each its part's own, else its component's first. None without a pcb. */
    public List<DrawnDocument> footprints(String slug) {
        if (pcbOf(slug).isEmpty()) {
            return List.of();
        }
        Set<String> seated = new LinkedHashSet<>();
        for (Part part : requireCircuitModel(slug).parts()) {
            if (part.footprint() != null) {
                seated.add(part.footprint());
            } else {
                library.component(part.component(), null).footprints().stream().findFirst().map(Seat::footprint).ifPresent(seated::add);
            }
        }
        return seated.stream().map(footprint -> library.footprint(footprint).footprint()).toList();
    }

    /** Refuses a model a featured design's footprints do not name: what is shown of a design is what it stands with. */
    public void requireShows(String slug, String model) {
        boolean named = footprints(slug).stream().map(DrawnDocument::drawing)
                .filter(FootprintDrawing.class::isInstance).map(FootprintDrawing.class::cast)
                .flatMap(drawing -> drawing.models().stream()).anyMatch(shown -> shown.reference().equals(model));
        if (!named) {
            throw CadProblem.notFound("The model of the featured design");
        }
    }

    private static boolean published(StoredDocument document) {
        return document.isCatalogue() && document.getStatus() == DocumentStatus.PUBLISHED;
    }
}
