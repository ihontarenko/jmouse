package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.jpa.StoredPage;
import org.jmouse.cad.jpa.catalogue.CategoryStore;
import org.jmouse.cad.jpa.catalogue.DocumentCategory;
import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.jpa.catalogue.DocumentRevision;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.jpa.catalogue.StoredDocument;
import org.jmouse.cad.library.DocumentIndex;
import org.jmouse.cad.simulation.LayoutAgreement;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.elaboration.SheetInstance;
import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CatalogueViews.CategoryDraft;
import org.jmouse.cad.store.catalogue.CatalogueViews.CategoryView;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentDetail;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentSummary;
import org.jmouse.cad.store.catalogue.CatalogueViews.Origin;
import org.jmouse.cad.store.catalogue.CatalogueViews.RelatedView;
import org.jmouse.cad.store.catalogue.CatalogueViews.RevisionSource;
import org.jmouse.cad.store.catalogue.CatalogueViews.RevisionView;
import org.jmouse.cad.store.catalogue.CatalogueViews.SheetBlock;
import org.jmouse.cad.store.catalogue.CatalogueViews.SheetView;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The catalogue of jM documents: browsing it, a document's page, and writing documents — into the shared catalogue by
 * its curators, or for an owner (a project) by whoever may write there.
 *
 * <p>A document is its text. Writing keeps a new revision of that text and derives the index from it
 * ({@link DocumentIndex}); every write is checked as a whole first: the text reads and its model reads, its slug is
 * free, its category exists, everything it names exists and is of the kind the naming needs, and a circuit's drawing
 * agrees with its netlist. An unchanged text writes nothing.</p>
 */
public class CadCatalogue {

    /** A category's code: lowercase words joined by hyphens, as a document's {@code meta { category … }} writes it. */
    private static final Pattern CATEGORY_CODE = Pattern.compile("[a-z][a-z0-9-]{1,39}");

    /** The meta fields the index keeps as columns, and so leaves out of a detail's {@code meta}. */
    private static final Set<String> INDEXED_META = Set.of(MetaNode.SUMMARY, MetaNode.CATEGORY, MetaNode.TAGS);

    private static final int SUMMARY_LENGTH = 500;
    private static final int TITLE_LENGTH   = 160;
    private static final int SLUG_LENGTH    = 120;
    private static final int TAG_LENGTH     = 64;

    private static final Logger LOGGER = LoggerFactory.getLogger(CadCatalogue.class);

    private final DocumentStore   documents;
    private final CategoryStore   categories;
    private final DocumentAccess  access;
    private final DocumentReading reading;
    private final CadAuthority    authority;
    private final CadPlaces       places;
    private final CadProperties   properties;
    private final Clock           clock;

    public CadCatalogue(DocumentStore documents, CategoryStore categories, DocumentAccess access, DocumentReading reading,
                        CadAuthority authority, CadPlaces places, CadProperties properties, Clock clock) {
        this.documents = documents;
        this.categories = categories;
        this.access = access;
        this.reading = reading;
        this.authority = authority;
        this.places = places;
        this.properties = properties;
        this.clock = clock;
    }

    // ── Categories ───────────────────────────────────────────────────────────────────────────────────────────────

    /** The sections, each counting its published catalogue documents — of one kind when {@code kind} names one. */
    @Transactional(readOnly = true)
    public List<CategoryView> categories(String kind) {
        String counted = blankToNull(kind);
        return categories.allInOrder().stream().map(category -> categoryView(category, counted)).toList();
    }

