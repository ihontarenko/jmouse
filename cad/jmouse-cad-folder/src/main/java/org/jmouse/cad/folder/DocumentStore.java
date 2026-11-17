package org.jmouse.cad.folder;

import java.io.InputStream;
import org.jmouse.storage.local.LocalFileStore;
import org.jmouse.storage.exception.ObjectNotFoundException;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.Content;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.core.io.PortablePaths;
import org.jmouse.git.local.CommitAuthor;
import org.jmouse.git.local.CommitAuthors;
import org.jmouse.git.local.LocalGit;
import org.jmouse.git.local.PathRevision;
import org.jmouse.git.local.PathsCommit;
import org.jmouse.git.local.WorkingTreeChange;

import java.io.IOException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 📚 The documents: an index in memory ({@link StoredDocument}) of the files behind it — the library folder's, a git
 * repository in which every revision of a document's text is a commit, and the files libraries ship, read in place.
 *
 * <h3>Where a document stands in the folder</h3>
 *
 * <p>Every place in words, as the {@link FolderLayout} says them both ways — {@code libraries/owner/symbols/op-amps/lm358.symbol.jm}
 * —; the store composes and reads no path of its own. A document's files stand beside it, in {@code <slug>.files/}. A document
 * filed elsewhere moves, and its commit says so, so its history goes on.</p>
 *
 * <h3>What a commit says</h3>
 *
 * <p>Who wrote it — the trailer {@code Written-By: <actor>} — and where it came from — {@code Origin: WRITTEN|IMPORTED}
 * —: a revision is read back from its commit whole. The commit's author is the person the host names
 * ({@link CommitAuthors}), else the folder's own identity. A file put into the folder by hand, or changed there, is
 * recorded as found when the folder is opened: the folder is the truth.</p>
 *
 * <p>It does not authorize anything, and it reads no text: the index is handed to it by whoever reads the texts.</p>
 */
public class DocumentStore {

    /** A document's file ends so: {@code resistor.component.jm}. */
    private static final String SUFFIX = FolderLayout.SUFFIX;
    /** The words the folder ignores a document's own files by: they are bytes, kept beside the history, never in it. */
    private static final String IGNORED_FILES = "*" + ".files/";
    /** A document's own files stand beside it in a folder ending so: {@code resistor.files}. */
    private static final String FILES = ".files";
    private static final String SEPARATOR = PortablePaths.SEPARATOR;

    private static final String WRITTEN_BY = "Written-By";
    private static final String ORIGIN = "Origin";
    private static final Pattern WRITTEN_BY_LINE = Pattern.compile("^" + WRITTEN_BY + ": (.+)$", Pattern.MULTILINE);
    private static final Pattern ORIGIN_LINE = Pattern.compile("^" + ORIGIN + ": ([A-Z]+)$", Pattern.MULTILINE);

    /**
     * One revision of a held document: what it is, the commit it is, where the file stood in it and its content's
     * identifier. Its text — and so its checksum — is read only when asked for: opening the folder reads no old text.
     */
    private final class Kept {

        private final int number;
        private final Instant writtenAt;
        private final String writtenBy;
        private final RevisionOrigin origin;
        private final String commit;
        private final String path;
        /** The text's identifier in the repository; null for one written now, whose checksum is known. */
        private final String content;
        private String checksum;

        private Kept(int number, Instant writtenAt, String writtenBy, RevisionOrigin origin, String commit, String path, String content,
                     String checksum) {
            this.number = number;
            this.writtenAt = writtenAt;
            this.writtenBy = writtenBy;
            this.origin = origin;
            this.commit = commit;
            this.path = path;
            this.content = content;
            this.checksum = checksum;
        }

        private String checksum() {
            if (checksum == null) {
                checksum = checksumOf(git.text(folder, content));
            }
            return checksum;
        }

        private DocumentRevision revision() {
            return new DocumentRevision(number, checksum(), writtenAt, writtenBy, origin);
        }
    }

    /** The storage backend a document's own files are kept through: the library folder, on this disk. */
    private static final String FILES_BACKEND = "library";
    /** The trailer a kept file is said by: {@code File: <path> sha256=<hex> bytes=<n>}. */
    private static final String FILE_LINE = "File: ";

    /** The repository setting that lets git on Windows read a path longer than 260 characters. */
    private static final String LONG_PATHS = "core.longpaths";

    /** Where the folder is, asked each time the store opens: another folder is another library. */
    private final Supplier<Path> folders;
    /** The folder the store holds now: the one it was last opened on. */
    private volatile Path folder;
    /** The documents' own files, through jMouse storage, rooted at the folder the store holds now. */
    private volatile LocalFileStore files;
    private final LocalGit git;
    /** How a place is written in words. */
    private final FolderLayout layout;
    private final CommitAuthors authors;
    private final CommitAuthor identity;
    private final Map<String, StoredDocument> documents = new LinkedHashMap<>();
    /** Every held document's revisions, the oldest first, by its address. */
    private final Map<String, List<Kept>> histories = new HashMap<>();

