package org.jmouse.parallax.store.catalogue;

import org.jmouse.formula.FormulaException;
import org.jmouse.parallax.catalogue.HomeCategoryPolicy;
import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Computations;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.circuit.elaboration.SheetHierarchy;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectKind;
import org.jmouse.parallax.object.ObjectNature;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.authority.ParallaxPlaces;
import org.jmouse.parallax.jpa.catalogue.CategoryStore;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.jpa.catalogue.ObjectCategory;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.store.catalogue.CatalogueViews.CategoryView;
import org.jmouse.parallax.store.catalogue.CatalogueViews.ObjectDetail;
import org.jmouse.parallax.store.catalogue.CatalogueViews.ObjectSummary;
import org.jmouse.parallax.store.catalogue.CatalogueViews.PresetView;
import org.jmouse.parallax.store.catalogue.CatalogueViews.PropertyView;
import org.jmouse.parallax.store.catalogue.CatalogueViews.RelatedView;
import org.jmouse.parallax.store.catalogue.CatalogueViews.RepresentationView;
import org.jmouse.parallax.store.catalogue.CatalogueViews.RevisionSource;
import org.jmouse.parallax.store.catalogue.CatalogueViews.RevisionView;
import org.jmouse.parallax.store.catalogue.CatalogueViews.SheetBlockView;
import org.jmouse.parallax.store.catalogue.CatalogueViews.SheetView;
import org.jmouse.parallax.event.ObjectCreated;
import org.jmouse.parallax.event.ObjectDeleted;
import org.jmouse.parallax.event.ObjectStatusChanged;
import org.jmouse.parallax.store.formula.FormulaNotations;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.support.ParallaxIdentifiers;
import org.jmouse.parallax.store.support.ParallaxJson;
import org.jmouse.parallax.store.support.ParallaxPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The library of mathematical objects: browsing the published catalogue, an object's page, and writing objects —
 * into the catalogue by its editors, or for an owner (a workspace) by whoever may write there.
 *
 * <p>An object is its jMX text. Writing keeps a new revision of that text in {@link ObjectStore} and derives the
 * index columns of {@link MathematicalObject} from it; reading takes the head revision through the adapter
 * ({@link ObjectDocuments}) — the definition, sliders, properties, representations and presets all come from there,
 * read once per revision. Every write is checked as a whole: the text reads, the definition compiles against its
 * sliders, every representation applies to the object and its settings are in range, every relation names an object
 * that exists. An unchanged text writes nothing.</p>
 */
public class Catalogue {

    /** The tag that makes a collection a formula set, listed on the Formulas page; its items are ordinary objects. */
    public static final String FORMULA_SET_TAG = "formula-set";

    /** The section an object is filed in when its text names none. */
    private static final String GENERAL_CATEGORY = HomeCategoryPolicy.GENERAL;

    /** A property key: lowercase words joined by hyphens. */
    private static final Pattern PROPERTY_KEY = Pattern.compile("[a-z][a-z0-9-]{0,63}");

    private static final Logger LOGGER = LoggerFactory.getLogger(Catalogue.class);

    private final ObjectStore objects;
    private final CategoryStore categories;
    private final ObjectAccess objectAccess;
    private final ParallaxAuthority authority;
    private final Computations computations;
    private final ParallaxJson documents;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final FormulaNotations notations;
    private final ComponentModels componentModels;
    private final ObjectDocuments objectDocuments;
    private final ParallaxPlaces places;
    private final SharedDocuments sharedDocuments;
    private final HomeCategoryPolicy homeCategories;

    public Catalogue(ObjectStore objects, CategoryStore categories, ObjectAccess objectAccess,
                     ParallaxAuthority authority, Computations computations, ParallaxJson documents,
                     ApplicationEventPublisher events, Clock clock, FormulaNotations notations,
                     ComponentModels componentModels, ObjectDocuments objectDocuments,
                     ParallaxPlaces places, SharedDocuments sharedDocuments, HomeCategoryPolicy homeCategories) {
        this.objects = objects;
        this.categories = categories;
        this.objectAccess = objectAccess;
        this.authority = authority;
        this.computations = computations;
        this.documents = documents;
        this.events = events;
        this.clock = clock;
        this.notations = notations;
        this.componentModels = componentModels;
        this.objectDocuments = objectDocuments;
        this.places = places;
        this.sharedDocuments = sharedDocuments;
        this.homeCategories = homeCategories;
    }

    // ── Categories ───────────────────────────────────────────────────────────────────────────────────────────────

    public record CategoryDraft(String title, String description, Integer position) {
    }

    /** Creates a category, or renames and moves an existing one; its code never changes, objects point at it. */
    @Transactional
    public CategoryView saveCategory(String code, CategoryDraft draft) {
        authority.requireGlobally(ParallaxCapability.CATALOGUE_WRITE);
        if (code == null || !code.matches("[A-Z][A-Z0-9_]{1,39}")) {
            throw ParallaxProblem.invalid("A category code is 2–40 capital letters, digits and underscores.");
        }
        if (draft.title() == null || draft.title().isBlank()) {
            throw ParallaxProblem.invalid("Give the category a title.");
        }
        String title = draft.title().strip();
        String description = draft.description() == null || draft.description().isBlank() ? null : draft.description().strip();
        int position = draft.position() != null ? draft.position()
                : categories.allInOrder().stream().mapToInt(ObjectCategory::getPosition).max().orElse(0) + 1;
        ObjectCategory category = categories.find(code).orElse(null);
        if (category == null) {
            category = categories.save(new ObjectCategory(code, title, description, position));
        } else {
            category.describe(title, description, position);
        }
        ObjectCategory saved = category;
        return categories().stream().filter(view -> view.code().equals(saved.getCode())).findFirst().orElseThrow();
    }