    /** Creates a section, or renames and moves one; its code never changes, documents point at it. */
    @Transactional
    public CategoryView saveCategory(String code, CategoryDraft draft) {
        authority.requireGlobally(CadCapability.CATALOGUE_WRITE);
        if (code == null || !CATEGORY_CODE.matcher(code).matches()) {
            throw CadProblem.invalid("A category code is 2–40 lowercase letters, digits and hyphens, as a document's"
                                     + " 'category' writes it.");
        }
        if (draft.title() == null || draft.title().isBlank()) {
            throw CadProblem.invalid("Give the category a title.");
        }
        String title = draft.title().strip();
        String description = blankToNull(draft.description());
        int position = draft.position() != null ? draft.position() : nextCategoryPosition();
        DocumentCategory category = categories.find(code).orElse(null);
        if (category == null) {
            category = categories.save(new DocumentCategory(code, title, description, position));
        } else {
            category.describe(title, description, position);
        }
        return categoryView(category, null);
    }

    /** Removes an empty section; one that still files documents is refused, so no document loses its place. */
    @Transactional
    public void deleteCategory(String code) {
        authority.requireGlobally(CadCapability.CATALOGUE_WRITE);
        if (!categories.exists(code)) {
            throw CadProblem.notFound("The category");
        }
        if (documents.anyInCategory(code)) {
            throw CadProblem.conflict("Documents are filed in this category; file them elsewhere first.");
        }
        categories.delete(code);
    }

    /** A section the shipped documents declare, written when it is missing and never rewritten: a curator owns it. */
    @Transactional
    public boolean seedCategory(String code, String title, String description, Integer position) {
        if (categories.exists(code)) {
            return false;
        }
        categories.save(new DocumentCategory(code, title, description, position != null ? position : nextCategoryPosition()));
        categories.flush();
        return true;
    }

    private int nextCategoryPosition() {
        return categories.allInOrder().stream().mapToInt(DocumentCategory::getPosition).max().orElse(0) + 1;
    }

    /** @param kind the kind counted, or null for every kind */
    private CategoryView categoryView(DocumentCategory category, String kind) {
        long count = documents.countCatalogueInCategory(category.getCode(), StoredDocument.Status.PUBLISHED, kind);
        return new CategoryView(category.getCode(), category.getTitle(), category.getDescription(),
                                category.getPosition(), count);
    }

    // ── Reading ──────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * The catalogue listing: published documents — drafts too, for a curator who asks —, narrowed by every filter that
     * is given, featured first and then by title.
     */
    @Transactional(readOnly = true)
    public StoredPage<DocumentSummary> browse(String text, String category, String kind, String tag, Boolean featured,
                                              boolean includeDrafts, int page, int size) {
        requirePage(page, size);
        Set<StoredDocument.Status> statuses = includeDrafts && authority.permitsGlobally(CadCapability.CATALOGUE_WRITE)
                ? EnumSet.of(StoredDocument.Status.PUBLISHED, StoredDocument.Status.DRAFT)
                : EnumSet.of(StoredDocument.Status.PUBLISHED);
        String pattern = text == null || text.isBlank() ? null : "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
        return documents.browse(statuses, blankToNull(category), blankToNull(kind), featured, pattern,
                                blankToNull(tag), page, size).map(this::summary);
    }

    /** One owner's documents, most recently changed first; of one kind when {@code kind} names one. */
    @Transactional(readOnly = true)
    public StoredPage<DocumentSummary> owned(DocumentOwner owner, String kind, int page, int size) {
        requirePage(page, size);
        authority.require(CadCapability.OWNER_READ, places.requireOwner(owner));
        if (kind != null && !kind.isBlank()) {
            List<DocumentSummary> rows = documents.owned(owner, kind.strip()).stream().map(this::summary).toList();
            int from = Math.min(page * size, rows.size());
            return new StoredPage<>(rows.subList(from, Math.min(from + size, rows.size())), page, size, rows.size());
        }
        return documents.owned(owner, page, size).map(this::summary);
    }

    /** Readable documents by slug, in the order asked; a slug the reader cannot see is left out. */
    @Transactional(readOnly = true)
    public List<DocumentSummary> summaries(List<String> slugs) {
        Map<String, StoredDocument> found = documents.findBySlugs(slugs).stream()
                .collect(Collectors.toMap(StoredDocument::getSlug, Function.identity()));
        return slugs.stream().map(found::get).filter(document -> document != null && access.canRead(document))
                .map(this::summary).toList();
    }

