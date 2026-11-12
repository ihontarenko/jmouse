package org.jmouse.cad.folder;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.git.local.CommitAuthor;
import org.jmouse.git.local.JGitLocalGit;
import org.jmouse.revisions.RevisionLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one move of the documents out of the database they were kept in, into the library folder (ADR 0002): every
 * revision of every document written there becomes a commit — by its author, at its time, saying where it came from —,
 * and the files filed against it stand beside it. Run once, against an empty folder:
 *
 * <pre>
 * -Dcad.move.database=jdbc:mysql://… -Dcad.move.user=… -Dcad.move.password=… -Dcad.move.uploads=&lt;the files' storage root&gt;
 * -Dcad.move.folder=&lt;the library folder&gt; -Dcad.move.zone=Europe/Kyiv
 * </pre>
 *
 * <p>A document's kind and category are read from its head as written — no language is needed, so a kind a plugin brings
 * moves as any other.</p>
 *
 * <p>Once the folder, opened again, gives back every document moved and its files, the files moved are taken out of the
 * file store the way it removes one — every binding, then the file, in one transaction —, so nothing is kept twice; the
 * bytes nothing points at any more are the orphan sweeper's, as for any file removed.</p>
 */
@EnabledIfSystemProperty(named = "cad.move.database", matches = ".+")
class DatabaseDocumentsMove {

    private static final Pattern HEAD = Pattern.compile("^([a-z][a-z-]*):([a-z0-9][a-z0-9-]*)\\(", Pattern.MULTILINE);
    private static final Pattern CATEGORY = Pattern.compile("^\\s+category:\\s*([a-z0-9-]+)\\s*$", Pattern.MULTILINE);

    private record Revision(int number, String source, Instant writtenAt, String writtenBy, RevisionOrigin origin, RevisionLabel label) {
    }

    private record Moved(String id, String slug, DocumentOwner owner, String createdBy) {
    }

    /** A file filed against a document: its identifier in the file store, the name the document gives it, its bytes' key, when it was kept. */
    private record FiledFile(String identifier, String name, String storageKey, Instant keptAt) {
    }

    @Test
    void moveTheDocumentsIntoTheLibraryFolder() throws Exception {
        Path folder = Path.of(System.getProperty("cad.move.folder"));
        Path uploads = Path.of(System.getProperty("cad.move.uploads"));
        ZoneId zone = ZoneId.of(System.getProperty("cad.move.zone", ZoneId.systemDefault().getId()));
        assertTrue(!Files.exists(folder) || isEmpty(folder), "The library folder " + folder + " is to be empty before the move");
        DocumentStore store = new DocumentStore(folder, new JGitLocalGit(), actor -> Optional.empty(),
                new CommitAuthor(System.getProperty("cad.move.identity.name", "jmCAD"),
                        System.getProperty("cad.move.identity.email", "library@cad.jmouse.org")), () -> zone);
        store.open();
        int revisions = 0;
        int files = 0;
        List<String> moved = new ArrayList<>();
        List<String> movedFiles = new ArrayList<>();
        try (Connection database = DriverManager.getConnection(System.getProperty("cad.move.database"),
                System.getProperty("cad.move.user"), System.getProperty("cad.move.password"))) {
            for (Moved document : documents(database)) {
                List<Revision> written = revisions(database, document.id());
                StoredDocument kept = store.save(new StoredDocument(document.slug(), document.owner(), document.createdBy(),
                        written.getFirst().writtenAt()));
                for (Revision revision : written) {
                    Matcher head = HEAD.matcher(revision.source());
                    if (!head.find() || !head.group(2).equals(document.slug())) {
                        throw new IllegalStateException(document.slug() + "@" + revision.number() + " does not open as itself");
                    }
                    Matcher category = CATEGORY.matcher(revision.source());
                    kept.indexRevision(revision.number(), revision.label(), DocumentStore.checksumOf(revision.source()), head.group(1),
                            document.slug(), null, null, category.find() ? category.group(1) : null, false, revision.writtenAt());
                    store.appendSource(document.slug(), revision.number(), revision.label(), revision.source(), revision.writtenBy(),
                            revision.origin(), revision.writtenAt());
                    revisions++;
                }
                for (FiledFile file : files(database, document.id())) {
                    store.keepFile(document.slug(), file.name(), Files.readAllBytes(uploads.resolve(file.storageKey())),
                            document.createdBy(), file.keptAt());
                    movedFiles.add(file.identifier());
                    files++;
                }
                moved.add(document.slug());
            }
            // The folder opened again gives back what was moved: only then is the file store's copy taken away.
            DocumentStore opened = new DocumentStore(folder, new JGitLocalGit(), actor -> Optional.empty(),
                    new CommitAuthor("check", "check@cad.jmouse.org"), () -> zone);
            assertEquals(moved.size(), opened.open().size());
            for (String slug : moved) {
                assertEquals(store.fileNames(slug), opened.fileNames(slug), "The files of " + slug + " read back");
            }
            removeFromFileStore(database, movedFiles);
        }
        System.out.println("MOVED " + moved.size() + " documents, " + revisions + " revisions, " + files + " files into " + folder);
    }

