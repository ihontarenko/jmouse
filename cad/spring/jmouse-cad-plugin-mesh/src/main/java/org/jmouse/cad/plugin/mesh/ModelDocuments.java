package org.jmouse.cad.plugin.mesh;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.document.source.DocumentResources;
import org.jmouse.cad.document.source.HeldDocument;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentDetail;
import org.jmouse.cad.store.catalogue.StoredDocuments;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.cad.store.problem.CadProblem;

import java.util.Optional;


/**
 * Model documents as the files they are made of, for whoever may read them: the catalogue answers only a reader who may
 * see the document, and its files are those filed against it. What the mesh route reads a model from, and what a
 * plugin writing a pcb with its parts' models — STEP — reads them from.
 */
public class ModelDocuments {

    /**
     * A model as it is now.
     *
     * @param version what changes whenever the document does: its identity and version
     * @param files   its files, each by the name the document gives it
     */
    public record OpenedModel(DocumentNode document, String version, DocumentResources files) {
    }

    private final CadCatalogue  catalogue;
    private final CadLanguage   language;
    private final DocumentFiles files;
    private final CadAuthority  authority;
    private final CadPlaces     places;

    /** @param files the files documents hold; null where the installation keeps none, and every model is then refused */
    public ModelDocuments(CadCatalogue catalogue, CadLanguage language, DocumentFiles files, CadAuthority authority, CadPlaces places) {
        this.authority = authority;
        this.places = places;
        this.catalogue = catalogue;
        this.language = language;
        this.files = files;
    }

    /**
     * A model as a place finds it: one held besides the store — a file of a project's folder, its geometry the files beside
     * it — first, else the store's.
     *
     * @param place what asks: an export's documents; anything but a place's is the store alone
     */
    public OpenedModel open(String address, CatalogueDocuments place) {
        if (place instanceof StoredDocuments scope) {
            Optional<HeldDocument> held = scope.locate(address).filter(found -> found.location() instanceof DocumentLocation.Mounted);
            if (held.isPresent()) {
                return openedOf(address, held.get(), scope);
            }
            // A code is the copy nearest the place: the slug the place finds for it is the one opened.
            return open(scope.slugOf(address).orElse(address));
        }
        return open(address);
    }

    /** A model as the place of an owner finds it, for a reader who may read there. */
    public OpenedModel open(String address, DocumentOwner owner) {
        if (owner == null) {
            return open(address);
        }
        authority.require(CadCapability.OWNER_READ, places.requireOwner(owner));
        return open(address, catalogue.scope(owner));
    }

    private static OpenedModel openedOf(String address, HeldDocument held, StoredDocuments scope) {
        if (!ModelModule.MODEL.word().equals(held.document().kind().word())) {
            throw CadProblem.invalid(address + " is a " + held.document().kind().word() + ", not a model.");
        }
        return new OpenedModel(held.document(), held.version(), scope.resources(held));
    }

    /**
     * @param address the model's slug or identifier
     * @throws CadProblem when the reader may not see it, or it is not a model
     */
    public OpenedModel open(String address) {
        DocumentDetail detail = catalogue.detail(address);
        if (!ModelModule.MODEL.word().equals(detail.summary().kind())) {
            throw CadProblem.invalid(detail.summary().slug() + " is a " + detail.summary().kind() + ", not a model.");
        }
        String slug = detail.summary().slug();
        String documentId = detail.summary().id();
        DocumentNode model = language.language().readOne(DocumentInput.text(slug, detail.source()));
        return new OpenedModel(model, documentId + "@" + detail.version(), files.of(documentId, slug));
    }
}