    /** A document's page: its head, or the revision {@code address@revision} pins. */
    @Transactional(readOnly = true)
    public DocumentDetail detail(String reference) {
        String address = DocumentIndex.slugOf(reference);
        StoredDocument document = access.requireReadable(address);
        if (address.length() == reference.length()) {
            return detail(document);
        }
        String written = reference.substring(address.length() + DocumentIndex.REVISION_MARK.length());
        int revision = documents.revisionWritten(document.getId(), written)
                .orElseThrow(() -> CadProblem.notFound("The revision"));
        String source = documents.sourceAt(document.getId(), revision)
                .orElseThrow(() -> CadProblem.notFound("The revision"));
        return detail(document, source, revision);
    }

    /**
     * One placed sheet of a readable circuit as it runs inside it — the block a path of references reaches, outermost
     * first —, read with the components and sheets of the circuit's own owner.
     */
    @Transactional(readOnly = true)
    public SheetInstance placedSheet(String address, List<String> path) {
        StoredDocument document = access.requireReadable(address);
        StoredDocuments scope = scope(document.owner().orElse(null));
        if (!(scope.head(document).model().orElse(null) instanceof Circuit circuit)) {
            throw CadProblem.invalid("Only a circuit places sheets.");
        }
        Map<String, Double> values = new LinkedHashMap<>();
        ParameterEvaluation.evaluate(circuit.head().title(), circuit.parameters())
                .forEach(parameter -> values.put(parameter.name(), parameter.value()));
        try {
            return SheetInstance.of(circuit, values, scope, path == null ? List.of() : path);
        } catch (IllegalArgumentException refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
    }

    /** Every revision of a readable document's text, newest first. */
    @Transactional(readOnly = true)
    public List<RevisionView> revisions(String address) {
        StoredDocument document = access.requireReadable(address);
        return documents.revisions(document.getId()).stream().map(CadCatalogue::revisionView).toList();
    }

    /** One revision's text, named as after an {@code @}: its number, its label, a day — its last — or {@code latest}. */
    @Transactional(readOnly = true)
    public RevisionSource revision(String address, String written) {
        StoredDocument document = access.requireReadable(address);
        int revision = documents.revisionWritten(document.getId(), written)
                .orElseThrow(() -> CadProblem.notFound("The revision"));
        String label = documents.revisions(document.getId()).stream()
                .filter(candidate -> candidate.revision() == revision).findFirst()
                .map(candidate -> candidate.label().token())
                .orElseThrow(() -> CadProblem.notFound("The revision"));
        String source = documents.sourceAt(document.getId(), revision)
                .orElseThrow(() -> CadProblem.notFound("The revision"));
        return new RevisionSource(revision, label, source);
    }

    /** The readable documents whose text names this one — by one relation when {@code relation} names it. */
    @Transactional(readOnly = true)
    public List<DocumentSummary> usedBy(String address, String relation) {
        StoredDocument document = access.requireReadable(address);
        return documents.findRelating(document.getId(), blankToNull(relation)).stream()
                .filter(access::canRead).map(this::summary).toList();
    }

    /**
     * The circuits one owner holds as the sheets of one design: each with its ports, its shared nets and the blocks it
     * places the others with. A part counts as a block only when it names another circuit of the same owner.
     */
    @Transactional(readOnly = true)
    public List<SheetView> sheets(DocumentOwner owner) {
        authority.require(CadCapability.OWNER_READ, places.requireOwner(owner));
        List<StoredDocument> circuits = documents.owned(owner, SimulationModule.CIRCUIT.word());
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
            return new SheetView(document.getId(), document.getSlug(), document.getTitle(), document.getUpdatedAt(),
                                 List.of(), List.of(), List.of(), unreadable.getMessage());
        }
        List<SheetBlock> blocks = circuit.parts().stream()
                .filter(part -> sheets.contains(DocumentIndex.slugOf(part.component())))
                .map(part -> new SheetBlock(part.reference(), DocumentIndex.slugOf(part.component()))).toList();
        SheetBoundary boundary = circuit.boundary();
        return new SheetView(document.getId(), document.getSlug(), document.getTitle(), document.getUpdatedAt(),
                             boundary.ports().stream().map(SheetBoundary.Port::net).toList(),
                             boundary.nets().stream().map(SheetBoundary.Net::name).toList(), blocks, null);
    }

