package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.document.source.DocumentResources;
import org.jmouse.cad.document.source.DocumentSource;
import org.jmouse.cad.document.source.HeldDocument;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.library.DocumentIndex;
import org.jmouse.cad.store.authority.CadPlace;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The catalogue as the engine asks it — by slug, or by {@code slug@revision} — for a document of one owner, or of the
 * shared catalogue: what a circuit's parts, a component's base and family are found in. It sees the shared catalogue
 * and the documents of every owner of that owner's place — a project's own and its workspace's —, never another
 * owner's, whatever their slugs say.
 *
 * <p>Access is decided before the engine is asked: the document being run or written was checked, and what it names
 * is whatever its owner may place.</p>
 *
 * <p>It is the database's {@link DocumentSource}: a document it finds is held {@link DocumentLocation.Stored}, at the
 * revision found, with the files {@link DocumentFiles} keeps for it.</p>
 */
public class StoredDocuments implements PartDocuments, DocumentSource {

    private final DocumentStore   documents;
    private final DocumentReading reading;
    private final CadPlace        place;
    private final DocumentFiles   files;
    private final DocumentTexts   texts;
    /** What the place holds besides the store, asked of its sources once, and only when a document is looked for. */
    private final Supplier<List<PlaceSource.Holding>> holdings;
    private List<PlaceSource.PlacedDocument>          placed;

    /**
     * @param place    the owners whose documents are seen besides the catalogue's; the installation for the catalogue's
     *                 alone
     * @param holdings what the place holds besides the store, found before it
     */
    public StoredDocuments(DocumentStore documents, DocumentReading reading, CadPlace place, DocumentFiles files,
                           DocumentTexts texts, Supplier<List<PlaceSource.Holding>> holdings) {
        this.documents = documents;
        this.reading = reading;
        this.place = place;
        this.files = files;
        this.texts = texts;
        this.holdings = holdings;
    }

    /** What a revision's number is marked with when the document at hand is not what is stored. */
    private static final String DRAFT_MARK = "-draft";

    @Override
    public Optional<String> revisionOf(DocumentNode document) {
        return documents.findBySlug(document.slug()).map(stored -> {
            DocumentLanguage language = reading.language().language();
            boolean same = language.write(language.readOne(DocumentInput.text(stored.getSlug(), headSource(stored)))).equals(language.write(document));
            String revision = String.valueOf(stored.getRevision());
            return same ? revision : revision + DRAFT_MARK;
        });
    }

    /** A document's text as the store holds it at one revision, and the stored document it is of. */
    private record Located(StoredDocument document, int revision, ReadDocument read) {
    }

    @Override
    public Optional<DocumentNode> find(String reference) {
        return resolve(reference).map(ReadDocument::document);
    }

    /** The document a technical code names from here: the copy nearest this place ({@link #addressed}). */
    @Override
    public Optional<DocumentNode> findCoded(String code) {
        return TechnicalClass.ofCode(code).isPresent() ? find(code) : Optional.empty();
    }

    /** The codes of one class ending in this name that are seen from here: what the place holds first, then the store's, nearest first. */
    @Override
    public List<String> codesNamed(TechnicalClass kind, String name) {
        Stream<String> held = placed().stream().map(document -> document.read().index().code())
                .filter(code -> code != null && kind.nameIn(code).filter(name::equals).isPresent());
        Stream<String> stored = documents.findByCodeName(kind, name).stream().filter(this::visible)
                .sorted(Comparator.comparingInt(this::distance)).map(StoredDocument::getCode);
        return Stream.concat(held, stored).distinct().toList();
    }

    /** The document a reference names, read at the revision it pins — its head when it pins none. */
    public Optional<ReadDocument> resolve(String reference) {
        Optional<PlaceSource.PlacedDocument> held = placed(reference);
        return held.isPresent() ? held.map(PlaceSource.PlacedDocument::read) : located(reference).map(Located::read);
    }

    /** What the place holds besides the store: the documents of a folder of a project's, as they stand now. */
    public List<PlaceSource.PlacedDocument> placed() {
        if (placed == null) {
            placed = holdings.get().stream().flatMap(holding -> holding.documents().stream()).toList();
        }
        return placed;
    }

