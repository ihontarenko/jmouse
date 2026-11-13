package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.component.BenchRole;
import org.jmouse.cad.drawing.ArrangedDrawing;
import org.jmouse.cad.drawing.Holding;
import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.cad.store.CadRead;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.document.DocumentEditing;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.folder.StoredPage;
import org.jmouse.cad.folder.RevisionOrigin;
import org.jmouse.cad.folder.DocumentRevision;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.simulation.CategoryDocument;
import org.jmouse.cad.library.DocumentIndex;
import org.jmouse.cad.simulation.LayoutAgreement;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.elaboration.SheetInstance;
import org.jmouse.cad.simulation.elaboration.SheetUnfolding;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.cad.store.catalogue.CatalogueViews.CategoryDraft;
import org.jmouse.cad.store.catalogue.CatalogueViews.CategoryView;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentDetail;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentSummary;
import org.jmouse.cad.store.catalogue.CatalogueViews.LocationView;
import org.jmouse.cad.store.catalogue.CatalogueViews.RelatedView;
import org.jmouse.cad.store.catalogue.CatalogueViews.RevisionSource;
import org.jmouse.cad.store.catalogue.CatalogueViews.RevisionView;
import org.jmouse.cad.store.catalogue.CatalogueViews.SheetBlock;
import org.jmouse.cad.store.catalogue.CatalogueViews.SheetView;
import org.jmouse.cad.store.library.ShippedDocuments;
import org.jmouse.cad.store.plugin.EngineConfiguration;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.revisions.RevisionLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The catalogue of jM documents: browsing it, a document's page, and writing documents — into the shared catalogue by
 * its curators, or for an owner (a project) by whoever may write there.
 *
 * <p>A document is its text. Writing keeps a new revision of that text and derives the index from it
 * ({@link DocumentIndex}); every write is checked as a whole first: the text reads and its model reads, its slug is
 * free, it is filed in a category the catalogue holds, everything it names exists and is of the kind the naming needs, and a circuit's drawing
 * agrees with its netlist. An unchanged text writes nothing.</p>
 */
public class CadCatalogue {

    /** The meta fields the index keeps of its own, and so leaves out of a detail's {@code meta}. */
    private static final Set<String> INDEXED_META = Set.of(MetaNode.SUMMARY, MetaNode.CATEGORY, MetaNode.TAGS);

    /** The longest summary: the one sentence a list shows under a title. */
    private static final int SUMMARY_LENGTH = 500;
    /** The longest title: what a list, a tab and a sheet's frame show whole. */
    private static final int TITLE_LENGTH   = 160;
    /** The longest address: a document's file is named by it, and a file's name is bounded. */
    private static final int SLUG_LENGTH    = 120;

    private static final Logger LOGGER = LoggerFactory.getLogger(CadCatalogue.class);

    /** The kinds no document of is filed in a category by default: the categories themselves, and projects. */
    private static final Set<DocumentKind> OUTSIDE_CATEGORIES = Set.of(SimulationModule.CATEGORY, SimulationModule.PROJECT);

    private final DocumentStore   documents;
    private final DocumentAccess  access;
    private final DocumentReading reading;
    private final CadAuthority    authority;
    private final CadPlaces       places;
    private final EngineConfiguration configuration;
    private final Clock           clock;
    private final DocumentFiles   files;
    private final DocumentTexts   texts;
    /** What writes a text's head anew: a status said, a document filed elsewhere. */
    private final DocumentEditing editing;
    private final Supplier<List<PlaceSource>> placeSources;


    /** @param placeSources the places documents are held besides the library folder, as the plugins brought them, read on each asking */
    public CadCatalogue(DocumentStore documents, DocumentAccess access, DocumentReading reading,
                        CadAuthority authority, CadPlaces places, EngineConfiguration configuration, Clock clock, DocumentFiles files,
                        DocumentTexts texts, Supplier<List<PlaceSource>> placeSources) {
        this.documents = documents;
        this.access = access;
        this.reading = reading;
        this.authority = authority;
        this.places = places;
        this.configuration = configuration;
        this.clock = clock;
        this.files = files;
        this.texts = texts;
        this.editing = new DocumentEditing(reading.language().language(), reading.language().modules());
        this.placeSources = placeSources;
    }