    /** The catalogue the engine reads a document of this owner against — null for the shared catalogue's. */
    public StoredDocuments scope(DocumentOwner owner) {
        return new StoredDocuments(documents, reading, owner);
    }

    // ── Writing ──────────────────────────────────────────────────────────────────────────────────────────────────

    /** A new document of the shared catalogue, a draft until it is published. */
    @Transactional
    public DocumentDetail createInCatalogue(String source) {
        authority.requireGlobally(CadCapability.CATALOGUE_WRITE);
        String actor = authority.requireActor();
        ReadDocument read = readNew(source);
        StoredDocument document = new StoredDocument(newIdentifier(), read.index().slug(), null, actor, clock.instant());
        return detail(writeRevision(document, read, actor, Strictness.REFUSE));
    }

    /** A new document of an owner's — published at once: an owner's document has no curator to wait for. */
    @Transactional
    public DocumentDetail createOwned(DocumentOwner owner, String source) {
        authority.require(CadCapability.OWNER_WRITE, places.requireOwner(owner));
        places.requireAcceptsNew(owner);
        String actor = authority.requireActor();
        ReadDocument read = readNew(source);
        places.requireHolds(owner, read.index().kind());
        Instant now = clock.instant();
        StoredDocument document = new StoredDocument(newIdentifier(), read.index().slug(), owner, actor, now);
        document.changeStatus(StoredDocument.Status.PUBLISHED, now);
        StoredDocument written = writeRevision(document, read, actor, Strictness.REFUSE);
        places.recordChange(owner, now);
        return detail(written);
    }

    /** Appends a text as the document's next revision; the same text as the head writes nothing. */
    @Transactional
    public DocumentDetail update(String id, String source, long expectedVersion) {
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
        StoredDocument written = writeRevision(document, read, authority.requireActor(), Strictness.REFUSE);
        document.owner().ifPresent(owner -> places.recordChange(owner, clock.instant()));
        return detail(written);
    }

    /** Moves a catalogue document to a status; an owner's document has none to move. */
    @Transactional
    public DocumentDetail changeStatus(String id, String status) {
        authority.requireGlobally(CadCapability.CATALOGUE_WRITE);
        StoredDocument document = access.requireReadable(id);
        if (!document.isCatalogue()) {
            throw CadProblem.conflict("Only a catalogue document is published or archived.");
        }
        document.changeStatus(statusOf(status), clock.instant());
        return detail(document);
    }

    /**
     * Removes a document. A published catalogue document is archived instead — links to it are out there —, and one
     * that another document still names is refused, so nothing stops reading.
     */
    @Transactional
    public void delete(String id) {
        StoredDocument document = access.requireReadable(id);
        access.requireWritable(document);
        if (document.isCatalogue() && document.getStatus() == StoredDocument.Status.PUBLISHED) {
            throw CadProblem.conflict("A published catalogue document is archived, not deleted: links to it are out there.");
        }
        List<String> naming = documents.findRelating(document.getId(), null).stream().map(StoredDocument::getSlug).toList();
        if (!naming.isEmpty()) {
            throw CadProblem.conflict("'" + document.getSlug() + "' is named by " + String.join(", ", naming)
                                      + ": change those first.");
        }
        documents.remove(document);
        document.owner().ifPresent(owner -> places.recordChange(owner, clock.instant()));
    }

    // ── The shipped library ──────────────────────────────────────────────────────────────────────────────────────

