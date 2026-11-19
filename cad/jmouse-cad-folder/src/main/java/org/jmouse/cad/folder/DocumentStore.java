package org.jmouse.cad.folder;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.core.io.PortablePaths;
import org.jmouse.storage.Content;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.exception.ObjectNotFoundException;
import org.jmouse.storage.local.LocalFileStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 📚 The documents: an index in memory ({@link StoredDocument}) of the files behind it — the library folder's, and the files
 * libraries ship, read in place.
 *
 * <h3>The folder is files</h3>
 *
 * <p>A document of the library folder is its file, as it stands: opening the folder walks its files and reads nothing else. The
 * folder keeps no history of its own — whether it is a repository is no concern of the library; a project opened in the work is
 * where git, as a plugin, has its say. A document's revisions are those written while the engine runs: the file as it stood when
 * the folder was opened is its first, each save the next.</p>
 *
 * <h3>Where a document stands in the folder</h3>
 *
 * <p>Every place in words, as the {@link FolderLayout} says them both ways — {@code libraries/owner/symbols/op-amps/lm358.symbol.jm}
 * —; the store composes and reads no path of its own. A document's files stand beside it, in {@code <slug>.files/}. A document
 * filed elsewhere moves, its files with it.</p>
 *
 * <p>It does not authorize anything, and it reads no text: the index is handed to it by whoever reads the texts.</p>
 */
public class DocumentStore {

    /** A document's file ends so: {@code resistor.component.jm}. */
    private static final String SUFFIX = FolderLayout.SUFFIX;
    /** A document's own files stand beside it in a folder ending so: {@code resistor.files}. */
    private static final String FILES = ".files";
    private static final String SEPARATOR = PortablePaths.SEPARATOR;

    /** The storage backend a document's own files are kept through: the library folder, on this disk. */
    private static final String FILES_BACKEND = "library";

    /**
     * One revision of a held document: what it is, and where its file stood. Its text is the file's while it is the head and was
     * found there; kept here once a newer revision is written over the file.
     */
    private final class Kept {

        private final int number;
        private final Instant writtenAt;
        private final String writtenBy;
        private final RevisionOrigin origin;
        private final String path;
        /** The text, where it is no longer the file's; null for the head the folder was opened with, read from its file. */
        private String text;
        private String checksum;

        private Kept(int number, Instant writtenAt, String writtenBy, RevisionOrigin origin, String path, String text) {
            this.number = number;
            this.writtenAt = writtenAt;
            this.writtenBy = writtenBy;
            this.origin = origin;
            this.path = path;
            this.text = text;
            this.checksum = text == null ? null : checksumOf(text);
        }

        private String text() {
            return text != null ? text : read(path);
        }

        private String checksum() {
            if (checksum == null) {
                checksum = checksumOf(text());
            }
            return checksum;
        }

        /** Keeps its text here, before a newer revision is written over its file. */
        private void keepText() {
            if (text == null) {
                text = read(path);
            }
        }

        private DocumentRevision revision() {
            return new DocumentRevision(number, checksum(), writtenAt, writtenBy, origin);
        }
    }

    /** Where the folder is, asked each time the store opens: another folder is another library. */
    private final Supplier<Path> folders;
    /** The folder the store holds now: the one it was last opened on. */
    private volatile Path folder;
    /** The documents' own files, through jMouse storage, rooted at the folder the store holds now. */
    private volatile LocalFileStore files;
    /** How a place is written in words. */
    private final FolderLayout layout;
    private final Map<String, StoredDocument> documents = new LinkedHashMap<>();
    /** Every held document's revisions, the oldest first, by its address. */
    private final Map<String, List<Kept>> histories = new HashMap<>();

    /**
     * @param folders where the library folder is, asked each time it is opened
     * @param layout  how a place is written in words: an owner's folder, a kind's
     */
    public DocumentStore(Supplier<Path> folders, FolderLayout layout) {
        this.folders = folders;
        this.layout = layout;
    }