    // ── Categories ───────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * The categories the reader may see — the catalogue's published {@code category} documents, and those of the places
     * the reader reads, such as the category of a KiCad library an import brought into a workspace —, each filed under
     * its parent as every document files itself in its category, parents before their children, each counting the
     * published catalogue documents filed in it itself; of one kind when {@code kind} names one. A category standing
     * under one the reader cannot see is left out with what stands under it.
     */
    @CadRead
    public List<CategoryView> categories(String kind) {
        Map<String, Long> counted = documents.countCatalogueByCategory(DocumentStatus.PUBLISHED, blankToNull(kind));
        Map<String, CategoryDocument> seen = new LinkedHashMap<>();
        Map<String, DocumentOwner> owners = new HashMap<>();
        for (StoredDocument document : documents.findByKind(SimulationModule.CATEGORY.word())) {
            if (document.getStatus() == DocumentStatus.PUBLISHED && access.canRead(document)) {
                seen.put(document.getSlug(), categoryOf(document));
                document.owner().ifPresent(owner -> owners.put(document.getSlug(), owner));
            }
        }
        List<CategoryDocument> tree = seen.values().stream()
                .filter(category -> CategoryDocument.ringOrOrphan(category, seen::get).isEmpty()).toList();
        return CategoryDocument.parentsFirst(tree).stream()
                .map(category -> categoryView(category, counted.getOrDefault(category.slug(), 0L), owners.get(category.slug())))
                .toList();
    }

    /**
     * Writes a category's document: a new one is created and published, one that is there takes the next revision. Its
     * address never changes — every document filed in it names it —; moving it under another parent rewrites its own
     * document alone. An order left out keeps the one it has, or puts a new category last among its siblings.
     */
    @Transactional
    public CategoryView saveCategory(String code, CategoryDraft draft) {
        authority.requireGlobally(CadCapability.CATALOGUE_WRITE);
        Optional<StoredDocument> held = documents.findBySlug(code);
        if (held.isPresent() && !isCategory(held.get())) {
            throw CadProblem.conflict("'" + code + "' is a " + held.get().getKind() + ", not a category.");
        }
        String parent = blankToNull(draft.parent());
        Integer order = draft.order() != null ? draft.order()
                : held.map(found -> categoryOf(found).order()).orElseGet(() -> nextOrderUnder(parent));
        CategoryDocument category = new CategoryDocument(code, blankToNull(draft.title()), blankToNull(draft.summary()), parent, order);
        String source = reading.language().language().write(category.document());
        if (held.isPresent()) {
            update(held.get().getId(), source, held.get().getVersion());
        } else {
            changeStatus(createInCatalogue(source).summary().id(), DocumentStatus.PUBLISHED.name());
        }
        return categoryView(category, documents.countCatalogueByCategory(DocumentStatus.PUBLISHED, null)
                .getOrDefault(code, 0L), null);
    }

    /**
     * Takes a category out of the catalogue as any catalogue document is taken out: archived once published, removed while
     * a draft. One that still files a document or a category is refused, so nothing loses its place.
     */
    @Transactional
    public void deleteCategory(String code) {
        authority.requireGlobally(CadCapability.CATALOGUE_WRITE);
        StoredDocument category = documents.findBySlug(code).filter(CadCatalogue::isCategory)
                .orElseThrow(() -> CadProblem.notFound("The category"));
        if (documents.anyInCategory(code)) {
            throw CadProblem.conflict("Documents or categories are filed in '" + code + "'; file them elsewhere first.");
        }
        if (category.getStatus() == DocumentStatus.PUBLISHED) {
            changeStatus(category.getId(), DocumentStatus.ARCHIVED.name());
        } else {
            delete(category.getId());
        }
    }

    private List<CategoryDocument> catalogueCategories() {
        return documents.findByKind(SimulationModule.CATEGORY.word()).stream()
                .filter(document -> document.isCatalogue() && document.getStatus() == DocumentStatus.PUBLISHED)
                .map(this::categoryOf).toList();
    }

    /** A category as its head revision says it. */
    private CategoryDocument categoryOf(StoredDocument document) {
        return CategoryDocument.of(scope(document.owner().orElse(null)).head(document).document());
    }

    /** The order after the last of a parent's categories in the catalogue; the first, under one that has none. */
    private int nextOrderUnder(String parent) {
        return catalogueCategories().stream().filter(category -> Objects.equals(category.parent(), parent))
                .map(CategoryDocument::order).filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(-1) + 1;
    }

    private static boolean isCategory(StoredDocument document) {
        return SimulationModule.CATEGORY.word().equals(document.getKind());
    }

    /** @param owner whose the category is; null for the catalogue's */
    private static CategoryView categoryView(CategoryDocument category, long documentCount, DocumentOwner owner) {
        return new CategoryView(category.slug(), category.title(), category.summary(), category.parent(), category.order(),
                                documentCount, owner);
    }

    /**
     * What is wrong with where a text files itself, if anything: in what is no category the catalogue holds — archived,
     * or another kind —, or, for a category, under itself through its parents.
     */
    private Optional<String> misfiled(ReadDocument read) {
        String category = read.index().category();
        if (category == null) {
            return Optional.empty();
        }
        boolean held = documents.findBySlug(category)
                .filter(found -> isCategory(found) && found.getStatus() != DocumentStatus.ARCHIVED).isPresent();
        if (!held) {
            return Optional.of("'" + category + "' is not a category.");
        }
        if (!read.document().kind().equals(SimulationModule.CATEGORY)) {
            return Optional.empty();
        }
        CategoryDocument written = CategoryDocument.of(read.document());
        return CategoryDocument.ringOrOrphan(written, slug -> slug.equals(written.slug()) ? written
                : documents.findBySlug(slug).filter(CadCatalogue::isCategory).map(this::categoryOf).orElse(null));
    }