    /** What writing one shipped text did. */
    public enum ShippedOutcome {
        /** The document did not exist and was created from the text. */
        CREATED,
        /** Nobody had edited it, so the newer text became its next revision. */
        REVISED,
        /** The text is the one already taken. */
        UNCHANGED,
        /** A curator edited it since; the edit is kept and the newer text recorded as waiting. */
        KEPT_EDIT,
        /** An owner's own document holds the address; it is left alone and nothing is written. */
        ADDRESS_TAKEN
    }

    /**
     * Whether a shipped text is already the one taken, without reading it: the start-up import's fast path, which
     * reads only what moved.
     */
    @Transactional(readOnly = true)
    public boolean shippedUnchanged(String slug, String checksum) {
        return documents.findBySlug(slug).filter(StoredDocument::isCatalogue)
                .filter(document -> checksum.equals(document.getShippedChecksum()) && !document.isShippedUpdatePending())
                .isPresent();
    }

    /**
     * Writes one document the shipped library holds, without a caller. A new document is created and published; one
     * nobody has edited takes a newer text as its next revision; one a curator has edited keeps the edit. Relations are
     * indexed afterwards, by {@link #indexShippedRelations}, once everything they may name exists.
     *
     * @param folder where its file stands under the library's root
     */
    @Transactional
    public ShippedOutcome writeShipped(String source, String origin, String folder) {
        ReadDocument read = reading.read(source, origin);
        StoredDocument document = documents.findBySlug(read.index().slug()).orElse(null);
        Instant now = clock.instant();
        if (document == null) {
            document = new StoredDocument(newIdentifier(), read.index().slug(), null, null, now);
            document.fileUnder(folder);
            document.changeStatus(StoredDocument.Status.PUBLISHED, now);
            document.recordShippedText(read.checksum());
            writeRevision(document, read, null, Strictness.LATER);
            return ShippedOutcome.CREATED;
        }
        if (!document.isCatalogue()) {
            LOGGER.warn("An owner's document already holds the address '{}'; the shipped document is not written.",
                        document.getSlug());
            return ShippedOutcome.ADDRESS_TAKEN;
        }
        document.fileUnder(folder);
        if (read.checksum().equals(document.getShippedChecksum()) && !document.isShippedUpdatePending()) {
            return ShippedOutcome.UNCHANGED;
        }
        if (read.checksum().equals(document.getSourceChecksum())) {
            document.recordShippedText(read.checksum());
            return ShippedOutcome.UNCHANGED;
        }
        if (documents.isHeadEdited(document.getId())) {
            document.recordShippedUpdateKept(read.checksum());
            return ShippedOutcome.KEPT_EDIT;
        }
        document.recordShippedText(read.checksum());
        writeRevision(document, read, null, Strictness.LATER);
        return ShippedOutcome.REVISED;
    }

    /**
     * Derives a shipped document's relations from its head, once the whole library is written. A reference to
     * something the catalogue does not hold is left out and logged, never fatal: the application must start.
     */
    @Transactional
    public void indexShippedRelations(String slug) {
        documents.findBySlug(slug).filter(StoredDocument::isCatalogue).ifPresent(document -> document.replaceRelations(
                relationsOf(document, scope(null).head(document), Strictness.LEAVE_OUT)));
    }