    /**
     * @param folders      where the library folder is, asked each time it is opened; made a repository when it is not one yet
     * @param layout       how a place is written in words: an owner's folder, a kind's
     * @param authors      who a person's commit is by
     * @param identity     who a commit nobody named is by: the engine's, the folder's
     */
    public DocumentStore(Supplier<Path> folders, FolderLayout layout, LocalGit git, CommitAuthors authors, CommitAuthor identity) {
        this.folders = folders;
        this.layout = layout;
        this.git = git;
        this.authors = authors;
        this.identity = identity;
    }

    /**
     * 🔐 The checksum a text is cached and compared under: its SHA-256, in lowercase hex.
     */
    /** The checksum of a file's bytes: its SHA-256, in lowercase hex. */
    public static String checksumOf(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", impossible);
        }
    }

    public static String checksumOf(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", impossible);
        }
    }

    // ── The folder ───────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Opens the library folder — where it is now: another folder is another library, its documents and their revisions alone
     * —: makes it a repository when it is none, records what was put or changed there by hand, and answers every document it
     * holds — each with its revisions and its place, its index left to whoever reads its text ({@link #heldText}). What the
     * store held before is let go.
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
        if (!git.status(folder).repository()) {
            git.initialize(folder);
        }
        // A document's path — its owner, its category, its slug and kind — runs past 260 characters on Windows, where git
        // reads a longer one only when the repository says so: set every time, for a folder made before this was.
        git.configure(folder, LONG_PATHS, "true");
        ignoreFiles();
        recordFound();
        // Every document file's history in one walk of the repository, however many there are.
        Map<String, List<PathRevision>> all = git.histories(folder, this::isDocumentPath);
        List<StoredDocument> held = new ArrayList<>();
        for (String path : documentPaths()) {
            String file = path.substring(path.lastIndexOf(SEPARATOR) + 1);
            String slug = file.substring(0, file.indexOf('.'));
            List<Kept> history = keptOf(all.getOrDefault(path, List.of()));
            if (history.isEmpty()) {
                continue;
            }
            Kept first = history.getFirst();
            Kept head = history.getLast();
            FolderLayout.Placed placed = placedAt(path).orElseThrow();
            StoredDocument document = new StoredDocument(slug, placed.owner(), first.writtenBy, first.writtenAt);
            // What its place says, until its text is read: its kind from its name, its category from its folder.
            document.indexRevision(head.number, null, file.substring(slug.length() + 1,
                    file.length() - SUFFIX.length()), slug, null, null, placed.category(), false, head.writtenAt);
            histories.put(slug, history);
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
     * Keeps a document's own files out of the history: bytes — a model, a datasheet — are not text a revision compares, and a
     * history that holds every one only grows. The folder's {@code .gitignore} says so, written once; each kept file's commit says
     * its sha256 and its size instead ({@link #keepFile}).
     */
    private void ignoreFiles() {
        Path ignore = folder.resolve(".gitignore");
        try {
            String written = Files.exists(ignore) ? Files.readString(ignore, StandardCharsets.UTF_8) : "";
            if (written.lines().anyMatch(line -> line.strip().equals(IGNORED_FILES))) {
                return;
            }
            Files.writeString(ignore, (written.isEmpty() || written.endsWith("\n") ? written : written + "\n") + IGNORED_FILES + "\n",
                              StandardCharsets.UTF_8);
        } catch (IOException unwritable) {
            throw new UncheckedIOException("The library folder's .gitignore cannot be written", unwritable);
        }
        git.commitPaths(folder, new PathsCommit("Its own files kept beside the history\n\n" + ORIGIN + ": " + RevisionOrigin.WRITTEN,
                identity, null, List.of(".gitignore")));
    }

    /** Commits every document file put into the folder, changed or taken away there by hand, as found. */
    private void recordFound() {
        List<String> found = git.status(folder).changes().stream().map(WorkingTreeChange::path)
                .filter(path -> path.endsWith(SUFFIX) || path.contains(FILES + SEPARATOR)).toList();
        if (!found.isEmpty()) {
            git.commitPaths(folder, new PathsCommit("Found in the library folder\n\n" + ORIGIN + ": " + RevisionOrigin.WRITTEN,
                    identity, null, found));
        }
    }

    /**
     * Whether a path of the folder is a document's file: in an owner's folder or the catalogue's, none of a repository's own, none
     * of a document's files.
     */
    private boolean isDocumentPath(String path) {
        return path.endsWith(SUFFIX) && !path.startsWith(".git") && !path.contains(FILES + SEPARATOR)
               && placedAt(path).isPresent();
    }

    /** Every document file of the folder, relative to it. */
    private List<String> documentPaths() {
        try (Stream<Path> walked = Files.walk(folder)) {
            return walked.filter(Files::isRegularFile).map(path -> PortablePaths.below(folder, path))
                    .filter(this::isDocumentPath).sorted().toList();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The library folder " + folder + " cannot be read", unreadable);
        }
    }

    /** What a path of the folder says of the document at it, as the layout reads it. */
    private Optional<FolderLayout.Placed> placedAt(String path) {
        return layout.placedAt(List.of(path.split(SEPARATOR)));
    }

    /** A file's revisions as its commits say them, the oldest first; their texts are not read. */
    private List<Kept> keptOf(List<PathRevision> commits) {
        List<Kept> kept = new ArrayList<>();
        for (PathRevision commit : commits) {
            kept.add(new Kept(kept.size() + 1, commit.time(), trailer(WRITTEN_BY_LINE, commit.message()).orElse(null),
                    trailer(ORIGIN_LINE, commit.message()).map(RevisionOrigin::valueOf).orElse(RevisionOrigin.UNRECORDED),
                    commit.commit(), commit.path(), commit.content(), null));
        }
        return kept;
    }

    private static Optional<String> trailer(Pattern line, String message) {
        Matcher found = line.matcher(message);
        return found.find() ? Optional.of(found.group(1).strip()) : Optional.empty();
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

    private CommitAuthor authorOf(String actor) {
        return actor == null ? identity : authors.authorOf(actor).orElse(identity);
    }

    private static String messageOf(String what, String slug, String actor, RevisionOrigin origin) {
        StringBuilder message = new StringBuilder(what).append(' ').append(slug).append("\n\n");
        if (actor != null) {
            message.append(WRITTEN_BY).append(": ").append(actor).append('\n');
        }
        return message.append(ORIGIN).append(": ").append(origin.name()).toString();
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

    /**
     * 🗑️ Takes a document away: from the index, and — one of the library folder — its file and its files, the removal
     * committed by whoever took it away.
     */
    public synchronized void remove(StoredDocument document, String actor) {
        documents.remove(document.getId());
        List<Kept> history = histories.remove(document.getId());
        if (history == null || history.isEmpty()) {
            return;
        }
        String path = history.getLast().path;
        List<String> paths = new ArrayList<>(List.of(path));
        paths.addAll(fileEntries(path));
        deleteFiles(path);
        git.commitPaths(folder, new PathsCommit(messageOf("Removed", document.getSlug(), actor, RevisionOrigin.WRITTEN),
                authorOf(actor), null, paths));
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
        Kept kept = history.get(revision - 1);
        return revision == history.size() ? Optional.of(read(kept.path)) : Optional.of(git.text(folder, kept.content));
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
     * ➕ Writes a text as the given revision of a document: its file, where the document stands now — moved there, its
     * files with it, when it was filed elsewhere —, committed by whoever wrote it.
     *
     * @param writtenBy who saved it; null for the engine
     * @param origin    where it came from: a person, or an import
     * @param writtenAt when; a history written again keeps its own times
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
        String before = history.isEmpty() ? null : history.getLast().path;
        List<PathsCommit.Move> moves = new ArrayList<>();
        List<String> paths = new ArrayList<>(List.of(target));
        if (before != null && !before.equals(target)) {
            moves.add(new PathsCommit.Move(before, target));
            paths.addAll(fileEntries(before));
            moveFiles(before, target);
            paths.addAll(fileEntries(target));
            try {
                Files.deleteIfExists(folder.resolve(before));
            } catch (IOException undeletable) {
                throw new UncheckedIOException("The document file " + before + " cannot be moved", undeletable);
            }
        }
        write(target, source);
        String commit = git.commitPaths(folder, new PathsCommit(messageOf(history.isEmpty() ? "Created" : "Written", document.getSlug(),
                writtenBy, origin), authorOf(writtenBy), writtenAt, paths, moves)).id();
        history.add(new Kept(revision, writtenAt, writtenBy, origin, commit, target, null, checksumOf(source)));
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

    /**
     * Keeps a file a document holds — written anew under its name, or taken away when {@code content} is null —, committed
     * by whoever kept it, now.
     */
    public synchronized void keepFile(String documentId, String name, byte[] content, String actor) {
        keepFile(documentId, name, content, actor, null);
    }

    /** The same, at its own time: a history written again keeps the time each file was kept. */
    public synchronized void keepFile(String documentId, String name, byte[] content, String actor, Instant when) {
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
            if (fileEntries(history.getLast().path).stream().noneMatch(entry::equals)) {
                return;
            }
            files.delete(key);
        } else {
            files.write(key, Content.ofBytes(name, null, content));
        }
        // The bytes stay out of the history; the commit says which they were.
        String said = messageOf(content == null ? "Removed the file " + name + " of" : "Kept the file " + name + " of", documentId, actor,
                RevisionOrigin.WRITTEN) + (content == null ? "" : "\n" + FILE_LINE + entry + " sha256=" + checksumOf(content) + " bytes=" + content.length);
        git.commitPaths(folder, new PathsCommit(said, authorOf(actor), when, List.of(entry)));
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