    // ── Reading ──────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * The catalogue listing: published documents — drafts too, for a curator who asks —, narrowed by every filter that
     * is given, featured first and then by title.
     */
    @CadRead
    public StoredPage<DocumentSummary> browse(String text, String category, String kind, String tag, Boolean featured,
                                              boolean includeDrafts, int page, int size) {
        return browse(text, category, kind, tag, null, featured, includeDrafts, page, size);
    }

    /**
     * The same, narrowed to what is something to a bench: a role's word, or {@code any} for every bench device; null for all.
     */
    @CadRead
    public StoredPage<DocumentSummary> browse(String text, String category, String kind, String tag, String benchRole, Boolean featured,
                                              boolean includeDrafts, int page, int size) {
        requirePage(page, size);
        Set<DocumentStatus> statuses = includeDrafts && authority.permitsGlobally(CadCapability.CATALOGUE_WRITE)
                ? EnumSet.of(DocumentStatus.PUBLISHED, DocumentStatus.DRAFT)
                : EnumSet.of(DocumentStatus.PUBLISHED);
        String pattern = text == null || text.isBlank() ? null : "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
        // A category means what is filed in it and in every category under it.
        Set<String> categories = blankToNull(category) == null ? null : CategoryDocument.within(category.strip(), catalogueCategories());
        return documents.browse(statuses, categories, blankToNull(kind), featured, pattern,
                                blankToNull(tag), blankToNull(benchRole), page, size).map(DocumentSummary::of);
    }

    /** One owner's documents, most recently changed first; of one kind when {@code kind} names one. */
    @CadRead
    public StoredPage<DocumentSummary> owned(DocumentOwner owner, String kind, int page, int size) {
        requirePage(page, size);
        authority.require(CadCapability.OWNER_READ, places.requireOwner(owner));
        if (kind != null && !kind.isBlank()) {
            List<DocumentSummary> rows = documents.owned(owner, kind.strip()).stream().map(DocumentSummary::of).toList();
            int from = Math.min(page * size, rows.size());
            return new StoredPage<>(rows.subList(from, Math.min(from + size, rows.size())), page, size, rows.size());
        }
        return documents.owned(owner, page, size).map(DocumentSummary::of);
    }

    /** Readable documents by slug, in the order asked; a slug the reader cannot see is left out. */
    @CadRead
    public List<DocumentSummary> summaries(List<String> slugs) {
        Map<String, StoredDocument> found = documents.findBySlugs(slugs).stream()
                .collect(Collectors.toMap(StoredDocument::getSlug, Function.identity()));
        return slugs.stream().map(found::get).filter(document -> document != null && access.canRead(document))
                .map(DocumentSummary::of).toList();
    }

    /** A document's page: its head, or the revision {@code address@revision} pins. */
    @CadRead
    public DocumentDetail detail(String reference) {
        String address = DocumentIndex.slugOf(reference);
        StoredDocument document = access.requireReadable(address);
        if (address.length() == reference.length()) {
            return detail(document);
        }
        String written = reference.substring(address.length() + DocumentIndex.REVISION_MARK.length());
        int revision = texts.revisionWritten(document, written).orElseThrow(() -> CadProblem.notFound("The revision"));
        String source = texts.at(document, revision).orElseThrow(() -> CadProblem.notFound("The revision"));
        return detail(document, source, revision);
    }