    /**
     * Takes the shipped file's newer text for a document a curator had edited: the file's text becomes the next
     * revision and nothing waits any more. The curator's edit stays in the history, one revision back.
     */
    @Transactional
    public DocumentDetail takeShippedText(String id, String shippedSource) {
        StoredDocument document = access.requireReadable(id);
        access.requireWritable(document);
        if (!document.isCatalogue() || !document.isShippedUpdatePending()) {
            throw CadProblem.conflict("Nothing newer ships for this document.");
        }
        ReadDocument read = reading.read(shippedSource, document.getSlug());
        document.recordShippedText(read.checksum());
        if (read.checksum().equals(document.getSourceChecksum())) {
            return detail(document);
        }
        return detail(writeRevision(document, read, authority.requireActor(), Strictness.REFUSE));
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
    private StoredDocument writeRevision(StoredDocument document, ReadDocument read, String writtenBy,
                                         Strictness strictness) {
        validate(document, read);
        Instant now = clock.instant();
        int revision = document.getRevision() + 1;
        LocalDate day = LocalDate.ofInstant(now, properties.revisions().zone());
        RevisionLabel label = documents.nextLabel(document.getId(), day);
        DocumentIndex index = read.index();
        document.indexRevision(revision, label, read.checksum(), index.kind(), index.title().strip(), index.summary(),
                               index.category(), index.featured(), now);
        document.replaceTags(tagsOf(index));
        if (strictness != Strictness.LATER) {
            document.replaceRelations(relationsOf(document, read, strictness));
        }
        StoredDocument stored = documents.save(document);
        documents.flush();
        documents.appendSource(stored.getId(), revision, label, read.source(), writtenBy, now);
        return stored;
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
        if (index.category() != null && !categories.exists(index.category())) {
            throw CadProblem.invalid("'" + index.category() + "' is not a category.");
        }
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
            if (tag.length() > TAG_LENGTH) {
                throw CadProblem.invalid("A tag is at most " + TAG_LENGTH + " characters: '" + tag + "' is not.");
            }
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
            Optional<StoredDocument> related = documents.findBySlug(reference.slug()).filter(scope::visible);
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
        String problem = null;
        try {
            read = source != null ? reading.read(source, document.getSlug()) : scope.head(document);
        } catch (CadProblem unreadable) {
            problem = unreadable.getMessage();
        }
        Map<String, StoredDocument> related = documents.findByIds(document.getRelations().stream()
                        .map(StoredDocument.Relation::getRelatedDocumentId).toList()).stream()
                .collect(Collectors.toMap(StoredDocument::getId, Function.identity()));
        List<RelatedView> relatedViews = document.getRelations().stream()
                .map(relation -> Optional.ofNullable(related.get(relation.getRelatedDocumentId()))
                        .filter(access::canRead)
                        .map(found -> new RelatedView(found.getSlug(), found.getTitle(), found.getKind(), relation.getKind())))
                .flatMap(Optional::stream).toList();
        Object model = read == null ? null : read.model().orElse(null);
        return new DocumentDetail(summary(document, problem), text, revision, document.getVersion(),
                                  read == null ? Map.of() : metaOf(read), relatedViews,
                                  model, ModelFacts.slidersOf(model), ModelFacts.presentationOf(model),
                                  document.isShippedUpdatePending(), access.canWrite(document));
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

    private DocumentSummary summary(StoredDocument document) {
        String problem = null;
        try {
            scope(document.owner().orElse(null)).head(document);
        } catch (CadProblem unreadable) {
            problem = unreadable.getMessage();
        }
        return summary(document, problem);
    }

    private static DocumentSummary summary(StoredDocument document, String problem) {
        return new DocumentSummary(document.getId(), document.getSlug(), document.getTitle(), document.getSummary(),
                                   document.getKind(), document.getCategoryCode(), document.getFolder(),
                                   document.isFeatured(), document.getStatus().name(),
                                   document.isCatalogue() ? Origin.CATALOGUE : Origin.OWNED,
                                   document.owner().orElse(null), List.copyOf(document.getTags()),
                                   document.getRevision(), document.getRevisionLabel(), document.getUpdatedAt(),
                                   problem);
    }

    private static RevisionView revisionView(DocumentRevision revision) {
        return new RevisionView(revision.revision(), revision.label().token(), revision.checksum(), revision.writtenAt(),
                                revision.writtenBy());
    }

    // ── Small things ─────────────────────────────────────────────────────────────────────────────────────────────

    private static StoredDocument.Status statusOf(String status) {
        try {
            return StoredDocument.Status.valueOf(status == null ? "" : status.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw CadProblem.invalid("A status is DRAFT, PUBLISHED or ARCHIVED.");
        }
    }

    private static void requirePage(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw CadProblem.invalid("A page is from 0, and holds 1–200 rows.");
        }
    }

    private static String newIdentifier() {
        return UUID.randomUUID().toString();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
