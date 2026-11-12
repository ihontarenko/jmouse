package org.jmouse.cad.folder;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.git.local.CommitAuthor;
import org.jmouse.git.local.CommitAuthors;
import org.jmouse.git.local.LocalGit;
import org.jmouse.git.local.PathRevision;
import org.jmouse.git.local.PathsCommit;
import org.jmouse.git.local.WorkingTreeChange;
import org.jmouse.revisions.RevisionLabel;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * <p>In the folder of its category: {@code catalogue/<category>/<slug>.<kind>.jm} for the shared catalogue's,
 * {@code owners/<owner type>/<owner id>/<category>/<slug>.<kind>.jm} for an owner's; one filed in no category stands
 * directly in its owner's folder. A category document is a document like any other: it stands in the folder of the
 * category above it. Its files stand beside it, in {@code <slug>.files/}. A document filed elsewhere moves, and its
 * commit says so, so its history goes on.</p>
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

    /** Where the shared catalogue's documents stand. */
    private static final String CATALOGUE = "catalogue";
    /** Where every owner's documents stand, by the owner's type and identifier. */
    private static final String OWNERS = "owners";
    /** A document's file ends so: {@code resistor.component.jm}. */
    private static final String SUFFIX = ".jm";
    /** A document's own files stand beside it in a folder ending so: {@code resistor.files}. */
    private static final String FILES = ".files";
    private static final String SEPARATOR = "/";

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
        private final RevisionLabel label;
        private final Instant writtenAt;
        private final String writtenBy;
        private final RevisionOrigin origin;
        private final String commit;
        private final String path;
        /** The text's identifier in the repository; null for one written now, whose checksum is known. */
        private final String content;
        private String checksum;

        private Kept(int number, RevisionLabel label, Instant writtenAt, String writtenBy, RevisionOrigin origin, String commit, String path,
                     String content, String checksum) {
            this.number = number;
            this.label = label;
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
            return new DocumentRevision(number, label, checksum(), writtenAt, writtenBy, origin);
        }
    }

    /** The repository setting that lets git on Windows read a path longer than 260 characters. */
    private static final String LONG_PATHS = "core.longpaths";

    private final Path folder;
    private final LocalGit git;
    private final CommitAuthors authors;
    private final CommitAuthor identity;
    private final Supplier<ZoneId> revisionZone;
    private final Map<String, StoredDocument> documents = new LinkedHashMap<>();
    /** Every held document's revisions, the oldest first, by its address. */
    private final Map<String, List<Kept>> histories = new HashMap<>();

    /**
     * @param folder       the library folder; made a repository when it is not one yet
     * @param authors      who a person's commit is by
     * @param identity     who a commit nobody named is by: the engine's, the folder's
     * @param revisionZone where the day a revision is said by begins, as the installation says it now
     */
    public DocumentStore(Path folder, LocalGit git, CommitAuthors authors, CommitAuthor identity, Supplier<ZoneId> revisionZone) {
        this.folder = folder;
        this.git = git;
        this.authors = authors;
        this.identity = identity;
        this.revisionZone = revisionZone;
    }

    /**
     * 🔐 The checksum a text is cached and compared under: its SHA-256, in lowercase hex.
     */
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
     * Opens the library folder: makes it a repository when it is none, records what was put or changed there by hand,
     * and answers every document it holds — each with its revisions and its place, its index left to whoever reads its
     * text ({@link #heldText}).
     */
    public synchronized List<StoredDocument> open() {
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
        recordFound();
        // Every document file's history in one walk of the repository, however many there are.
        Map<String, List<PathRevision>> all = git.histories(folder, DocumentStore::isDocumentPath);
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
            StoredDocument document = new StoredDocument(slug, ownerOf(path), first.writtenBy, first.writtenAt);
            // What its place says, until its text is read: its kind from its name, its category from its folder.
            document.indexRevision(head.number, head.label, null, file.substring(slug.length() + 1,
                    file.length() - SUFFIX.length()), slug, null, null, categoryOf(path), false, head.writtenAt);
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

    /** A held document's head revision: its number, its label and when it was written. */
    public synchronized DocumentRevision headOf(StoredDocument document) {
        return histories.get(document.getId()).getLast().revision();
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

    /** Whether a path of the folder is a document's file: none of a repository's own, none of a document's files. */
    private static boolean isDocumentPath(String path) {
        return path.endsWith(SUFFIX) && !path.startsWith(".git") && !path.contains(FILES + SEPARATOR);
    }

    /** Every document file of the folder, relative to it. */
    private List<String> documentPaths() {
        try (Stream<Path> walked = Files.walk(folder)) {
            return walked.filter(Files::isRegularFile).map(path -> folder.relativize(path).toString().replace(File.separatorChar, '/'))
                    .filter(DocumentStore::isDocumentPath).sorted().toList();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The library folder " + folder + " cannot be read", unreadable);
        }
    }

    /** The owner a path is a document of: none under {@value #CATALOGUE}, the owner named under {@value #OWNERS}. */
    private static DocumentOwner ownerOf(String path) {
        String[] parts = path.split(SEPARATOR);
        return parts[0].equals(OWNERS) && parts.length > 3 ? DocumentOwner.of(parts[1], parts[2]) : null;
    }

    /** The category whose folder a path stands in; none for one standing directly in its owner's folder. */
    private static String categoryOf(String path) {
        String[] parts = path.split(SEPARATOR);
        int ownerDepth = parts[0].equals(OWNERS) ? 3 : 1;
        return parts.length > ownerDepth + 1 ? parts[parts.length - 2] : null;
    }

    /** A file's revisions as its commits say them, the oldest first; their texts are not read. */
    private List<Kept> keptOf(List<PathRevision> commits) {
        List<Kept> kept = new ArrayList<>();
        Map<LocalDate, Integer> perDay = new HashMap<>();
        for (PathRevision commit : commits) {
            LocalDate day = LocalDate.ofInstant(commit.time(), revisionZone.get());
            int ordinal = perDay.merge(day, 1, Integer::sum);
            kept.add(new Kept(kept.size() + 1, new RevisionLabel(day, ordinal), commit.time(), trailer(WRITTEN_BY_LINE, commit.message()).orElse(null),
                    trailer(ORIGIN_LINE, commit.message()).map(RevisionOrigin::valueOf).orElse(RevisionOrigin.UNRECORDED),
                    commit.commit(), commit.path(), commit.content(), null));
        }
        return kept;
    }

    private static Optional<String> trailer(Pattern line, String message) {
        Matcher found = line.matcher(message);
        return found.find() ? Optional.of(found.group(1).strip()) : Optional.empty();
    }

    /**
     * Where a document stands in the folder: in its owner's folder, in the folder of its category, named by its slug and
     * kind.
     */
    private static String placeOf(StoredDocument document) {
        StringBuilder path = new StringBuilder();
        document.owner().ifPresentOrElse(
                owner -> path.append(OWNERS).append(SEPARATOR).append(owner.type()).append(SEPARATOR).append(owner.id()),
                () -> path.append(CATALOGUE));
        if (document.getCategoryCode() != null) {
            path.append(SEPARATOR).append(document.getCategoryCode());
        }
        return path.append(SEPARATOR).append(document.getSlug()).append('.').append(document.getKind()).append(SUFFIX).toString();
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
     * ({@code FP.CHIP:R0805}). A code asked so names the catalogue's document of that code; failing one, the only document
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

    /** Every document whose code of this class ends in this name, in whatever library — {@code R0805} finds {@code FP.CHIP:R0805}. */
    public synchronized List<StoredDocument> findByCodeName(TechnicalClass kind, String name) {
        return select(document -> document.getCode() != null && kind.nameIn(document.getCode()).filter(name::equals).isPresent());
    }

    /** Every document indexed as one a library ships, held in its file. */
    public synchronized List<StoredDocument> findShipped() {
        return select(document -> document.getHolding() == StoredDocument.Holding.SHIPPED);
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
     * @param text a text the title, summary or slug holds, any case; null for any
     */
    public synchronized StoredPage<StoredDocument> browse(Collection<DocumentStatus> statuses, String category, String kind,
                                                          Boolean featured, String text, String tag, int page, int size) {
        String wanted = text == null ? null : text.toLowerCase(Locale.ROOT);
        List<StoredDocument> listed = select(document -> document.isCatalogue() && statuses.contains(document.getStatus())
                && (category == null || category.equals(document.getCategoryCode()))
                && (kind == null || kind.equals(document.getKind()))
                && (featured == null || featured == document.isFeatured())
                && (wanted == null || holds(document.getTitle(), wanted) || holds(document.getSummary(), wanted)
                    || holds(document.getSlug(), wanted))
                && (tag == null || document.getTags().contains(tag)));
        return StoredPage.of(listed.stream().sorted(Comparator.comparing(StoredDocument::isFeatured).reversed()
                .thenComparing(StoredDocument::getTitle, Comparator.nullsLast(Comparator.naturalOrder()))).toList(), page, size);
    }

    private static boolean holds(String field, String wanted) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(wanted);
    }

    /** 📂 One owner's documents, most recently changed first. */
    public synchronized StoredPage<StoredDocument> owned(DocumentOwner owner, int page, int size) {
        return StoredPage.of(select(document -> document.owner().filter(owner::equals).isPresent()).stream()
                .sorted(Comparator.comparing(StoredDocument::getUpdatedAt).reversed()).toList(), page, size);
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

    /** 🏷️ The label the next revision of a document written on {@code day} takes: that day, after every revision of it that day. */
    public synchronized RevisionLabel nextLabel(String documentId, LocalDate day) {
        int written = (int) histories.getOrDefault(documentId, List.of()).stream().filter(kept -> kept.label.day().equals(day)).count();
        return RevisionLabel.next(day, written);
    }

    /**
     * 🔎 The revision what follows an {@code @} names: a revision number ({@code lm358@3}), a label
     * ({@code lm358@20261023_001}), a day — its last revision — ({@code lm358@20261023}) or {@code latest}.
     */
    public synchronized Optional<Integer> revisionWritten(String documentId, String written) {
        String value = written == null ? "" : written.strip();
        if (RevisionLabel.Reference.written(value)) {
            return revisionOf(documentId, RevisionLabel.Reference.parse(value));
        }
        if (!value.matches("\\d{1,7}")) {
            return Optional.empty();
        }
        int revision = Integer.parseInt(value);
        return revision >= 1 ? Optional.of(revision) : Optional.empty();
    }

    /** 🔎 The revision number a reference names: one revision by its label, the last of a day, or the latest. */
    public synchronized Optional<Integer> revisionOf(String documentId, RevisionLabel.Reference reference) {
        return histories.getOrDefault(documentId, List.of()).stream()
                .filter(kept -> switch (reference) {
                    case RevisionLabel.Exact exact -> kept.label.equals(exact.label());
                    case RevisionLabel.LastOfDay lastOfDay -> kept.label.day().equals(lastOfDay.day());
                    case RevisionLabel.Latest ignored -> true;
                })
                .map(kept -> kept.number).max(Integer::compare);
    }

    /**
     * ➕ Writes a text as the given revision of a document: its file, where the document stands now — moved there, its
     * files with it, when it was filed elsewhere —, committed by whoever wrote it.
     *
     * @param writtenBy who saved it; null for the engine
     * @param origin    where it came from: a person, or an import
     * @param writtenAt when; a history written again keeps its own times
     */
    public synchronized void appendSource(String documentId, int revision, RevisionLabel label, String source, String writtenBy,
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
        history.add(new Kept(revision, label, writtenAt, writtenBy, origin, commit, target, null, checksumOf(source)));
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
        Path files = folder.resolve(filesOf(documentPath));
        if (!Files.isDirectory(files)) {
            return List.of();
        }
        try (Stream<Path> listed = Files.list(files)) {
            return listed.map(path -> folder.relativize(path).toString().replace('\\', '/')).sorted().toList();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The files of " + documentPath + " cannot be read", unreadable);
        }
    }

    private void moveFiles(String from, String to) {
        Path source = folder.resolve(filesOf(from));
        if (!Files.isDirectory(source)) {
            return;
        }
        try {
            Path target = folder.resolve(filesOf(to));
            Files.createDirectories(target.getParent());
            Files.move(source, target);
        } catch (IOException unmovable) {
            throw new UncheckedIOException("The files of " + from + " cannot be moved", unmovable);
        }
    }

    private void deleteFiles(String documentPath) {
        try {
            for (String entry : fileEntries(documentPath)) {
                Files.deleteIfExists(folder.resolve(entry));
            }
            Files.deleteIfExists(folder.resolve(filesOf(documentPath)));
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
        Path file = folder.resolve(filesOf(history.getLast().path)).resolve(name);
        try {
            return Files.isRegularFile(file) ? Optional.of(Files.readAllBytes(file)) : Optional.empty();
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
        try {
            Path file = folder.resolve(entry);
            if (content == null) {
                if (!Files.deleteIfExists(file)) {
                    return;
                }
            } else {
                Files.createDirectories(file.getParent());
                Files.write(file, content);
            }
        } catch (IOException unwritable) {
            throw new UncheckedIOException("The file " + name + " of " + documentId + " cannot be kept", unwritable);
        }
        git.commitPaths(folder, new PathsCommit(messageOf(content == null ? "Removed the file " + name + " of" : "Kept the file " + name + " of",
                documentId, actor, RevisionOrigin.WRITTEN), authorOf(actor), when, List.of(entry)));
    }

    /** The folder the documents are kept in. */
    public Path folder() {
        return folder;
    }

    /** Where a document of the library folder stands in it, by its address: what a host shows as where a copy is held. */
    public synchronized Optional<String> pathInFolder(String documentId) {
        List<Kept> history = histories.get(documentId);
        return history == null || history.isEmpty() ? Optional.empty() : Optional.of(history.getLast().path);
    }
}
