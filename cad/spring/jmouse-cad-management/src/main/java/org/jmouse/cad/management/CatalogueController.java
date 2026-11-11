package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.jpa.StoredPage;
import org.jmouse.cad.simulation.elaboration.SheetInstance;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CatalogueViews.CategoryDraft;
import org.jmouse.cad.store.catalogue.CatalogueViews.CategoryView;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentDetail;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentSummary;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentText;
import org.jmouse.cad.store.catalogue.CatalogueViews.RevisionSource;
import org.jmouse.cad.store.catalogue.CatalogueViews.RevisionView;
import org.jmouse.cad.store.catalogue.CatalogueViews.SheetView;
import org.jmouse.cad.store.catalogue.CatalogueViews.SlugList;
import org.jmouse.cad.store.catalogue.CatalogueViews.StatusChange;
import org.jmouse.cad.store.library.ShippedLibrary;
import org.jmouse.cad.store.problem.CadProblem;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 📚 The catalogue of documents and its categories, and an owner's documents and sheets —
 * {@code JMOUSE_CAD_API.md} §2.1 and §2.2.
 *
 * <p>No route here declares what it requires: the product does, through {@link CadAccessRules}. A route marked
 * {@link PublicEndpoint} decides inside its service, per document, and the rules leave it alone.</p>
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class CatalogueController {

    /** The most slugs one summaries request may ask for. */
    private static final int MAXIMUM_SUMMARIES = 500;

    private final CadCatalogue   catalogue;
    private final ShippedLibrary library;

    public CatalogueController(CadCatalogue catalogue, ShippedLibrary library) {
        this.catalogue = catalogue;
        this.library = library;
    }

    // ── Categories ───────────────────────────────────────────────────────────────────────────────────────────────

    @GetMapping("/categories")
    @PublicEndpoint("the published catalogue is open to every reader")
    public List<CategoryView> categories(@RequestParam(required = false) String kind) {
        return catalogue.categories(kind);
    }

    @PutMapping("/categories/{code}")
    public CategoryView saveCategory(@PathVariable String code, @RequestBody CategoryDraft draft) {
        return catalogue.saveCategory(code, draft);
    }

    @DeleteMapping("/categories/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable String code) {
        catalogue.deleteCategory(code);
    }

    // ── Documents ────────────────────────────────────────────────────────────────────────────────────────────────

    @GetMapping("/documents")
    @PublicEndpoint("the published catalogue is open to every reader; drafts are added only for a curator")
    public StoredPage<DocumentSummary> browse(@RequestParam(required = false) String text,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) String kind,
                                              @RequestParam(required = false) String tag,
                                              @RequestParam(required = false) Boolean featured,
                                              @RequestParam(defaultValue = "false") boolean drafts,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        return catalogue.browse(text, category, kind, tag, featured, drafts, page, size);
    }

    /** The documents a list names, summarised in the order asked; one the reader cannot see is left out. */
    @PostMapping("/documents/summaries")
    @PublicEndpoint("each document is summarised only when the reader may see it")
    public List<DocumentSummary> summaries(@RequestBody SlugList request) {
        List<String> slugs = request.slugs() == null ? List.of() : request.slugs();
        if (slugs.size() > MAXIMUM_SUMMARIES) {
            throw CadProblem.invalid("Ask for at most " + MAXIMUM_SUMMARIES + " documents at once.");
        }
        return catalogue.summaries(slugs);
    }

    @GetMapping("/documents/{address}/sheet")
    @PublicEndpoint("a circuit's sheet is answered only when the reader may see the circuit")
    public SheetInstance placedSheet(@PathVariable String address, @RequestParam List<String> path) {
        return catalogue.placedSheet(address, path);
    }

    @GetMapping("/documents/{address}")
    @PublicEndpoint("a document is answered only when the reader may see it; a hidden one is not found")
    public DocumentDetail document(@PathVariable String address) {
        return catalogue.detail(address);
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDetail create(@RequestBody DocumentText text) {
        return catalogue.createInCatalogue(text.source());
    }

    @PutMapping("/documents/{id}")
    @PublicEndpoint("the document's own rule decides: a curator writes the catalogue's, an owner's writer its own")
    public DocumentDetail update(@PathVariable String id, @RequestParam long version, @RequestBody DocumentText text) {
        return catalogue.update(id, text.source(), version);
    }

    @PutMapping("/documents/{id}/status")
    public DocumentDetail changeStatus(@PathVariable String id, @RequestBody StatusChange change) {
        return catalogue.changeStatus(id, change.status());
    }

    /** The shipped file's newer text taken over a curator's edit, which stays one revision back. */
    @PostMapping("/documents/{id}/shipped")
    public DocumentDetail takeShipped(@PathVariable String id) {
        DocumentDetail current = catalogue.detail(id);
        String source = library.shippedSource(current.summary().slug())
                .orElseThrow(() -> CadProblem.conflict("Nothing ships for this document."));
        return catalogue.takeShippedText(id, source);
    }

    @DeleteMapping("/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicEndpoint("the document's own rule decides: a curator removes the catalogue's, an owner's writer its own")
    public void delete(@PathVariable String id) {
        catalogue.delete(id);
    }

    @GetMapping("/documents/{address}/revisions")
    @PublicEndpoint("a document's history is answered only when the reader may see the document")
    public List<RevisionView> revisions(@PathVariable String address) {
        return catalogue.revisions(address);
    }

    @GetMapping("/documents/{address}/revisions/{revision}")
    @PublicEndpoint("a revision is answered only when the reader may see the document")
    public RevisionSource revision(@PathVariable String address, @PathVariable String revision) {
        return catalogue.revision(address, revision);
    }

    @GetMapping("/documents/{address}/used-by")
    @PublicEndpoint("only the documents the reader may see are listed")
    public List<DocumentSummary> usedBy(@PathVariable String address,
                                        @RequestParam(required = false) String relation) {
        return catalogue.usedBy(address, relation);
    }

    // ── An owner's documents ─────────────────────────────────────────────────────────────────────────────────────

    @GetMapping(CadManagementRoutes.OWNER + "/documents")
    public StoredPage<DocumentSummary> owned(@OwnerAddress String owner,
                                             @RequestParam(required = false) String kind,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return catalogue.owned(OwnerAddressArgumentResolver.ownerOf(owner), kind, page, size);
    }

    @PostMapping(CadManagementRoutes.OWNER + "/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDetail createOwned(@OwnerAddress String owner, @RequestBody DocumentText text) {
        return catalogue.createOwned(OwnerAddressArgumentResolver.ownerOf(owner), text.source());
    }

    @GetMapping(CadManagementRoutes.OWNER + "/sheets")
    public List<SheetView> sheets(@OwnerAddress String owner) {
        return catalogue.sheets(OwnerAddressArgumentResolver.ownerOf(owner));
    }
}