    /** The checksum of a file's bytes: its SHA-256, in lowercase hex. */
    public static String checksumOf(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", impossible);
        }
    }

    /** 🔐 The checksum a text is cached and compared under: its SHA-256, in lowercase hex. */
    public static String checksumOf(String source) {
        return checksumOf(source.getBytes(StandardCharsets.UTF_8));
    }

    // ── The folder ───────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Opens the library folder — where it is now: another folder is another library —, and answers every document it holds,
     * each at its first revision, the file as it stands, its place what its path says and its index left to whoever reads its
     * text ({@link #heldText}). What the store held before is let go.
     */
    public synchronized List<StoredDocument> open() {
        folder = folders.get();
        files = new LocalFileStore(FILES_BACKEND, folder);
        documents.clear();
        histories.clear();
        try {
            Files.createDirectories(folder);
        } catch (IOException unwritable) {
            throw new UncheckedIOException("The library folder " + folder + " cannot be made", unwritable);
        }
        List<StoredDocument> held = new ArrayList<>();
        for (String path : documentPaths()) {
            String file = path.substring(path.lastIndexOf(SEPARATOR) + 1);
            String slug = file.substring(0, file.indexOf('.'));
            Instant writtenAt = modifiedAt(path);
            FolderLayout.Placed placed = placedAt(path).orElseThrow();
            StoredDocument document = new StoredDocument(slug, placed.owner(), null, writtenAt);
            // What its place says, until its text is read: its kind from its name, its category from its folder.
            document.indexRevision(1, null, file.substring(slug.length() + 1, file.length() - SUFFIX.length()), slug, null, null,
                    placed.category(), false, writtenAt);
            histories.put(slug, new ArrayList<>(List.of(new Kept(1, writtenAt, null, RevisionOrigin.UNRECORDED, path, null))));
            documents.put(slug, document);
            held.add(document);
        }
        return held;
    }

    /** The head text of a held document, as its file holds it. */
    public synchronized String heldText(StoredDocument document) {
        return read(pathOf(document.getId()));
    }

    /** A held document's head revision: its number and when it was written. */
    public synchronized DocumentRevision headOf(StoredDocument document) {
        return histories.get(document.getId()).getLast().revision();
    }

    /**
     * Whether a path of the folder is a document's file: in an owner's folder or the catalogue's, none of a hidden folder's — a
     * repository's own —, none of a document's files.
     */
    private boolean isDocumentPath(String path) {
        return path.endsWith(SUFFIX) && !path.startsWith(".") && !path.contains(SEPARATOR + ".") && !path.contains(FILES + SEPARATOR)
               && placedAt(path).isPresent();
    }

    /** Every document file of the folder, relative to it; a hidden folder is not walked into. */
    private List<String> documentPaths() {
        try (Stream<Path> walked = Files.walk(folder)) {
            return walked.filter(Files::isRegularFile).map(path -> PortablePaths.below(folder, path))
                    .filter(this::isDocumentPath).sorted().toList();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The library folder " + folder + " cannot be read", unreadable);
        }
    }

    private Instant modifiedAt(String path) {
        try {
            return Files.getLastModifiedTime(folder.resolve(path)).toInstant();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The document file " + path + " cannot be read", unreadable);
        }
    }

    /** What a path of the folder says of the document at it, as the layout reads it. */
    private Optional<FolderLayout.Placed> placedAt(String path) {
        return layout.placedAt(List.of(path.split(SEPARATOR)));
    }

    /** Where a document stands in the folder, as the layout says it. */
    private String placeOf(StoredDocument document) {
        return layout.pathOf(new FolderLayout.Place(document.owner().orElse(null), document.getKind(), document.getCategoryCode(),
                document.getSlug()));
    }

    private String pathOf(String documentId) {
        List<Kept> history = histories.get(documentId);
        if (history == null || history.isEmpty()) {
            throw new IllegalStateException("'" + documentId + "' is no document of the library folder");
        }
        return history.getLast().path;
    }

    private String read(String path) {
        try {
            return Files.readString(folder.resolve(path), StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The document file " + path + " cannot be read", unreadable);
        }
    }

    // ── The index ────────────────────────────────────────────────────────────────────────────────────────────────

    public synchronized Optional<StoredDocument> find(String id) {
        return Optional.ofNullable(documents.get(id));
    }

    /**
     * One document by its address, asked from no place: its slug, which is its identifier, or its technical code
     * ({@code CHIP:R0805:F}). A code asked so names the catalogue's document of that code; failing one, the only document
     * that has it. Several owners' copies and no catalogue one is no answer: which is meant only a place can say, and
     * {@code StoredDocuments.addressed} is where a place says it.
     */
    public synchronized Optional<StoredDocument> findByAddress(String address) {
        if (TechnicalClass.ofCode(address).isEmpty()) {
            return find(address);
        }
        List<StoredDocument> coded = findByCode(address);
        Optional<StoredDocument> catalogue = coded.stream().filter(StoredDocument::isCatalogue).findFirst();
        if (catalogue.isPresent()) {
            return catalogue;
        }
        return coded.size() == 1 ? Optional.of(coded.getFirst()) : Optional.empty();
    }

    public synchronized Optional<StoredDocument> findBySlug(String slug) {
        return find(slug);
    }

    /** Every document a technical code names — the catalogue's and every owner's copy of it alike: a copy keeps the code. */
    public synchronized List<StoredDocument> findByCode(String code) {
        return select(document -> code.equals(document.getCode()));
    }

    /** Every document whose code of this class ends in this name, in whatever library — {@code R0805} finds {@code CHIP:R0805:F}. */
    public synchronized List<StoredDocument> findByCodeName(TechnicalClass kind, String name) {
        return select(document -> document.getCode() != null && kind.nameIn(document.getCode()).filter(name::equals).isPresent());
    }

    /** Every document indexed as one a library ships, held in its file. */
    public synchronized List<StoredDocument> findShipped() {
        return select(document -> document.isShipped());
    }

    public synchronized boolean existsBySlug(String slug) {
        return documents.containsKey(slug);
    }

    /** The documents with these identifiers; an identifier nothing has is left out. */
    public synchronized List<StoredDocument> findByIds(Collection<String> ids) {
        return ids.stream().map(documents::get).filter(Objects::nonNull).toList();
    }

    public synchronized List<StoredDocument> findBySlugs(Collection<String> slugs) {
        return findByIds(slugs);
    }

    /** The documents whose texts name this one — by a relation of that kind when one is given. */
    public synchronized List<StoredDocument> findRelating(String relatedDocumentId, String relationKind) {
        return select(document -> document.getRelations().stream().anyMatch(relation -> relation.relatedDocumentId().equals(relatedDocumentId)
                && (relationKind == null || relation.kind().equals(relationKind))));
    }

    /** Every document of one kind, catalogue and owned alike. */
    public synchronized List<StoredDocument> findByKind(String kind) {
        return select(document -> kind.equals(document.getKind()));
    }

    /**
     * 📖 The catalogue listing: catalogue documents in the statuses asked for, narrowed by every filter that is not
     * {@code null}, featured first and then by title.
     *
     * @param categories the categories a document may be filed in — one and every category under it —; null for any
     * @param text a text the title, summary or slug holds, any case; null for any
     */
    /** The role that asks for every document that is anything to a bench. */
    public static final String ANY_BENCH_ROLE = "any";

    public synchronized StoredPage<StoredDocument> browse(Collection<DocumentStatus> statuses, Set<String> categories, String kind,
                                                          Boolean featured, String text, String tag, String benchRole, int page, int size) {
        String wanted = text == null ? null : text.toLowerCase(Locale.ROOT);
        List<StoredDocument> listed = select(document -> document.isCatalogue() && statuses.contains(document.getStatus())
                && (categories == null || categories.contains(document.getCategoryCode()))
                && (kind == null || kind.equals(document.getKind()))
                && (featured == null || featured == document.isFeatured())
                && (wanted == null || holds(document.getTitle(), wanted) || holds(document.getSummary(), wanted)
                    || holds(document.getSlug(), wanted))
                && (tag == null || document.getTags().contains(tag))
                && (benchRole == null || (ANY_BENCH_ROLE.equals(benchRole) ? document.getBenchRole() != null : benchRole.equals(document.getBenchRole()))));
        return StoredPage.of(listed.stream().sorted(Comparator.comparing(StoredDocument::isFeatured).reversed()
                .thenComparing(StoredDocument::getTitle, Comparator.nullsLast(Comparator.naturalOrder()))).toList(), page, size);
    }

    private static boolean holds(String field, String wanted) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(wanted);
    }

    /** 📂 One owner's documents, most recently changed first. */
    public synchronized List<StoredDocument> owned(DocumentOwner owner) {
        return select(document -> document.owner().filter(owner::equals).isPresent()).stream()
                .sorted(Comparator.comparing(StoredDocument::getUpdatedAt).reversed()).toList();
    }

    /** 📂 One owner's documents, most recently changed first, a page of them. */
    public synchronized StoredPage<StoredDocument> owned(DocumentOwner owner, int page, int size) {
        return StoredPage.of(owned(owner), page, size);
    }

    /** 📋 Every document of one kind an owner holds, by title — a project's sheets. */
    public synchronized List<StoredDocument> owned(DocumentOwner owner, String kind) {
        return select(document -> document.owner().filter(owner::equals).isPresent() && kind.equals(document.getKind())).stream()
                .sorted(Comparator.comparing(StoredDocument::getTitle, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    /** How many documents one owner holds: a project's sheets, and what an import made for them. */
    public synchronized long countOwned(DocumentOwner owner) {
        return documents.values().stream().filter(document -> document.owner().filter(owner::equals).isPresent()).count();
    }

    /** How many documents are the catalogue's, and how many an owner holds. */
    public synchronized DocumentCounts count() {
        long catalogue = documents.values().stream().filter(StoredDocument::isCatalogue).count();
        return new DocumentCounts(catalogue, documents.size() - catalogue);
    }

    /** The documents the index holds, the catalogue's apart from those owners hold. */
    public record DocumentCounts(long catalogue, long owned) {
    }

    /** Whether any document, catalogue or owned, is filed in a category. */
    public synchronized boolean anyInCategory(String categoryCode) {
        return documents.values().stream().anyMatch(document -> categoryCode.equals(document.getCategoryCode()));
    }

    /** How many catalogue documents of one status each category holds — of one kind when one is given. */
    public synchronized Map<String, Long> countCatalogueByCategory(DocumentStatus status, String kind) {
        Map<String, Long> counted = new LinkedHashMap<>();
        documents.values().stream().filter(document -> document.isCatalogue() && document.getStatus() == status
                        && document.getCategoryCode() != null && (kind == null || kind.equals(document.getKind())))
                .forEach(document -> counted.merge(document.getCategoryCode(), 1L, Long::sum));
        return counted;
    }

    /** 💾 Keeps a document in the index: a new one is added, one indexed already is the same object. */
    public synchronized StoredDocument save(StoredDocument document) {
        documents.putIfAbsent(document.getId(), document);
        return document;
    }

    /** 🗑️ Takes a document away: from the index, and — one of the library folder — its file and its files. */
    public synchronized void remove(StoredDocument document) {
        documents.remove(document.getId());
        List<Kept> history = histories.remove(document.getId());
        if (history == null || history.isEmpty()) {
            return;
        }
        deleteFiles(history.getLast().path);
    }

    private List<StoredDocument> select(Predicate<StoredDocument> wanted) {
        return documents.values().stream().filter(wanted).toList();
    }

    // ── Texts ────────────────────────────────────────────────────────────────────────────────────────────────────

    /** 📜 The text of one revision of a document of the library folder; empty for one it does not hold. */
    public synchronized Optional<String> sourceAt(String documentId, int revision) {
        List<Kept> history = histories.get(documentId);
        if (history == null || revision < 1 || revision > history.size()) {
            return Optional.empty();
        }
        return Optional.of(history.get(revision - 1).text());
    }

    /** ✍️ Whether a person saved the head revision — as opposed to the engine writing its own. */
    public synchronized boolean isHeadEdited(String documentId) {
        List<Kept> history = histories.get(documentId);
        return history != null && !history.isEmpty() && history.getLast().writtenBy != null;
    }

    /** Where a document's revision came from; {@link RevisionOrigin#UNRECORDED} for one this store does not hold. */
    public synchronized RevisionOrigin originOf(String documentId, int revision) {
        List<Kept> history = histories.get(documentId);
        return history == null || revision < 1 || revision > history.size() ? RevisionOrigin.UNRECORDED
                : history.get(revision - 1).origin;
    }

    /** 📜 Every revision of a document, the newest first, without their texts. */
    public synchronized List<DocumentRevision> revisions(String documentId) {
        List<DocumentRevision> revisions = new ArrayList<>(histories.getOrDefault(documentId, List.of()).stream().map(Kept::revision).toList());
        Collections.reverse(revisions);
        return List.copyOf(revisions);
    }

    /** The word that names a document's newest revision after an {@code @}: {@code lm358@latest}. */
    public static final String LATEST = "latest";

    /** 🔎 The revision what follows an {@code @} names: its number ({@code lm358@3}) or {@code latest}; nothing else names one. */
    public synchronized Optional<Integer> revisionWritten(String documentId, String written) {
        String value = written == null ? "" : written.strip();
        List<Kept> history = histories.getOrDefault(documentId, List.of());
        if (value.equalsIgnoreCase(LATEST)) {
            return history.isEmpty() ? Optional.empty() : Optional.of(history.getLast().number);
        }
        if (!value.matches("\\d{1,7}")) {
            return Optional.empty();
        }
        int revision = Integer.parseInt(value);
        return revision >= 1 && revision <= history.size() ? Optional.of(revision) : Optional.empty();
    }

    /**
     * ➕ Writes a text as the given revision of a document: its file, where the document stands now — moved there, its files with
     * it, when it was filed elsewhere.
     *
     * @param writtenBy who saved it; null for the engine
     * @param origin    where it came from: a person, or an import
     * @param writtenAt when
     */
    public synchronized void appendSource(String documentId, int revision, String source, String writtenBy,
                                          RevisionOrigin origin, Instant writtenAt) {
        StoredDocument document = documents.get(documentId);
        if (document == null) {
            throw new IllegalStateException("'" + documentId + "' is not indexed: index it before its text is written");
        }
        List<Kept> history = histories.computeIfAbsent(documentId, ignored -> new ArrayList<>());
        if (revision != history.size() + 1) {
            throw new IllegalStateException("'" + documentId + "' is at revision " + history.size() + ", not " + (revision - 1));
        }
        String target = placeOf(document);
        if (!history.isEmpty()) {
            Kept head = history.getLast();
            // The head's text kept before its file is written over or moved away.
            head.keepText();
            if (!head.path.equals(target)) {
                moveFiles(head.path, target);
                try {
                    Files.deleteIfExists(folder.resolve(head.path));
                } catch (IOException undeletable) {
                    throw new UncheckedIOException("The document file " + head.path + " cannot be moved", undeletable);
                }
            }
        }
        write(target, source);
        history.add(new Kept(revision, writtenAt, writtenBy, origin, target, source));
    }

    private void write(String path, String text) {
        try {
            Path file = folder.resolve(path);
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException unwritable) {
            throw new UncheckedIOException("The document file " + path + " cannot be written", unwritable);
        }
    }

    // ── A document's files ───────────────────────────────────────────────────────────────────────────────────────

    /** The folder a document's own files stand in, beside its file. */
    private static String filesOf(String documentPath) {
        String file = documentPath.substring(documentPath.lastIndexOf(SEPARATOR) + 1);
        String slug = file.substring(0, file.indexOf('.'));
        return documentPath.substring(0, documentPath.lastIndexOf(SEPARATOR) + 1) + slug + FILES;
    }

    /** Every file a document holds, by the path it stands at in the folder. */
    private List<String> fileEntries(String documentPath) {
        try {
            return files.list(StorageKey.of(filesOf(documentPath))).stream().filter(entry -> !entry.folder())
                    .map(entry -> entry.key().value()).sorted().toList();
        } catch (ObjectNotFoundException none) {
            return List.of();
        }
    }

    private void moveFiles(String from, String to) {
        try {
            files.move(StorageKey.of(filesOf(from)), StorageKey.of(filesOf(to)));
        } catch (ObjectNotFoundException none) {
            // A document that holds no files moves alone.
        }
    }

    private void deleteFiles(String documentPath) {
        fileEntries(documentPath).forEach(entry -> files.delete(StorageKey.of(entry)));
        try {
            files.deleteFolder(StorageKey.of(filesOf(documentPath)));
        } catch (ObjectNotFoundException none) {
            // It held no files.
        }
        try {
            Files.deleteIfExists(folder.resolve(documentPath));
        } catch (IOException undeletable) {
            throw new UncheckedIOException("The document file " + documentPath + " cannot be removed", undeletable);
        }
    }

    /** The names of the files a document of the library folder holds, in order. */
    public synchronized List<String> fileNames(String documentId) {
        List<Kept> history = histories.get(documentId);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        return fileEntries(history.getLast().path).stream().map(path -> path.substring(path.lastIndexOf(SEPARATOR) + 1)).toList();
    }

    /** One file a document of the library folder holds, read whole; empty when it holds none of that name. */
    public synchronized Optional<byte[]> readFile(String documentId, String name) {
        List<Kept> history = histories.get(documentId);
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }
        StorageKey key = StorageKey.of(filesOf(history.getLast().path) + SEPARATOR + name);
        try (InputStream content = files.read(key).getInputStream()) {
            return Optional.of(content.readAllBytes());
        } catch (ObjectNotFoundException none) {
            return Optional.empty();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The file " + name + " of " + documentId + " cannot be read", unreadable);
        }
    }

    /** Keeps a file a document holds: written anew under its name, or taken away when {@code content} is null. */
    public synchronized void keepFile(String documentId, String name, byte[] content) {
        if (name.contains(SEPARATOR) || name.contains("\\") || name.startsWith(".")) {
            throw new IllegalArgumentException("A document's file is named by one plain name, not '" + name + "'");
        }
        List<Kept> history = histories.get(documentId);
        if (history == null || history.isEmpty()) {
            throw new IllegalStateException("'" + documentId + "' is no document of the library folder: it holds no files");
        }
        String entry = filesOf(history.getLast().path) + SEPARATOR + name;
        StorageKey key = StorageKey.of(entry);
        if (content == null) {
            if (fileEntries(history.getLast().path).stream().anyMatch(entry::equals)) {
                files.delete(key);
            }
            return;
        }
        files.write(key, Content.ofBytes(name, null, content));
    }

    /** The folder the documents are kept in: the one the store was last opened on, where it is now before it was ever opened. */
    public Path folder() {
        Path opened = folder;
        return opened != null ? opened : folders.get();
    }

    /** Where a document of the library folder stands in it, by its address: what a host shows as where a copy is held. */
    public synchronized Optional<String> pathInFolder(String documentId) {
        List<Kept> history = histories.get(documentId);
        return history == null || history.isEmpty() ? Optional.empty() : Optional.of(history.getLast().path);
    }
}