    /** Whether a document was found besides the store: who may see it was decided by who may ask this place. */
    public boolean isPlaced(ReadDocument read) {
        return placed().stream().anyMatch(document -> document.read() == read);
    }

    /** A document the place holds besides the store; it keeps no revisions, so a reference that pins one is the store's. */
    private Optional<PlaceSource.PlacedDocument> placed(String reference) {
        String word = DocumentIndex.slugOf(reference);
        if (word.length() != reference.length()) {
            return Optional.empty();
        }
        boolean coded = TechnicalClass.ofCode(word).isPresent();
        return placed().stream().filter(document -> word.equals(coded ? document.read().index().code() : document.read().index().slug()))
                .findFirst();
    }

    @Override
    public Optional<HeldDocument> locate(String reference) {
        Optional<PlaceSource.PlacedDocument> held = placed(reference);
        if (held.isPresent()) {
            return held.map(document -> new HeldDocument(document.read().document(), document.location(), document.read().checksum()));
        }
        return located(reference).map(found -> new HeldDocument(found.read().document(),
                DocumentTexts.locationOf(found.document(), found.revision()),
                found.document().getId() + DocumentIndex.REVISION_MARK + found.revision()));
    }

    @Override
    public DocumentResources resources(HeldDocument document) {
        return switch (document.location()) {
            case DocumentLocation.Stored stored -> files.of(stored.id(), document.document().slug());
            // A shipped document's files would ship beside it in its jar; none does yet.
            case DocumentLocation.Shipped shipped -> DocumentResources.none();
            case DocumentLocation.Mounted mounted -> holdings.get().stream()
                    .map(holding -> holding.resourcesOf(mounted)).flatMap(Optional::stream).findFirst().orElseGet(DocumentResources::none);
            default -> throw new IllegalArgumentException("The catalogue holds no " + document.location().kind() + " document");
        };
    }

    /**
     * The document a word names from here: by its slug, or by its technical code ({@code CHIP:R0805:F}) — then the copy
     * nearest this place, an owner's own before the catalogue's, as a copy keeps the code of what it was copied from.
     */
    public Optional<StoredDocument> addressed(String word) {
        if (TechnicalClass.ofCode(word).isEmpty()) {
            return documents.findBySlug(word).filter(this::visible);
        }
        return documents.findByCode(word).stream().filter(this::visible).min(Comparator.comparingInt(this::distance));
    }

    /** The slug of the document a reference names from here — its code read, its revision kept —; empty for none. */
    public Optional<String> slugOf(String reference) {
        Optional<PlaceSource.PlacedDocument> held = placed(reference);
        if (held.isPresent()) {
            return held.map(document -> document.read().index().slug());
        }
        String word = DocumentIndex.slugOf(reference);
        return addressed(word).map(document -> document.getSlug() + reference.substring(word.length()));
    }

    /** How near a document is to this place: its owner's place in the chain, the catalogue last. */
    private int distance(StoredDocument document) {
        List<DocumentOwner> chain = place.chain();
        int index = document.owner().map(chain::indexOf).orElse(-1);
        return index < 0 ? chain.size() - index : chain.size() - 1 - index;
    }

    private Optional<Located> located(String reference) {
        String slug = DocumentIndex.slugOf(reference);
        Optional<StoredDocument> found = addressed(slug);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        StoredDocument document = found.get();
        if (slug.length() == reference.length()) {
            return Optional.of(new Located(document, document.getRevision(), head(document)));
        }
        String written = reference.substring(slug.length() + DocumentIndex.REVISION_MARK.length());
        return texts.revisionWritten(document, written).flatMap(revision -> texts.at(document, revision)
                .map(source -> new Located(document, revision, reading.read(source, reference))));
    }

    /** A stored document's head revision, read once per revision. */
    public ReadDocument head(StoredDocument document) {
        return reading.stored(document.getSourceChecksum(), () -> headSource(document), document.getSlug());
    }

    /** A catalogued document's head text, wherever it is held. */
    public String headSource(StoredDocument document) {
        return texts.head(document);
    }

    /** Whether the engine may find this document from here. */
    public boolean visible(StoredDocument document) {
        return document.isCatalogue() || document.owner().filter(place.chain()::contains).isPresent();
    }
}