    /** Removes the files moved the way the file store removes one (ManagedFiles.delete): every binding, then the file. */
    private static void removeFromFileStore(Connection database, List<String> identifiers) throws SQLException {
        boolean committing = database.getAutoCommit();
        database.setAutoCommit(false);
        try (PreparedStatement bindings = database.prepareStatement("DELETE FROM file_bindings WHERE file_id = ?");
             PreparedStatement managed = database.prepareStatement("DELETE FROM managed_files WHERE id = ?")) {
            for (String identifier : identifiers) {
                bindings.setString(1, identifier);
                bindings.addBatch();
                managed.setString(1, identifier);
                managed.addBatch();
            }
            bindings.executeBatch();
            managed.executeBatch();
            database.commit();
        } catch (SQLException failed) {
            database.rollback();
            throw failed;
        } finally {
            database.setAutoCommit(committing);
        }
    }

    private static boolean isEmpty(Path folder) throws java.io.IOException {
        try (var listed = Files.list(folder)) {
            return listed.findAny().isEmpty();
        }
    }

    private static List<Moved> documents(Connection database) throws SQLException {
        List<Moved> documents = new ArrayList<>();
        try (PreparedStatement query = database.prepareStatement(
                "SELECT id, slug, owner_type, owner_id, created_by FROM cad_documents WHERE holding = 'STORED' ORDER BY created_at, slug");
             ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                String ownerType = rows.getString("owner_type");
                documents.add(new Moved(rows.getString("id"), rows.getString("slug"),
                        ownerType == null ? null : DocumentOwner.of(ownerType, rows.getString("owner_id")), rows.getString("created_by")));
            }
        }
        return documents;
    }

    private static List<Revision> revisions(Connection database, String documentId) throws SQLException {
        List<Revision> revisions = new ArrayList<>();
        try (PreparedStatement query = database.prepareStatement("""
                SELECT revision, source, written_at, written_by, origin, revision_day, day_ordinal FROM cad_document_revisions
                WHERE document_id = ? ORDER BY revision""")) {
            query.setString(1, documentId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    revisions.add(new Revision(rows.getInt("revision"), rows.getString("source"),
                            rows.getTimestamp("written_at").toInstant(), rows.getString("written_by"),
                            RevisionOrigin.valueOf(rows.getString("origin")),
                            new RevisionLabel(rows.getDate("revision_day").toLocalDate(), rows.getInt("day_ordinal"))));
                }
            }
        }
        return revisions;
    }

    /** Each file filed against a document and not trashed. */
    private static List<FiledFile> files(Connection database, String documentId) throws SQLException {
        List<FiledFile> files = new ArrayList<>();
        try (PreparedStatement query = database.prepareStatement("""
                SELECT m.id, m.display_name, s.storage_key, m.created_at FROM file_bindings b JOIN managed_files m ON m.id = b.file_id
                JOIN stored_files s ON s.id = m.stored_file_id
                WHERE b.owner_type = 'CAD_DOCUMENT' AND b.owner_id = ? AND m.trashed_at IS NULL ORDER BY b.sort_order, m.display_name""")) {
            query.setString(1, documentId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    files.add(new FiledFile(rows.getString(1), rows.getString(2), rows.getString(3), rows.getTimestamp(4).toInstant()));
                }
            }
        }
        return files;
    }
}
