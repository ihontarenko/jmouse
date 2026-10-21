package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.Computation;
import org.jmouse.parallax.Computations;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.jpa.catalogue.Instrument;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.object.ObjectKind;
import org.jmouse.parallax.object.ObjectNature;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.circuit.elaboration.SheetHierarchy;
import org.jmouse.parallax.store.catalogue.CatalogueViews.CategoryView;
import org.jmouse.parallax.store.catalogue.CatalogueViews.InstrumentView;
import org.jmouse.parallax.store.catalogue.CatalogueViews.ObjectDetail;
import org.jmouse.parallax.store.catalogue.CatalogueViews.ObjectSummary;
import org.jmouse.parallax.store.catalogue.CatalogueViews.RevisionSource;
import org.jmouse.parallax.store.catalogue.CatalogueViews.RevisionView;
import org.jmouse.parallax.store.catalogue.CatalogueViews.SheetView;
import org.jmouse.parallax.store.catalogue.ComponentModels;
import org.jmouse.parallax.store.catalogue.Instruments;
import org.jmouse.parallax.store.catalogue.ObjectDraft;
import org.jmouse.parallax.store.catalogue.ShippedObjects;
import org.jmouse.parallax.store.support.ParallaxPage;
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

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 📚 The catalogue of objects, their categories, an owner's objects, the instruments, the component models and the
 * representation vocabulary.
 *
 * <p>No route here declares what it requires: the product does, through {@link ParallaxAccessRules}. A route marked
 * {@link PublicEndpoint} decides inside its service, per object, and the rules leave it alone.</p>
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX)
public class CatalogueController {

    private static final int MAXIMUM_SUMMARIES = 500;

    /** Whole-number settings that set how many elements are produced: more of them is more to draw. */
    private static final Set<String> RENDER_LOAD = Set.of("resolution", "samples", "count", "lines", "seeds", "starts",
            "rectangles", "shown", "steps", "iterations");

    private final Catalogue       catalogue;
    private final Instruments     instruments;
    private final Computations    computations;
    private final ComponentModels componentModels;
    private final ShippedObjects  shippedObjects;

    public CatalogueController(Catalogue catalogue, Instruments instruments, Computations computations,
                               ComponentModels componentModels, ShippedObjects shippedObjects) {
        this.catalogue       = catalogue;
        this.instruments     = instruments;
        this.computations    = computations;
        this.componentModels = componentModels;
        this.shippedObjects  = shippedObjects;
    }

    /** The slugs a shelf or a set names. */
    public record SlugList(List<String> slugs) {
    }

    /** An object's new status. */
    public record StatusRequest(MathematicalObject.Status status) {
    }

    /**
     * One representation the server can compute, described well enough for the interface to build its controls.
     * {@code renderLoad} names the settings whose increase adds to what a viewport draws — the ones the interface stops
     * raising when the reader's machine falls below the minimum frame rate.
     */
    public record RepresentationDescription(RepresentationKind kind, RepresentationKind.Family family,
                                            List<SettingSpecification> settings, List<String> renderLoad) {
    }

    @GetMapping("/component-models")
    @PublicEndpoint("lists built-in models and only saved models the caller may read")
    public List<ComponentModels.ReferenceView> componentModels() {
        return componentModels.list();
    }

    @GetMapping("/component-models/{reference}")
    @PublicEndpoint("built-in models are public; saved models require object read access")
    public ComponentModels.ModelView componentModel(@PathVariable String reference) {
        return componentModels.describe(reference);
    }

    @GetMapping("/symbols")
    @PublicEndpoint("lists catalogue symbols and only owned symbols the caller may read, with their drawings")
    public List<ComponentModels.SymbolView> symbols() {
        return componentModels.symbols();
    }

    @GetMapping("/footprints")
    @PublicEndpoint("lists catalogue footprints and only owned footprints the caller may read, with their drawings")
    public List<ComponentModels.FootprintView> footprints() {
        return componentModels.footprints();
    }