    /**
     * One placed sheet of a readable circuit as it runs inside it — the block a path of references reaches, outermost
     * first —, read with the components and sheets of the circuit's own owner.
     */
    @CadRead
    public SheetInstance placedSheet(String address, List<String> path) {
        StoredDocument document = access.requireReadable(address);
        StoredDocuments scope = scope(document.owner().orElse(null));
        if (!(scope.head(document).model().orElse(null) instanceof Circuit circuit)) {
            throw CadProblem.invalid("Only a circuit places sheets.");
        }
        try {
            return SheetInstance.of(circuit, SheetUnfolding.valuesOf(circuit), scope, path == null ? List.of() : path);
        } catch (IllegalArgumentException refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
    }

    /**
     * The parts a circuit's blocks become, however deep, named as a bench runs them. The blocks are said by the reader — a
     * sheet being drawn wires them its own way before it is saved —; the sheets they place are read as stored, with the
     * components and sheets of the circuit's own owner.
     */
    @CadRead
    public List<Part> unfoldedBlocks(String address, List<Part> blocks) {
        StoredDocument document = access.requireReadable(address);
        StoredDocuments scope = scope(document.owner().orElse(null));
        if (!(scope.head(document).model().orElse(null) instanceof Circuit circuit)) {
            throw CadProblem.invalid("Only a circuit places sheets.");
        }
        Circuit placing = new Circuit(circuit.head(), circuit.parameters(), blocks, circuit.layout(), circuit.probes(),
                circuit.timeBase(), circuit.expectations(), circuit.analysis(), circuit.views(), circuit.frame(), circuit.boundary(), circuit.groups());
        try {
            return SheetUnfolding.of(placing, scope).parts();
        } catch (IllegalArgumentException refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
    }

    /** Every revision of a readable document's text, newest first. */
    @CadRead
    public List<RevisionView> revisions(String address) {
        StoredDocument document = access.requireReadable(address);
        return texts.revisions(document).stream().map(CadCatalogue::revisionView).toList();
    }

    /** One revision's text, named as after an {@code @}: its number, its label, a day — its last — or {@code latest}. */
    @CadRead
    public RevisionSource revision(String address, String written) {
        StoredDocument document = access.requireReadable(address);
        int revision = texts.revisionWritten(document, written).orElseThrow(() -> CadProblem.notFound("The revision"));
        String label = texts.revisions(document).stream()
                .filter(candidate -> candidate.revision() == revision).findFirst()
                .map(candidate -> candidate.label().token())
                .orElseThrow(() -> CadProblem.notFound("The revision"));
        String source = texts.at(document, revision).orElseThrow(() -> CadProblem.notFound("The revision"));
        return new RevisionSource(revision, label, source);
    }

    /** The readable documents whose text names this one — by one relation when {@code relation} names it. */
    @CadRead
    public List<DocumentSummary> usedBy(String address, String relation) {
        StoredDocument document = access.requireReadable(address);
        return documents.findRelating(document.getId(), blankToNull(relation)).stream()
                .filter(access::canRead).map(DocumentSummary::of).toList();
    }

    /**
     * The circuits one owner holds as the sheets of one design: each with its ports, its shared nets and the blocks it
     * places the others with. A part counts as a block only when it names another circuit of the same owner.
     */
    @CadRead
    public List<SheetView> sheets(DocumentOwner owner) {
        authority.require(CadCapability.OWNER_READ, places.requireOwner(owner));
        return sheetsOf(documents.owned(owner, SimulationModule.CIRCUIT.word()), owner);
    }

    /**
     * Circuits that make one design, as its sheets: each read against the place they are held in — an owner's, or the shared
     * catalogue's for circuits a library ships —; a part counts as a block only when it names another of them.
     *
     * @param owner the place they are read in; null for the shared catalogue
     */
    public List<SheetView> sheetsOf(List<StoredDocument> circuits, DocumentOwner owner) {
        Set<String> slugs = circuits.stream().map(StoredDocument::getSlug).collect(Collectors.toSet());
        StoredDocuments scope = scope(owner);
        return circuits.stream().map(circuit -> sheet(circuit, slugs, scope)).toList();
    }

    private SheetView sheet(StoredDocument document, Set<String> sheets, StoredDocuments scope) {
        Circuit circuit;
        try {
            circuit = scope.head(document).model().filter(Circuit.class::isInstance).map(Circuit.class::cast)
                    .orElseThrow(() -> CadProblem.invalid("Its text is no longer a circuit."));
        } catch (CadProblem unreadable) {
            return new SheetView(document.getId(), document.getSlug(), document.getTitle(), document.getCategoryCode(), document.getUpdatedAt(),
                                 List.of(), List.of(), List.of(), unreadable.getMessage());
        }
        List<SheetBlock> blocks = circuit.parts().stream()
                .filter(part -> sheets.contains(DocumentIndex.slugOf(part.component())))
                .map(part -> new SheetBlock(part.reference(), DocumentIndex.slugOf(part.component()))).toList();
        SheetBoundary boundary = circuit.boundary();
        return new SheetView(document.getId(), document.getSlug(), document.getTitle(), document.getCategoryCode(), document.getUpdatedAt(),
                             boundary.ports().stream().map(SheetBoundary.Port::net).toList(),
                             boundary.nets().stream().map(SheetBoundary.Net::name).toList(), blocks,
                             document.refusal().orElse(null));
    }

    /**
     * The catalogue the engine reads a document of this owner against: the shared catalogue's, and every owner of the
     * owner's place — a project sees its own and those of the places it stands in. Null for the shared catalogue's alone.
     */
    public StoredDocuments scope(DocumentOwner owner) {
        CadPlace place = owner == null ? CadPlace.installation() : places.requireOwner(owner);
        return new StoredDocuments(documents, reading, place, files, texts, () -> place.isInstallation() ? List.of()
                : placeSources.get().stream().map(source -> source.holding(place)).toList());
    }

    // ── Writing ──────────────────────────────────────────────────────────────────────────────────────────────────

    /** A new document of the shared catalogue, a draft until it is published: its text says so. */
    @Transactional
    public DocumentDetail createInCatalogue(String source) {
        authority.requireGlobally(CadCapability.CATALOGUE_WRITE);
        String actor = authority.requireActor();
        ReadDocument written = readNew(source);
        ReadDocument read = asNew(null, written, written.index().status() == DocumentStatus.PUBLISHED ? DocumentStatus.DRAFT : null);
        StoredDocument document = new StoredDocument(read.index().slug(), null, actor, clock.instant());
        return detail(writeRevision(document, read, actor, RevisionOrigin.WRITTEN, Strictness.REFUSE));
    }

    /** A new document of an owner's — published at once: an owner's document has no curator to wait for. */
    @Transactional
    public DocumentDetail createOwned(DocumentOwner owner, String source) {
        return createOwned(owner, source, RevisionOrigin.WRITTEN);
    }

    /** The same, its first revision saying where it came from: a person's, or an import's. */
    @Transactional
    public DocumentDetail createOwned(DocumentOwner owner, String source, RevisionOrigin origin) {
        authority.require(CadCapability.OWNER_WRITE, places.requireOwner(owner));
        places.requireAcceptsNew(owner);
        String actor = authority.requireActor();
        ReadDocument read = asNew(owner, readNew(source), null);
        places.requireHolds(owner, read.index().kind());
        Instant now = clock.instant();
        StoredDocument written = writeRevision(new StoredDocument(read.index().slug(), owner, actor, now), read, actor, origin,
                                               Strictness.REFUSE);
        places.recordChange(owner, now);
        return detail(written);
    }

    /** Appends a text as the document's next revision; the same text as the head writes nothing. */
    @Transactional
    public DocumentDetail update(String id, String source, long expectedVersion) {
        return update(id, source, expectedVersion, RevisionOrigin.WRITTEN);
    }

    /** The same, the revision saying where it came from: a person's, or an import's. */
    @Transactional
    public DocumentDetail update(String id, String source, long expectedVersion, RevisionOrigin origin) {
        StoredDocument document = access.requireReadable(id);
        access.requireWritable(document);
        if (document.getVersion() != expectedVersion) {
            throw CadProblem.staleVersion("The document");
        }
        ReadDocument read = reading.read(source, document.getSlug());
        if (!read.index().slug().equals(document.getSlug())) {
            throw CadProblem.invalidDocument("A document keeps its address: write 'as " + document.getSlug()
                                             + "' after its title.", read.document().line(), 0);
        }
        if (read.checksum().equals(document.getSourceChecksum())) {
            return detail(document);
        }
        if (origin == RevisionOrigin.WRITTEN) {
            heldRefusal(document, read).ifPresent(refusal -> {
                throw CadProblem.conflict(refusal);
            });
        }
        StoredDocument written = writeRevision(document, read, authority.requireActor(), origin, Strictness.REFUSE);
        document.owner().ifPresent(owner -> places.recordChange(owner, clock.instant()));
        return detail(written);
    }

    /**
     * Why a drawing written anew may not replace the one it revises: it moved or removed what was held where it stands — a locked
     * item, or one of a locked group —, whoever wrote it. Empty for a document that is no drawing of such items. An import writes
     * what its source says, and is not asked.
     */
    private Optional<String> heldRefusal(StoredDocument document, ReadDocument written) {
        if (!(written.model().orElse(null) instanceof ArrangedDrawing after)) {
            return Optional.empty();
        }
        Object before = reading.read(texts.head(document), document.getSlug()).model().orElse(null);
        return before instanceof ArrangedDrawing held ? Holding.refusal(held, after) : Optional.empty();
    }

    /**
     * Moves a catalogue document to a status — its text written anew to say it, as the next revision —; an owner's
     * document has none to move.
     */
    @Transactional
    public DocumentDetail changeStatus(String id, String status) {
        authority.requireGlobally(CadCapability.CATALOGUE_WRITE);
        StoredDocument document = access.requireReadable(id);
        if (!document.isCatalogue()) {
            throw CadProblem.conflict("Only a catalogue document is published or archived.");
        }
        DocumentStatus wanted = statusOf(status);
        if (document.getStatus() == wanted) {
            return detail(document);
        }
        ReadDocument head = scope(null).head(document);
        return update(id, headed(head, new DocumentEditing.Head(null, null, null, wanted)), document.getVersion());
    }

    /**
     * A new text as the catalogue keeps it: one naming no category filed in the installation's category for those
     * ({@link EngineConfiguration#defaultCategory()}) — so every document's file says where it stands, whoever wrote it,
     * a person or an import —, and its status written anew when one is given. A kind that stands outside the categories is
     * never filed so: a category naming no parent stands at the top, and a project stands where its keeper does. Nor is a
     * project's document: the project is its place.
     *
     * @param owner  whose it is to be; null for the shared catalogue's
     * @param status the status its text is to say; null to leave it as written
     */
    private ReadDocument asNew(DocumentOwner owner, ReadDocument written, DocumentStatus status) {
        String category = written.index().category() == null && !OUTSIDE_CATEGORIES.contains(written.document().kind())
                          && !CadPlaces.isProject(owner)
                ? configuration.defaultCategory().orElse(null) : null;
        if (category == null && status == null) {
            return written;
        }
        return reading.read(headed(written, new DocumentEditing.Head(null, null, category, status)), "the new document");
    }

    /** A text with its head written anew. */
    private String headed(ReadDocument read, DocumentEditing.Head head) {
        return editing.reheaded(read.document(), head);
    }

    /**
     * Removes a document. A published catalogue document is archived instead — links to it are out there —, and one
     * that another document still names is refused, so nothing stops reading.
     */
    @Transactional
    public void delete(String id) {
        StoredDocument document = access.requireReadable(id);
        access.requireWritable(document);
        if (document.isCatalogue() && document.getStatus() == DocumentStatus.PUBLISHED) {
            throw CadProblem.conflict("A published catalogue document is archived, not deleted: links to it are out there.");
        }
        List<String> naming = documents.findRelating(document.getId(), null).stream().map(StoredDocument::getSlug).toList();
        if (!naming.isEmpty()) {
            throw CadProblem.conflict("'" + document.getSlug() + "' is named by " + String.join(", ", naming)
                                      + ": change those first.");
        }
        documents.remove(document, authority.requireActor());
        document.owner().ifPresent(owner -> places.recordChange(owner, clock.instant()));
    }

    // ── What is indexed at start ─────────────────────────────────────────────────────────────────────────────────

    /** What indexing one shipped document did. */
    public enum ShippedOutcome {
        /** The document is indexed. */
        CREATED,
        /** A document of the library folder holds the address; it is left alone and nothing is indexed. */
        ADDRESS_TAKEN
    }

    /**
     * Indexes one document a library ships, without a caller — never copying its text, which stays in its file (ADR
     * 0001) —, as the index is made on every start; its relations afterwards, by {@link #indexRelations}, once
     * everything they may name exists.
     */
    public ShippedOutcome indexShipped(ShippedDocuments.ShippedDocument shipped) {
        StoredDocument held = documents.findBySlug(shipped.slug()).orElse(null);
        if (held != null && !held.isShipped()) {
            LOGGER.warn("A document of the library folder already holds the address '{}'; the shipped one is not indexed.", shipped.slug());
            return ShippedOutcome.ADDRESS_TAKEN;
        }
        Instant now = clock.instant();
        StoredDocument document = new StoredDocument(shipped.slug(), null, null, now);
        document.holdShipped(shipped.library(), shipped.path());
        RevisionLabel label = RevisionLabel.next(LocalDate.ofInstant(now, configuration.revisionZone()), 0);
        // What its file's name says, until its text is read.
        document.indexRevision(1, label, DocumentStore.checksumOf(shipped.source()), shipped.kind(), shipped.slug(), null, null,
                               null, false, now);
        indexFile(document, shipped.source(), shipped.origin(), 1, label, now);
        return ShippedOutcome.CREATED;
    }

    /**
     * Indexes one document of the library folder, as the folder opened it: its head text read from its file, its
     * revision and its time its last commit's; its relations afterwards, by {@link #indexRelations}.
     */
    public void indexHeld(StoredDocument held) {
        DocumentRevision head = documents.headOf(held);
        indexFile(held, documents.heldText(held), held.getSlug(), head.revision(), head.label(), head.writtenAt());
    }

    /**
     * Indexes a document from the text its file holds. The file is the truth: a text the catalogue would refuse to write
     * — one put in the folder by hand, one a library ships wrong — is indexed all the same, as far as it reads, and says
     * why it is refused ({@link StoredDocument#refusal()}); it never stops the catalogue being made.
     */
    private void indexFile(StoredDocument document, String source, String origin, int revision, RevisionLabel label, Instant at) {
        ReadDocument read;
        try {
            read = reading.read(source, origin);
        } catch (CadProblem unreadable) {
            // What its place says stands; a text that does not read says no status, so the catalogue's is a draft until
            // it reads — an owner's is published, as every owner's is.
            refuse(document, unreadable);
            document.changeStatus(document.isCatalogue() ? DocumentStatus.DRAFT : DocumentStatus.PUBLISHED, at);
            documents.save(document);
            return;
        }
        try {
            validate(document, read);
            document.judge(Optional.empty());
        } catch (CadProblem refused) {
            refuse(document, refused);
        }
        index(document, read, revision, label, at, Strictness.LATER);
    }

    private static void refuse(StoredDocument document, CadProblem refusal) {
        LOGGER.warn("The document '{}' is indexed as its file holds it, refused: {}", document.getSlug(), refusal.getMessage());
        document.judge(Optional.of(refusal.getMessage()));
    }

    /**
     * Derives a document's relations from its text, once everything it may name is indexed. A reference to something
     * the catalogue does not hold is left out and logged, never fatal: the application must start. So is an owner the
     * host does not know — a folder of an account or a project it has no row for —: the document is indexed as its
     * file holds it, refused with the reason, and relates to nothing until its owner exists.
     */
    public void indexRelations(String slug) {
        documents.findBySlug(slug).ifPresent(document -> {
            // Its owner unknown, or its text one the language refuses: indexed as its file holds it, refused with the reason.
            ReadDocument head;
            StoredDocuments scope;
            try {
                scope = scope(document.owner().orElse(null));
                head = scope.head(document);
            } catch (CadProblem refusal) {
                refuse(document, refusal);
                return;
            }
            document.replaceRelations(relationsOf(document, head, Strictness.LEAVE_OUT));
        });
    }

    // ── One revision ─────────────────────────────────────────────────────────────────────────────────────────────

    /** How a write treats a reference to something it cannot find. */
    private enum Strictness {
        /** Refuse the write. */
        REFUSE,
        /** Leave the reference out, and say so in the log. */
        LEAVE_OUT,
        /** Index nothing now: a later pass does, once a whole batch exists. */
        LATER
    }

    private ReadDocument readNew(String source) {
        ReadDocument read = reading.read(source, "the new document");
        if (documents.existsBySlug(read.index().slug())) {
            throw CadProblem.conflict("A document is already called '" + read.index().slug()
                                      + "'; write another address after 'as'.");
        }
        return read;
    }

    /**
     * Checks a text as a whole, keeps it as the document's next revision and derives the index from it.
     *
     * @return the stored document, now managed
     */
    private StoredDocument writeRevision(StoredDocument document, ReadDocument read, String writtenBy, RevisionOrigin origin,
                                         Strictness strictness) {
        validate(document, read);
        document.judge(Optional.empty());
        Instant now = clock.instant();
        int revision = document.getRevision() + 1;
        RevisionLabel label = documents.nextLabel(document.getId(), LocalDate.ofInstant(now, configuration.revisionZone()));
        StoredDocument stored = index(document, read, revision, label, now, strictness);
        documents.appendSource(stored.getId(), revision, label, read.source(), writtenBy, origin, now);
        return stored;
    }

    /**
     * Takes the document's index from a text at a revision — what a written revision and a file are both indexed by —,
     * the text checked before: refused for a write, judged for a file.
     *
     * @return the document, indexed
     */
    private StoredDocument index(StoredDocument document, ReadDocument read, int revision, RevisionLabel label, Instant now,
                                 Strictness strictness) {
        DocumentIndex index = read.index();
        // Where a catalogue document stands is what its text says; an owner's is published, having no curator to wait for.
        document.changeStatus(document.isCatalogue() ? index.status() : DocumentStatus.PUBLISHED, now);
        document.indexRevision(revision, label, read.checksum(), index.kind(), index.title().strip(), index.code(), index.summary(),
                               index.category(), index.featured(), now);
        document.replaceTags(tagsOf(index));
        document.changeBenchRole(index.benchRole().map(BenchRole::word).orElse(null));
        if (strictness != Strictness.LATER) {
            document.replaceRelations(relationsOf(document, read, strictness));
        }
        return documents.save(document);
    }

    private void validate(StoredDocument document, ReadDocument read) {
        DocumentIndex index = read.index();
        if (index.slug().length() > SLUG_LENGTH) {
            throw CadProblem.invalidDocument("An address is at most " + SLUG_LENGTH + " characters.", read.document().line(), 0);
        }
        if (index.title() == null || index.title().isBlank() || index.title().length() > TITLE_LENGTH) {
            throw CadProblem.invalidDocument("A title is 1–" + TITLE_LENGTH + " characters.", read.document().line(), 0);
        }
        if (index.summary() != null && index.summary().length() > SUMMARY_LENGTH) {
            throw CadProblem.invalid("A summary is at most " + SUMMARY_LENGTH + " characters.");
        }
        misfiled(read).ifPresent(refusal -> {
            throw CadProblem.invalid(refusal);
        });
        if (read.model().orElse(null) instanceof Circuit circuit) {
            List<String> contradictions = LayoutAgreement.contradictions(circuit, scope(document.owner().orElse(null)));
            if (!contradictions.isEmpty()) {
                throw CadProblem.layoutDisagrees(contradictions);
            }
        }
    }

    private static Set<String> tagsOf(DocumentIndex index) {
        Set<String> tags = new LinkedHashSet<>();
        for (String tag : index.tags()) {
            tags.add(tag.toLowerCase(Locale.ROOT));
        }
        return tags;
    }

    /**
     * The relations a text implies, each to a document the owner may place and of the kind the naming needs. One that
     * names nothing is refused, or left out and logged, as {@code strictness} says.
     */
    private List<StoredDocument.Relation> relationsOf(StoredDocument document, ReadDocument read, Strictness strictness) {
        StoredDocuments scope = scope(document.owner().orElse(null));
        List<StoredDocument.Relation> relations = new ArrayList<>();
        for (DocumentIndex.Reference reference : read.index().references()) {
            // A reference names a document by its slug or by its code, found as the scope finds every name.
            Optional<StoredDocument> related = scope.addressed(reference.slug());
            String refusal = null;
            if (related.isEmpty()) {
                refusal = "it names '" + reference.slug() + "' (" + reference.relation() + "), which the catalogue does not hold";
            } else if (!DocumentIndex.names(reference.relation(), related.get().getKind())) {
                refusal = "it names '" + reference.slug() + "' as its " + reference.relation() + ", which is a "
                        + related.get().getKind();
            }
            if (refusal != null) {
                if (strictness == Strictness.REFUSE) {
                    throw CadProblem.invalidDocument("'" + document.getSlug() + "' does not read: " + refusal + ".",
                                                     read.document().line(), 0);
                }
                LOGGER.warn("Document '{}': {}; the relation is left out.", document.getSlug(), refusal);
                continue;
            }
            relations.add(new StoredDocument.Relation(related.get().getId(), reference.relation(), relations.size()));
        }
        return relations;
    }

    // ── Views ────────────────────────────────────────────────────────────────────────────────────────────────────

    private DocumentDetail detail(StoredDocument document) {
        return detail(document, null, document.getRevision());
    }

    /**
     * @param source the text of the revision asked for; null for the head
     */
    private DocumentDetail detail(StoredDocument document, String source, int revision) {
        StoredDocuments scope = scope(document.owner().orElse(null));
        String text = source != null ? source : scope.headSource(document);
        ReadDocument read = null;
        // The head's problem is what the index found; an earlier revision's, what reading it finds.
        String problem = source == null ? document.refusal().orElse(null) : null;
        try {
            read = source != null ? reading.read(source, document.getSlug()) : scope.head(document);
        } catch (CadProblem unreadable) {
            problem = unreadable.getMessage();
        }
        Map<String, StoredDocument> related = documents.findByIds(document.getRelations().stream()
                        .map(StoredDocument.Relation::relatedDocumentId).toList()).stream()
                .collect(Collectors.toMap(StoredDocument::getId, Function.identity()));
        List<RelatedView> relatedViews = document.getRelations().stream()
                .map(relation -> Optional.ofNullable(related.get(relation.relatedDocumentId()))
                        .filter(access::canRead)
                        .map(found -> new RelatedView(found.getSlug(), found.getTitle(), found.getKind(), relation.kind())))
                .flatMap(Optional::stream).toList();
        Object model = read == null ? null : read.model().orElse(null);
        return new DocumentDetail(DocumentSummary.of(document, problem), text, revision, document.getVersion(),
                                  read == null ? Map.of() : metaOf(read), relatedViews,
                                  model, ModelFacts.slidersOf(model), ModelFacts.presentationOf(model),
                                  access.canWrite(document));
    }

    /**
     * A document held besides the store — a file of a mounted folder —, as a page shows one of the store's: it has no
     * revisions and no stored identity, so its identity is where it is, and its version the source's own word for its text.
     *
     * @param version  what says which text this is: a write of another is refused by the source
     * @param owner    the place it belongs to; null for a folder that belongs to none
     * @param writable whether the place it is held in lets the reader write it
     */
    public DocumentDetail heldDetail(ReadDocument read, DocumentLocation location, String identity, DocumentOwner owner,
                                     Instant changed, long version, boolean writable) {
        DocumentIndex index = read.index();
        Object model = read.model().orElse(null);
        DocumentSummary summary = new DocumentSummary(identity, index.slug(), index.title(), index.summary(), index.kind(),
                index.category(), null, index.featured(), DocumentStatus.PUBLISHED.name(), LocationView.of(location), owner,
                index.tags(), index.benchRole().map(BenchRole::word).orElse(null), 0, null, changed, null);
        return new DocumentDetail(summary, read.source(), 0, version, metaOf(read), List.of(), model, ModelFacts.slidersOf(model),
                                  ModelFacts.presentationOf(model), writable);
    }

    private static Map<String, String> metaOf(ReadDocument read) {
        Map<String, String> meta = new LinkedHashMap<>();
        read.document().block(MetaNode.class).ifPresent(block -> {
            for (MetaLineNode line : block.lines()) {
                if (!INDEXED_META.contains(line.key())) {
                    meta.put(line.key(), line.text());
                }
            }
        });
        return meta;
    }

    /** A document as a list shows it, its problem what the catalogue finds wrong with its head text. */
    private static RevisionView revisionView(DocumentRevision revision) {
        return new RevisionView(revision.revision(), revision.label().token(), revision.checksum(), revision.writtenAt(),
                                revision.writtenBy());
    }

    /** Whether a document is still as an import left it: its head revision the engine's, not a person's. */
    public boolean leftByAnImport(StoredDocument document) {
        return documents.originOf(document.getId(), document.getRevision()).leftByAnImport(document.getRevision());
    }

    // ── Small things ─────────────────────────────────────────────────────────────────────────────────────────────

    private static DocumentStatus statusOf(String status) {
        try {
            return DocumentStatus.written(Optional.ofNullable(status).map(word -> word.toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException unknown) {
            throw CadProblem.invalid(unknown.getMessage() + ".");
        }
    }

    private static void requirePage(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw CadProblem.invalid("A page is from 0, and holds 1–200 rows.");
        }
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