    /** Removes an empty category; one that still files objects is refused, so no object loses its place. */
    @Transactional
    public void deleteCategory(String code) {
        authority.requireGlobally(ParallaxCapability.CATALOGUE_WRITE);
        if (!categories.exists(code)) {
            throw ParallaxProblem.notFound("The category");
        }
        if (objects.anyInCategory(code)) {
            throw ParallaxProblem.conflict("Objects are filed in this category; move them first.");
        }
        categories.delete(code);
    }

    @Transactional(readOnly = true)
    public List<CategoryView> categories() {
        return categories(null);
    }

    /** The sections, each counting its published objects — of one nature when {@code nature} names one, as a tab shows them. */
    @Transactional(readOnly = true)
    public List<CategoryView> categories(ObjectNature nature) {
        List<ObjectKind> kinds = Arrays.stream(ObjectKind.values())
                .filter(candidate -> nature == null || candidate.nature() == nature).toList();
        return categories.allInOrder().stream()
                .map(category -> new CategoryView(category.getCode(), category.getTitle(), category.getDescription(),
                        objects.countCatalogueInCategory(category.getCode(), MathematicalObject.Status.PUBLISHED, kinds)))
                .toList();
    }

    // ── Reading ──────────────────────────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ParallaxPage<ObjectSummary> browse(String text, String category, ObjectKind kind, ObjectNature nature, String tag,
                                           Boolean featured, boolean includeDrafts, int page, int size) {
        Set<MathematicalObject.Status> statuses = includeDrafts && authority.permitsGlobally(ParallaxCapability.CATALOGUE_WRITE)
                ? EnumSet.of(MathematicalObject.Status.PUBLISHED, MathematicalObject.Status.DRAFT)
                : EnumSet.of(MathematicalObject.Status.PUBLISHED);
        String pattern = text == null || text.isBlank() ? null : "%" + text.strip().toLowerCase() + "%";
        ParallaxPage.requireBounds(page, size);
        List<ObjectKind> kinds = Arrays.stream(ObjectKind.values())
                .filter(candidate -> nature == null || candidate.nature() == nature).toList();
        return ParallaxPage.of(objects.browse(statuses, blankToNull(category), kind, kinds, featured, pattern, blankToNull(tag),
                page, size), this::summary);
    }

    @Transactional(readOnly = true)
    public ParallaxPage<ObjectSummary> ownedObjects(ParallaxOwner owner, int page, int size) {
        authority.require(ParallaxCapability.SCENE_READ, places.requireOwner(owner));
        return ParallaxPage.of(objects.owned(owner, page, size), this::summary);
    }

    /**
     * The circuits one owner holds as the sheets of one design: each with its ports, its global nets and the blocks it
     * places the others with. A part counts as a block only when it names another circuit of the same owner — a
     * component, or a circuit somewhere else, is just a part.
     */
    @Transactional(readOnly = true)
    public List<SheetView> sheets(ParallaxOwner owner) {
        authority.require(ParallaxCapability.SCENE_READ, places.requireOwner(owner));
        List<MathematicalObject> circuits = objects.owned(owner, ObjectKind.CIRCUIT);
        Set<String> slugs = circuits.stream().map(MathematicalObject::getSlug).collect(Collectors.toSet());
        return circuits.stream().map(circuit -> sheet(circuit, slugs)).toList();
    }

    /**
     * The sheet a path of blocks reaches from a circuit, as that instance is run within it — what the circuit's bench
     * draws when it is opened inside a block, so the live run's names reach the sheet's parts.
     */
    @Transactional(readOnly = true)
    public SheetHierarchy.PlacedSheet placedSheet(String circuitId, List<String> path) {
        MathematicalObject object = objectAccess.requireReadable(circuitId);
        if (!(definitionOf(object) instanceof ObjectDefinition.Circuit circuit)) {
            throw ParallaxProblem.invalid("Only a circuit places sheets.");
        }
        Map<String, Double> values = new LinkedHashMap<>();
        parametersOf(object).forEach(parameter -> values.put(parameter.name(), parameter.defaultValue()));
        try {
            return SheetHierarchy.placed(circuit, values, componentModels.forCircuit(object), path);
        } catch (IllegalArgumentException refused) {
            throw ParallaxProblem.invalid(refused.getMessage());
        }
    }

    private SheetView sheet(MathematicalObject object, Set<String> sheets) {
        ObjectDefinition.Circuit circuit;
        try {
            if (!(definitionOf(object) instanceof ObjectDefinition.Circuit read)) {
                return new SheetView(object.getId(), object.getSlug(), object.getTitle(), object.getUpdatedAt(), List.of(),
                        List.of(), List.of(), "Its text is no longer a circuit.");
            }
            circuit = read;
        } catch (IllegalArgumentException | ParallaxProblem unreadable) {
            return new SheetView(object.getId(), object.getSlug(), object.getTitle(), object.getUpdatedAt(), List.of(),
                    List.of(), List.of(), unreadable.getMessage());
        }
        List<SheetBlockView> blocks = circuit.elements().stream()
                .filter(element -> element.type() == ObjectDefinition.CircuitElementType.MODEL && sheets.contains(element.component()))
                .map(element -> new SheetBlockView(element.id(), element.component())).toList();
        return new SheetView(object.getId(), object.getSlug(), object.getTitle(), object.getUpdatedAt(),
                circuit.boundary().ports().stream().map(ObjectDefinition.SheetPort::net).toList(),
                List.copyOf(circuit.boundary().globals()), blocks, null);
    }

    /** Readable objects by slug, in the order asked, summarised; a slug the reader cannot open is left out. */
    @Transactional(readOnly = true)
    public List<ObjectSummary> summaries(List<String> slugs) {
        Map<String, MathematicalObject> found = objects.findBySlugs(slugs).stream()
                .collect(Collectors.toMap(MathematicalObject::getSlug, Function.identity()));
        return slugs.stream().map(found::get).filter(object -> object != null && objectAccess.canRead(object))
                .map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public ObjectDetail detail(String idOrSlug) {
        return detail(objectAccess.requireReadable(idOrSlug));
    }

    @Transactional(readOnly = true)
    public MathematicalObject requireReadable(String idOrSlug) {
        return objectAccess.requireReadable(idOrSlug);
    }

    /** The object without an access decision: the worker's path, for a job that was authorized when it was queued. */
    @Transactional(readOnly = true)
    public MathematicalObject requireReadableUnchecked(String idOrSlug) {
        return objects.find(idOrSlug).or(() -> objects.findBySlug(idOrSlug))
                .orElseThrow(() -> ParallaxProblem.notFound("The object"));
    }

    /** Every revision of a readable object's text, newest first. */
    @Transactional(readOnly = true)
    public List<RevisionView> revisions(String idOrSlug) {
        MathematicalObject object = objectAccess.requireReadable(idOrSlug);
        return objects.revisions(object.getId()).stream()
                .map(revision -> new RevisionView(revision.revision(), revision.checksum(), revision.writtenAt(),
                        revision.writtenBy()))
                .toList();
    }

    /** One revision of a readable object's text. */
    @Transactional(readOnly = true)
    public RevisionSource revision(String idOrSlug, int revision) {
        MathematicalObject object = objectAccess.requireReadable(idOrSlug);
        return objects.sourceAt(object.getId(), revision).map(source -> new RevisionSource(revision, source))
                .orElseThrow(() -> ParallaxProblem.notFound("The revision"));
    }

    /** What the object's head text describes, read once per revision. */
    public DefinedObject documentOf(MathematicalObject object) {
        return objectDocuments.cached(object.getSourceChecksum(), () -> sourceOf(object), object.getSlug());
    }

    /** The head revision's text: the object itself. */
    public String sourceOf(MathematicalObject object) {
        return objects.sourceAt(object.getId(), object.getRevision())
                .orElseThrow(() -> new IllegalStateException("Object '" + object.getSlug() + "' has no revision "
                        + object.getRevision() + " of its text"));
    }

    public ObjectDefinition definitionOf(MathematicalObject object) {
        return documentOf(object).definition();
    }

    public List<ParameterDefinition> parametersOf(MathematicalObject object) {
        return documentOf(object).parameters();
    }

    /**
     * How the object is shown, in order, exactly one of them primary. A text that names no representation opens on
     * the first one its definition supports.
     */
    public List<DefinedObject.Representation> representationsOf(MathematicalObject object) {
        DefinedObject defined = documentOf(object);
        return representationsOf(defined);
    }

    /**
     * An object read out of its session: its definition and parameters, safe to compute on for as long as it takes,
     * and the components it may use — the same library {@link #compile} gives it, so a circuit built of saved
     * components (a generator, a model) is evaluated exactly as the bench runs it.
     */
    public record Evaluable(String objectId, ObjectDefinition definition, List<ParameterDefinition> parameters,
                            ComponentLibrary components) {
    }

    /** Every published catalogue object, or the one readable object named, detached for evaluation. */
    @Transactional(readOnly = true)
    public List<Evaluable> evaluables(String objectId) {
        List<MathematicalObject> targets = objectId == null
                ? objects.all().stream().filter(object -> object.isCatalogue()
                && object.getStatus() == MathematicalObject.Status.PUBLISHED).toList()
                : List.of(requireReadable(objectId));
        return targets.stream()
                .map(object -> new Evaluable(object.getId(), definitionOf(object), parametersOf(object),
                        componentModels.forCircuit(object)))
                .toList();
    }

    /** The object compiled with these parameter values: what a computation runs on. */
    public CompiledObject compile(MathematicalObject object, Map<String, Double> values) {
        return compileChecked(definitionOf(object), parametersOf(object), values, componentModels.forCircuit(object));
    }

    /** Compute an unsaved circuit variation with the object's parameters and its permitted component library. */
    public CompiledObject compileVariant(MathematicalObject object, ObjectDefinition.Circuit circuit,
                                         Map<String, Double> values) {
        if (!(definitionOf(object) instanceof ObjectDefinition.Circuit)) {
            throw ParallaxProblem.invalid("Only a circuit can be varied in the simulator.");
        }
        return compileChecked(circuit, parametersOf(object), values, componentModels.forCircuit(object));
    }

    /** Compiles a definition that pins no saved component; the shared families and function libraries resolve. */
    public CompiledObject compileChecked(ObjectDefinition definition, List<ParameterDefinition> parameters,
                                         Map<String, Double> values) {
        return compileChecked(definition, parameters, values, sharedDocuments);
    }

    public static CompiledObject compileChecked(ObjectDefinition definition, List<ParameterDefinition> parameters,
                                                Map<String, Double> values, ComponentLibrary library) {
        try {
            return DefinitionCompiler.compile(definition, parameters, values, library);
        } catch (FormulaException exception) {
            throw ParallaxProblem.invalid("invalid-formula", describe(exception));
        }
    }

    // ── Writing ──────────────────────────────────────────────────────────────────────────────────────────────────

    @Transactional
    public ObjectDetail createInCatalogue(ObjectDraft draft) {
        authority.requireGlobally(ParallaxCapability.CATALOGUE_WRITE);
        String actor = authority.requireActor();
        WrittenText text = textOfNew(draft);
        MathematicalObject object = writeRevision(new MathematicalObject(ParallaxIdentifiers.newIdentifier(),
                text.defined().slug(), null, actor, clock.instant()),
                text, actor, RelationIndexing.NOW);
        events.publishEvent(new ObjectCreated(object.getId(), object.getSlug(), actor));
        return detail(object);
    }

    @Transactional
    public ObjectDetail createOwned(ParallaxOwner owner, ObjectDraft draft) {
        authority.require(ParallaxCapability.SCENE_WRITE, places.requireOwner(owner));
        String actor = authority.requireActor();
        WrittenText text = textOfNew(draft);
        MathematicalObject created = new MathematicalObject(ParallaxIdentifiers.newIdentifier(), text.defined().slug(), owner,
                actor, clock.instant());
        created.changeStatus(MathematicalObject.Status.PUBLISHED, clock.instant());
        return detail(writeRevision(created, text, actor, RelationIndexing.NOW));
    }

    /** Appends the draft's text as the object's next revision; the same text as the head writes nothing. */
    @Transactional
    public ObjectDetail update(String id, ObjectDraft draft, long expectedVersion) {
        MathematicalObject object = objectAccess.requireReadable(id);
        objectAccess.requireWritable(object);
        if (object.getVersion() != expectedVersion) {
            throw ParallaxProblem.staleRevision("The object");
        }
        WrittenText text = textOf(draft, object.getSlug());
        if (!text.defined().slug().equals(object.getSlug())) {
            throw ParallaxProblem.invalidDocument("An object keeps its address: write 'as " + object.getSlug()
                    + "' after its title.", 1, 0);
        }
        if (text.checksum().equals(object.getSourceChecksum())) {
            return detail(object);
        }
        return detail(writeRevision(object, text, authority.requireActor(), RelationIndexing.NOW));
    }

    /**
     * Takes the shipped catalogue's newer text for an object a curator had edited: the file's text becomes the next
     * revision and nothing waits any more. The curator's edit stays in the history, one revision back.
     */
    @Transactional
    public ObjectDetail takeShippedText(String id, String shippedSource) {
        MathematicalObject object = objectAccess.requireReadable(id);
        objectAccess.requireWritable(object);
        if (!object.isCatalogue() || !object.isShippedUpdatePending()) {
            throw ParallaxProblem.conflict("Nothing newer ships for this object.");
        }
        WrittenText text = textOfSource(shippedSource, object.getSlug());
        object.recordShippedText(text.checksum());
        if (text.checksum().equals(object.getSourceChecksum())) {
            return detail(object);
        }
        return detail(writeRevision(object, text, authority.requireActor(), RelationIndexing.NOW));
    }

    @Transactional
    public ObjectDetail changeStatus(String id, MathematicalObject.Status status) {
        authority.requireGlobally(ParallaxCapability.CATALOGUE_PUBLISH);
        MathematicalObject object = objectAccess.requireReadable(id);
        if (!object.isCatalogue()) {
            throw ParallaxProblem.conflict("Only a catalogue object is published or archived.");
        }
        object.changeStatus(status, clock.instant());
        events.publishEvent(new ObjectStatusChanged(object.getId(), object.getSlug(), status.name(), authority.requireActor()));
        return detail(object);
    }

    @Transactional
    public void delete(String id) {
        MathematicalObject object = objectAccess.requireReadable(id);
        objectAccess.requireWritable(object);
        if (object.isCatalogue() && object.getStatus() == MathematicalObject.Status.PUBLISHED) {
            throw ParallaxProblem.conflict("A published catalogue object is archived, not deleted: links to it are out there.");
        }
        requireUnplaced(object);
        objects.remove(object);
        events.publishEvent(new ObjectDeleted(object.getId(), object.getSlug(), authority.requireActor()));
    }

    /**
     * Refuses to delete a sheet another circuit of its owner still places: that circuit would stop reading. The blocks
     * are named, so the reader knows what to remove first.
     */
    private void requireUnplaced(MathematicalObject object) {
        if (object.getKind() != ObjectKind.CIRCUIT || object.owner().isEmpty()) {
            return;
        }
        List<String> placing = sheets(object.owner().get()).stream()
                .flatMap(sheet -> sheet.blocks().stream().filter(block -> block.sheet().equals(object.getSlug()))
                        .map(block -> block.block() + " in “" + sheet.title() + "”"))
                .toList();
        if (!placing.isEmpty()) {
            throw ParallaxProblem.conflict("“" + object.getTitle() + "” is placed by " + String.join(", ", placing)
                    + ": remove those blocks first.");
        }
    }

    // ── The shipped catalogue ────────────────────────────────────────────────────────────────────────────────────

    /** What writing one shipped text did. */
    public enum ShippedOutcome {
        /** The object did not exist and was created from the text. */
        CREATED,
        /** Nobody had edited the object, so the newer text became its next revision. */
        REVISED,
        /** The text is the one already taken. */
        UNCHANGED,
        /** A curator edited the object since; the edit is kept and the newer text recorded as waiting. */
        KEPT_EDIT,
        /** A workspace's own object holds the address; it is left alone and nothing is written. */
        ADDRESS_TAKEN
    }

    /** One shipped text written: the object it is, and what writing it did. */
    public record ShippedWrite(String slug, ShippedOutcome outcome) {
    }

    /**
     * Writes one object the shipped catalogue holds, without a caller: the bootstrap's path. A new object is created
     * and published; one nobody has edited takes a newer text as its next revision; one a curator has edited keeps
     * the edit. Relations are indexed afterwards, by {@link #indexShippedRelations}, once every object they may name
     * exists.
     *
     * @param documentName where the text came from, for a refusal
     */
    @Transactional
    public ShippedWrite writeShipped(String source, String documentName) {
        WrittenText text = textOfSource(source, documentName);
        return new ShippedWrite(text.defined().slug(), writeShipped(text));
    }

    private ShippedOutcome writeShipped(WrittenText text) {
        MathematicalObject object = objects.findBySlug(text.defined().slug()).orElse(null);
        if (object == null) {
            object = new MathematicalObject(ParallaxIdentifiers.newIdentifier(), text.defined().slug(), null, null,
                    clock.instant());
            object.changeStatus(MathematicalObject.Status.PUBLISHED, clock.instant());
            object.recordShippedText(text.checksum());
            writeRevision(object, text, null, RelationIndexing.LATER);
            return ShippedOutcome.CREATED;
        }
        if (!object.isCatalogue()) {
            LOGGER.warn("A workspace object already holds the address '{}'; the shipped object is not written.",
                    object.getSlug());
            return ShippedOutcome.ADDRESS_TAKEN;
        }
        if (text.checksum().equals(object.getShippedChecksum()) && !object.isShippedUpdatePending()) {
            return ShippedOutcome.UNCHANGED;
        }
        if (text.checksum().equals(object.getSourceChecksum())) {
            object.recordShippedText(text.checksum());
            return ShippedOutcome.UNCHANGED;
        }
        if (objects.isHeadEdited(object.getId())) {
            object.recordShippedUpdateKept(text.checksum());
            return ShippedOutcome.KEPT_EDIT;
        }
        object.recordShippedText(text.checksum());
        writeRevision(object, text, null, RelationIndexing.LATER);
        return ShippedOutcome.REVISED;
    }

    /**
     * Derives a shipped object's relations from its head text, once the whole catalogue is written. A relation that
     * names an object which does not exist is left out and logged, never fatal: the catalogue must start.
     */
    @Transactional
    public void indexShippedRelations(String slug) {
        objects.findBySlug(slug).filter(MathematicalObject::isCatalogue).ifPresent(object -> object.replaceRelations(
                relationsOf(object, documentOf(object), RelationIndexing.LENIENT)));
    }

    // ── Internals: text ──────────────────────────────────────────────────────────────────────────────────────────

    /** A text about to be kept, with what it says and the checksum it is cached and compared under. */
    private record WrittenText(String source, DefinedObject defined, String checksum) {
    }

    /** When a write derives its relations, and how strictly. */
    private enum RelationIndexing {
        /** Now, refusing a relation to an object that does not exist. */
        NOW,
        /** By a later pass, once every object of a batch exists. */
        LATER,
        /** Now, leaving out a relation to an object that does not exist. */
        LENIENT
    }

    /** A new object's text; a draft built by a form gets a free slug, a text keeps the one it was written with. */
    private WrittenText textOfNew(ObjectDraft draft) {
        if (hasSource(draft)) {
            WrittenText text = textOfSource(draft.source(), "the new object");
            if (objects.existsBySlug(text.defined().slug())) {
                throw ParallaxProblem.conflict("An object is already called '" + text.defined().slug()
                        + "'; write another address after 'as'.");
            }
            return text;
        }
        if (draft.definition() instanceof ObjectDefinition.Component) {
            WrittenText text = textOf(draft, null);
            if (objects.existsBySlug(text.defined().slug())) {
                throw ParallaxProblem.conflict("A component is already called '" + text.defined().slug()
                        + "'; write another address after 'as' in its text.");
            }
            return text;
        }
        return textOf(draft, uniqueSlug(draft));
    }

    /** The text a draft stands for: its own, or the one the adapter writes for the fields a form built. */
    private WrittenText textOf(ObjectDraft draft, String slug) {
        if (hasSource(draft)) {
            return textOfSource(draft.source(), slug == null ? "the object" : slug);
        }
        if (draft.definition() instanceof ObjectDefinition.Component component) {
            String source = ComponentSources.withSymbol(component.source(), symbolReferenceOf(draft));
            return textOfSource(source, slug == null ? "the component" : slug);
        }
        String source = objectDocuments.write(definedFromDraft(draft, slug));
        return textOfSource(source, slug);
    }

    private WrittenText textOfSource(String source, String documentName) {
        if (source.length() > 1_000_000) {
            throw ParallaxProblem.invalid("A document is at most a million characters.");
        }
        return new WrittenText(source, objectDocuments.read(source, documentName), ObjectStore.checksumOf(source));
    }

    private static boolean hasSource(ObjectDraft draft) {
        return draft.source() != null && !draft.source().isBlank();
    }

    /** The component editor chooses a symbol as a property; the text records it on its {@code symbol uses} line. */
    private static String symbolReferenceOf(ObjectDraft draft) {
        return draft.properties().stream().filter(property -> "symbol-reference".equals(property.key()))
                .map(ObjectDraft.PropertyDraft::value).filter(value -> value != null && !value.isBlank())
                .findFirst().orElse(null);
    }

    /** The object a form describes, ready to be written as text. */
    private DefinedObject definedFromDraft(ObjectDraft draft, String slug) {
        if (draft.definition() == null) {
            throw ParallaxProblem.invalid("An object needs a definition.");
        }
        requireTitle(draft.title());
        List<DefinedObject.Property> properties = new ArrayList<>();
        for (ObjectDraft.PropertyDraft property : draft.properties()) {
            if (property.key() == null || !PROPERTY_KEY.matcher(property.key()).matches()) {
                throw ParallaxProblem.invalid("A property key is lowercase words joined by hyphens.");
            }
            properties.add(new DefinedObject.Property(property.key(), property.label(), property.value()));
        }
        List<DefinedObject.Relation> relations = new ArrayList<>();
        for (ObjectDraft.RelationDraft relation : draft.relations()) {
            relations.add(new DefinedObject.Relation(relation.slug(), relationKind(relation.kind())));
        }
        return new DefinedObject(slug, draft.title().strip(), blankToNull(draft.summary()), blankToNull(draft.description()),
                draft.category(), normalizedTags(draft.tags()).stream().toList(),
                Boolean.TRUE.equals(draft.featured()), blankToNull(draft.formulaLatex()), draft.definition(),
                draft.parameters(), properties,
                draft.representations().stream().map(representation -> new DefinedObject.Representation(
                        representation.kind(), representation.primary(),
                        representation.settings() == null ? Map.of() : representation.settings())).toList(),
                draft.presets().stream().map(preset -> new DefinedObject.Preset(preset.name(), preset.description(),
                        preset.values() == null ? Map.of() : preset.values(), preset.camera())).toList(),
                relations);
    }

    // ── Internals: one revision ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Checks a text as a whole, keeps it as the object's next revision and derives the index from it.
     *
     * @return the stored object: a new one is merged rather than persisted (its identifier is assigned), so the
     *         instance given is not the one the persistence context manages
     */
    private MathematicalObject writeRevision(MathematicalObject object, WrittenText text, String writtenBy,
                                             RelationIndexing relations) {
        DefinedObject defined = text.defined();
        validate(object, text);
        Instant now = clock.instant();
        int revision = object.getRevision() + 1;
        object.indexRevision(revision, text.checksum(), defined.definition().objectKind(), defined.title().strip(),
                blankToNull(defined.summary()), categoryOf(defined), defined.featured(), now);
        object.replaceTags(normalizedTags(defined.tags()));
        if (relations != RelationIndexing.LATER) {
            object.replaceRelations(relationsOf(object, defined, relations));
        }
        // Everything the index changes is flushed at once, so the version the answer carries is the one stored.
        MathematicalObject stored = objects.save(object);
        objects.flush();
        objects.appendSource(stored.getId(), revision, text.source(), writtenBy, now);
        return stored;
    }

    private void validate(MathematicalObject object, WrittenText text) {
        DefinedObject defined = text.defined();
        requireTitle(defined.title());
        if (defined.summary() != null && defined.summary().length() > 500) {
            throw ParallaxProblem.invalid("A summary is at most 500 characters.");
        }
        String category = categoryOf(defined);
        if (!categories.exists(category)) {
            throw ParallaxProblem.invalid("'" + category.toLowerCase() + "' is not a category.");
        }
        if (defined.definition() instanceof ObjectDefinition.Symbol symbol) {
            validateSymbolDrawing(symbol.drawing());
        }
        if (defined.definition() instanceof ObjectDefinition.Circuit circuit) {
            validatePartSymbols(circuit);
        }
        CompiledObject compiled;
        try {
            compiled = DefinitionCompiler.compile(defined.definition(), defined.parameters(), Map.of(),
                    componentModels.forCircuit(object));
        } catch (FormulaException refused) {
            throw ObjectDocuments.refusal(refused, text.source());
        }
        for (DefinedObject.Representation representation : representationsOf(defined)) {
            Computation computation = computations.forObject(representation.kind(), compiled.definition())
                    .orElseThrow(() -> ParallaxProblem.invalid(representation.kind() + " does not apply to a "
                            + compiled.definition().objectKind() + "."));
            ComputationSettings.resolve(computation.settings(), representation.settings());
        }
        for (DefinedObject.Preset preset : defined.presets()) {
            ParameterDefinition.requireKnown(defined.parameters(), preset.values());
        }
        for (DefinedObject.Property property : defined.properties()) {
            if (!PROPERTY_KEY.matcher(property.key()).matches()) {
                throw ParallaxProblem.invalid("A property key is lowercase words joined by hyphens: '"
                        + property.key() + "' is not.");
            }
        }
    }

    /**
     * A part drawn with another symbol than its default may only use one its component's {@code symbol uses} line names.
     * A part whose component the catalogue does not hold yet — a KiCad part waiting for its model — has no symbols to
     * choose from, and is drawn with the one it names; simulating it names the model it needs.
     */
    private void validatePartSymbols(ObjectDefinition.Circuit circuit) {
        for (ObjectDefinition.CircuitElement element : circuit.elements()) {
            if (element.symbol() == null) {
                continue;
            }
            String component = ExperienceDefinitions.componentOf(element);
            List<String> variants;
            try {
                variants = componentModels.describe(component).symbolVariants();
            } catch (ParallaxProblem.NotFound missing) {
                continue;
            }
            if (!variants.contains(element.symbol())) {
                throw ParallaxProblem.invalid("Part '" + element.id() + "' is drawn with '" + element.symbol() + "', which '"
                        + component + "' does not offer; its symbols are " + (variants.isEmpty() ? "none" : String.join(", ", variants)) + ".");
            }
        }
    }

    private void validateSymbolDrawing(String source) {
        try {
            Map<?, ?> drawing = documents.read(source, Map.class);
            if (!(drawing.get("version") instanceof Number version) || version.intValue() != 1
                    || !(drawing.get("paths") instanceof List<?> paths) || paths.size() > 200
                    || paths.stream().anyMatch(path -> !(path instanceof String text) || text.length() > 2000)) {
                throw ParallaxProblem.invalid("A symbol needs version 1 and up to 200 vector paths.");
            }
            if (drawing.get("pins") != null) {
                if (!(drawing.get("pins") instanceof List<?> pins) || pins.size() > 64) {
                    throw ParallaxProblem.invalid("A symbol can have at most 64 pins.");
                }
                Set<String> names = new LinkedHashSet<>();
                for (Object entry : pins) {
                    if (!(entry instanceof Map<?, ?> pin) || !(pin.get("name") instanceof String name)
                            || !name.matches("[A-Za-z_][A-Za-z_0-9]{0,31}") || !names.add(name)
                            || !(pin.get("x") instanceof Number x) || !(pin.get("y") instanceof Number y)
                            || !Double.isFinite(x.doubleValue()) || !Double.isFinite(y.doubleValue())) {
                        // Where a pin stands is the drawing's: a display's screen is wide, and its pins with it. It was capped at
                        // 80 across and 60 up or down until 2026-10-20, a bound nothing that draws or routes a part relied on.
                        throw ParallaxProblem.invalid("Each pin needs a unique name and a position.");
                    }
                }
            }
        } catch (ParallaxProblem exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ParallaxProblem.invalid("The symbol drawing is not valid JSON.");
        }
    }

    /**
     * The representations a text names, or the first its definition supports; at most one is primary, and an object
     * with none marked opens on the first.
     */
    private List<DefinedObject.Representation> representationsOf(DefinedObject defined) {
        if (defined.representations().isEmpty()) {
            return computations.applicableTo(defined.definition(), defined.parameters()).stream().limit(1)
                    .map(kind -> new DefinedObject.Representation(kind, true, Map.of())).toList();
        }
        List<DefinedObject.Representation> shown = new ArrayList<>();
        boolean primarySeen = false;
        for (DefinedObject.Representation representation : defined.representations()) {
            boolean primary = representation.primary() && !primarySeen;
            primarySeen |= primary;
            shown.add(new DefinedObject.Representation(representation.kind(), primary, representation.settings()));
        }
        return shown;
    }

    /**
     * The relations a text implies: its {@code meta} relation fields, the catalogue components a circuit pins
     * ({@code CONSTRUCTED_FROM}), and the symbols a component is drawn with ({@code USES_SYMBOL}) — the ones its
     * {@code symbol uses} line names, each checked to draw every pin of the component —, and the footprint its
     * {@code footprint} line names, checked to have a pad for every pin number and not indexed.
     */
    private List<MathematicalObject.Relation> relationsOf(MathematicalObject object, DefinedObject defined,
                                                          RelationIndexing indexing) {
        List<MathematicalObject.Relation> relations = new ArrayList<>();
        Set<String> relatedIds = new LinkedHashSet<>();
        for (DefinedObject.Relation relation : defined.relations()) {
            MathematicalObject related = objects.findBySlug(relation.slug()).orElse(null);
            if (related == null) {
                if (indexing == RelationIndexing.NOW) {
                    throw ParallaxProblem.invalid("No object is called '" + relation.slug() + "'.");
                }
                LOGGER.warn("Object '{}' names '{}' as {}, which does not exist; the relation is left out.",
                        object.getSlug(), relation.slug(), relation.kind());
                continue;
            }
            if (related.getId().equals(object.getId())) {
                throw ParallaxProblem.invalid("An object does not relate to itself.");
            }
            if (relation.kind() == DefinedObject.RelationKind.USES_SYMBOL) {
                Optional<String> problem = symbolProblem(object, defined, related);
                if (problem.isPresent()) {
                    if (indexing == RelationIndexing.NOW) {
                        throw ParallaxProblem.invalid(problem.get() + ".");
                    }
                    LOGGER.warn("Object '{}' cannot be drawn with '{}': {}; the relation is left out.",
                            object.getSlug(), related.getSlug(), problem.get());
                    continue;
                }
            }
            if (relation.kind() == DefinedObject.RelationKind.USES_FOOTPRINT) {
                Optional<String> problem = footprintProblem(object, defined, related);
                if (problem.isPresent()) {
                    if (indexing == RelationIndexing.NOW) {
                        throw ParallaxProblem.invalid(problem.get() + ".");
                    }
                    LOGGER.warn("Object '{}' cannot land on '{}': {}.", object.getSlug(), related.getSlug(), problem.get());
                }
                // Checked, not indexed: object_relations admits the kinds its CHECK names, and that list grows only
                // with a schema rebuilt from its first migration. The footprint's users are read from the texts.
                continue;
            }
            if (relatedIds.add(related.getId())) {
                relations.add(new MathematicalObject.Relation(related.getId(), relation.kind().name(), relations.size()));
            }
        }
        if (defined.definition() instanceof ObjectDefinition.Circuit circuit) {
            for (ObjectDefinition.CircuitElement element : circuit.elements()) {
                if (element.type() != ObjectDefinition.CircuitElementType.MODEL || element.component() == null) {
                    continue;
                }
                int separator = element.component().lastIndexOf('@');
                if (separator < 1) {
                    continue;
                }
                objects.findBySlug(element.component().substring(0, separator))
                        .filter(component -> component.getKind() == ObjectKind.COMPONENT)
                        .filter(component -> !component.getId().equals(object.getId()))
                        .filter(component -> relatedIds.add(component.getId()))
                        .ifPresent(component -> relations.add(new MathematicalObject.Relation(
                                component.getId(), DefinedObject.RelationKind.CONSTRUCTED_FROM.name(), relations.size())));
            }
        }
        return relations;
    }

    /**
     * Why a component cannot be drawn with a symbol its {@code symbol uses} line names: it is not a symbol, it is a
     * symbol owned by somebody else, or the component's pins do not land on its ports.
     */
    private Optional<String> symbolProblem(MathematicalObject component, DefinedObject defined, MathematicalObject symbol) {
        if (symbol.getKind() != ObjectKind.SYMBOL) {
            return Optional.of("'" + symbol.getSlug() + "' is not a symbol");
        }
        if (!symbol.isCatalogue() && !symbol.owner().equals(component.owner())) {
            return Optional.of("a component can use a workspace symbol only from its own workspace");
        }
        if (!(defined.definition() instanceof ObjectDefinition.Component written)) {
            return Optional.of("only a component is drawn with a symbol");
        }
        return componentModels.bindingProblem(written.source(), symbol);
    }

    /**
     * Why a component's package cannot land on the footprint its {@code footprint} line names: it is not a footprint, it
     * is somebody else's, or a pin number of the component is no pad of it.
     */
    private Optional<String> footprintProblem(MathematicalObject component, DefinedObject defined,
                                              MathematicalObject footprint) {
        if (footprint.getKind() != ObjectKind.FOOTPRINT) {
            return Optional.of("'" + footprint.getSlug() + "' is not a footprint");
        }
        if (!footprint.isCatalogue() && !footprint.owner().equals(component.owner())) {
            return Optional.of("a component can use a workspace footprint only from its own workspace");
        }
        if (!(defined.definition() instanceof ObjectDefinition.Component written)) {
            return Optional.of("only a component lands on a footprint");
        }
        return componentModels.footprintProblem(written.source(), footprint);
    }

    private static DefinedObject.RelationKind relationKind(String kind) {
        try {
            return DefinedObject.RelationKind.valueOf(kind);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw ParallaxProblem.invalid("'" + kind + "' is not a relation.");
        }
    }

    private static Set<String> normalizedTags(Collection<String> tags) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String candidate = tag == null ? "" : tag.strip().toLowerCase();
            if (!ParallaxIdentifiers.isSlug(candidate) || candidate.length() > 64) {
                throw ParallaxProblem.invalid("A tag is lowercase words joined by hyphens: '" + tag + "' is not.");
            }
            normalized.add(candidate);
        }
        return normalized;
    }

    /** The section a document names, or else its kind's home when the host has that section, or else {@code GENERAL}. */
    private String categoryOf(DefinedObject defined) {
        if (defined.category() != null && !defined.category().isBlank()) {
            return defined.category();
        }
        if (defined.definition() == null) {
            return GENERAL_CATEGORY;
        }
        return homeCategories.homeOf(defined.definition().objectKind()).filter(categories::exists).orElse(GENERAL_CATEGORY);
    }

    private static void requireTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > 160) {
            throw ParallaxProblem.invalid("A title of 1–160 characters is required.");
        }
    }

    private String uniqueSlug(ObjectDraft draft) {
        String base = draft.slug() != null && !draft.slug().isBlank() ? draft.slug() : ParallaxIdentifiers.slugOf(
                draft.title() == null ? "object" : draft.title());
        if (!ParallaxIdentifiers.isSlug(base)) {
            throw ParallaxProblem.invalid("A slug is lowercase words joined by hyphens.");
        }
        String candidate = base;
        for (int suffix = 2; objects.existsBySlug(candidate); suffix++) {
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    // ── Internals: views ─────────────────────────────────────────────────────────────────────────────────────────

    ObjectSummary summary(MathematicalObject object) {
        DefinedObject defined;
        try {
            defined = documentOf(object);
        } catch (ParallaxProblem.Invalid unreadable) {
            // A text the language has moved on from since it was saved is still the object: listed from its index, with
            // the reason, so it can be opened and mended. Read whole, one such text refused every list it was in.
            return new ObjectSummary(object.getId(), object.getSlug(), object.getTitle(), object.getSummary(), object.getKind(),
                    object.getKind().nature(), object.getCategoryCode(), 0, null, null, object.isFeatured(), object.getStatus(),
                    CatalogueViews.Origin.of(object), Set.copyOf(object.getTags()), null, object.getRevision(),
                    object.getUpdatedAt(), unreadable.getMessage());
        }
        List<DefinedObject.Representation> representations = representationsOf(defined);
        RepresentationKind primary = representations.stream().filter(DefinedObject.Representation::primary).findFirst()
                .or(() -> representations.stream().findFirst()).map(DefinedObject.Representation::kind).orElse(null);
        return new ObjectSummary(object.getId(), object.getSlug(), object.getTitle(), object.getSummary(), object.getKind(),
                object.getKind().nature(), object.getCategoryCode(), defined.definition().ambientDimension(),
                defined.formulaLatex(), notations.describe(defined.definition(), defined.formulaLatex()),
                object.isFeatured(), object.getStatus(), CatalogueViews.Origin.of(object), Set.copyOf(object.getTags()), primary,
                object.getRevision(), object.getUpdatedAt(), null);
    }

    private ObjectDetail detail(MathematicalObject object) {
        DefinedObject defined = documentOf(object);
        Map<String, MathematicalObject> related = objects.findByIds(object.getRelations().stream()
                        .map(MathematicalObject.Relation::getRelatedObjectId).toList()).stream()
                .collect(Collectors.toMap(MathematicalObject::getId, Function.identity()));
        List<RelatedView> relatedViews = object.getRelations().stream()
                .filter(relation -> related.containsKey(relation.getRelatedObjectId())
                        && objectAccess.canRead(related.get(relation.getRelatedObjectId())))
                .map(relation -> {
                    MathematicalObject other = related.get(relation.getRelatedObjectId());
                    return new RelatedView(other.getSlug(), other.getTitle(), other.getKind(), relation.getKind());
                }).toList();
        List<PresetView> presetViews = defined.presets().stream()
                .map(preset -> new PresetView(ParallaxIdentifiers.slugOf(preset.name()), preset.name(), preset.description(),
                        preset.values(), preset.camera()))
                .toList();
        String source = sourceOf(object);
        return new ObjectDetail(summary(object), defined.description(), defined.definition(), object.getRevision(),
                defined.parameters(),
                defined.properties().stream().map(property -> new PropertyView(property.key(), property.label(),
                        property.value())).toList(),
                representationsOf(defined).stream().map(representation -> new RepresentationView(representation.kind(),
                        representation.primary(), representation.settings())).toList(),
                computations.applicableTo(defined.definition(), defined.parameters()), presetViews, relatedViews,
                CatalogueViews.ownerOf(object),
                object.getVersion(), source, object.getRevision(), object.isShippedUpdatePending(),
                ExperienceDefinitions.explain(source, defined), objectAccess.canWrite(object));
    }

    static String describe(FormulaException exception) {
        return exception.position() >= 0 ? exception.getMessage() + " (at character " + (exception.position() + 1) + ")"
                : exception.getMessage();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