    @GetMapping("/footprints/{slug}")
    @PublicEndpoint("a footprint the caller may read, and the components it may read that land on it")
    public ComponentModels.FootprintDetail footprint(@PathVariable String slug) {
        return componentModels.footprint(slug);
    }

    @GetMapping("/catalogue/categories")
    @PublicEndpoint("the published catalogue is open to every reader")
    public List<CategoryView> categories(@RequestParam(required = false) ObjectNature nature) {
        return catalogue.categories(nature);
    }

    @PutMapping("/catalogue/categories/{code}")
    public CategoryView saveCategory(@PathVariable String code, @RequestBody Catalogue.CategoryDraft draft) {
        return catalogue.saveCategory(code, draft);
    }

    @DeleteMapping("/catalogue/categories/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable String code) {
        catalogue.deleteCategory(code);
    }

    @GetMapping("/catalogue/objects")
    @PublicEndpoint("the published catalogue is open to every reader; drafts are added only for an editor")
    public ParallaxPage<ObjectSummary> browse(@RequestParam(required = false) String text,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) ObjectKind kind,
                                              @RequestParam(required = false) ObjectNature nature,
                                              @RequestParam(required = false) String tag,
                                              @RequestParam(required = false) Boolean featured,
                                              @RequestParam(defaultValue = "false") boolean includeDrafts,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "48") int size) {
        return catalogue.browse(text, category, kind, nature, tag, featured, includeDrafts, page, size);
    }

    /** The objects a shelf or a set names, summarised in the order asked; one the reader cannot open is left out. */
    @PostMapping("/catalogue/objects/summaries")
    @PublicEndpoint("ObjectAccess decides per object, as for the object itself")
    public List<ObjectSummary> summaries(@RequestBody SlugList request) {
        if (request.slugs() == null || request.slugs().size() > MAXIMUM_SUMMARIES) {
            throw ParallaxProblem.invalid("Ask for at most " + MAXIMUM_SUMMARIES + " objects at once.");
        }

        return catalogue.summaries(request.slugs());
    }

    @GetMapping("/catalogue/objects/{objectId}")
    @PublicEndpoint("ObjectAccess decides per object: published to anybody, drafts to editors, owned objects to the owner's readers")
    public ObjectDetail object(@PathVariable String objectId) {
        return catalogue.detail(objectId);
    }

    /** Every saved revision of the object's jMX text, newest first. */
    @GetMapping("/catalogue/objects/{objectId}/revisions")
    @PublicEndpoint("ObjectAccess decides per object, as for the object itself")
    public List<RevisionView> revisions(@PathVariable String objectId) {
        return catalogue.revisions(objectId);
    }

    /**
     * The sheet a path of blocks reaches from this circuit ({@code ?path=S1,S2}), as that instance is run within it:
     * its parts, nets and layout under the names the circuit's live run reports.
     */
    @GetMapping("/catalogue/objects/{objectId}/sheet")
    @PublicEndpoint("ObjectAccess decides per object, as for the object itself; the sheets are read with the circuit's own components")
    public SheetHierarchy.PlacedSheet placedSheet(@PathVariable String objectId, @RequestParam List<String> path) {
        return catalogue.placedSheet(objectId, path);
    }

    /** One revision's text: what {@code slug@revision} resolves to. */
    @GetMapping("/catalogue/objects/{objectId}/revisions/{revision}")
    @PublicEndpoint("ObjectAccess decides per object, as for the object itself")
    public RevisionSource revision(@PathVariable String objectId, @PathVariable int revision) {
        return catalogue.revision(objectId, revision);
    }

    /** A new catalogue object: its jMX text in {@code source}, or the fields a form built. */
    @PostMapping("/catalogue/objects")
    @ResponseStatus(HttpStatus.CREATED)
    public ObjectDetail create(@RequestBody ObjectDraft draft) {
        return catalogue.createInCatalogue(draft);
    }

    /** The object's next revision: its jMX text in {@code source}, or the fields a form built. */
    @PutMapping("/catalogue/objects/{objectId}")
    @PublicEndpoint("ObjectAccess.requireWritable decides: catalogue writing for the catalogue, scene writing at the object's owner")
    public ObjectDetail update(@PathVariable String objectId, @RequestParam long version, @RequestBody ObjectDraft draft) {
        return catalogue.update(objectId, draft, version);
    }

    /** The shipped catalogue's newer text, taken over a curator's edit of the object. */
    @PostMapping("/catalogue/objects/{objectId}/shipped")
    @PublicEndpoint("ObjectAccess.requireWritable decides: catalogue writing for the catalogue")
    public ObjectDetail takeShippedText(@PathVariable String objectId) {
        ObjectDetail current = catalogue.detail(objectId);
        String       source  = shippedObjects.sourceOf(current.summary().slug())
                .orElseThrow(() -> ParallaxProblem.conflict("The catalogue no longer ships this object."));

        return catalogue.takeShippedText(objectId, source);
    }

    @PutMapping("/catalogue/objects/{objectId}/status")
    public ObjectDetail changeStatus(@PathVariable String objectId, @RequestBody StatusRequest request) {
        return catalogue.changeStatus(objectId, request.status());
    }

    @DeleteMapping("/catalogue/objects/{objectId}")
    @PublicEndpoint("ObjectAccess.requireWritable decides, as for an update")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String objectId) {
        catalogue.delete(objectId);
    }

    /** The objects one owner holds, most recently changed first. */
    @GetMapping(ParallaxManagementRoutes.OWNER + "/objects")
    public ParallaxPage<ObjectSummary> ownedObjects(@OwnerAddress String owner,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "48") int size) {
        return catalogue.ownedObjects(OwnerAddressArgumentResolver.ownerOf(owner), page, size);
    }

    /** The circuits one owner holds as the sheets of one design — a project's tree: ports, globals, and the blocks. */
    @GetMapping(ParallaxManagementRoutes.OWNER + "/sheets")
    public List<SheetView> ownedSheets(@OwnerAddress String owner) {
        return catalogue.sheets(OwnerAddressArgumentResolver.ownerOf(owner));
    }

    /** A new object that belongs to one owner rather than to the shared catalogue. */
    @PostMapping(ParallaxManagementRoutes.OWNER + "/objects")
    @ResponseStatus(HttpStatus.CREATED)
    public ObjectDetail createOwned(@OwnerAddress String owner, @RequestBody ObjectDraft draft) {
        return catalogue.createOwned(OwnerAddressArgumentResolver.ownerOf(owner), draft);
    }

    @GetMapping("/instruments")
    @PublicEndpoint("the visualizers and labs are the portal's navigation, shown to every reader")
    public List<InstrumentView> instruments(@RequestParam(required = false) Instrument.Section section) {
        return instruments.list(section);
    }

    @GetMapping("/instruments/{code}")
    @PublicEndpoint("the visualizers and labs are the portal's navigation, shown to every reader")
    public InstrumentView instrument(@PathVariable String code) {
        return instruments.get(code);
    }

    @PutMapping("/instruments/{code}")
    public InstrumentView saveInstrument(@PathVariable String code, @RequestBody Instruments.InstrumentDraft draft) {
        return instruments.save(code, draft);
    }

    @GetMapping("/representations")
    @PublicEndpoint("the vocabulary of representations and their settings describes the API, not any data")
    public List<RepresentationDescription> representations() {
        return computations.all().stream()
                .sorted(Comparator.comparing(Computation::kind))
                .map(CatalogueController::describe)
                .toList();
    }

    private static RepresentationDescription describe(Computation computation) {
        List<String> renderLoad = computation.settings().stream()
                .filter(setting -> setting.type() == SettingSpecification.Type.INTEGER
                        && RENDER_LOAD.contains(setting.name()))
                .map(SettingSpecification::name)
                .toList();

        return new RepresentationDescription(computation.kind(), computation.kind().family(), computation.settings(),
                renderLoad);
    }
}
