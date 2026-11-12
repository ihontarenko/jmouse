package org.jmouse.cad.folder;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.git.local.CommitAuthor;
import org.jmouse.git.local.JGitLocalGit;
import org.jmouse.revisions.RevisionLabel;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The documents are the library folder's files: a write is a commit of the file where the document stands, a document
 * filed elsewhere moves with its files and keeps its history, and the folder opened again gives every revision back —
 * what was put there by hand recorded as found.
 */
class DocumentStoreTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    private static final CommitAuthor FOLDER = new CommitAuthor("library", "library@example.org");
    private static final DocumentOwner WORKSPACE = DocumentOwner.of("WORKSPACE", "w1");

    private static DocumentStore storeOf(Path folder) {
        return new DocumentStore(folder, new JGitLocalGit(), actor -> Optional.of(new CommitAuthor(actor, actor + "@example.org")),
                FOLDER, () -> KYIV);
    }

    /** What a reader of the text hands the store: the index of a revision. */
    private static void index(StoredDocument document, int revision, Instant at, String category) {
        document.indexRevision(revision, new RevisionLabel(LocalDate.ofInstant(at, KYIV), revision), "checksum", "component",
                "Resistor", null, null, category, false, at);
    }

    /** A footprint of a code, kept by an owner or by the catalogue. */
    private static StoredDocument coded(DocumentStore store, String slug, DocumentOwner owner, String code) {
        Instant at = Instant.parse("2026-11-07T08:00:00Z");
        StoredDocument document = store.save(new StoredDocument(slug, owner, "anna", at));
        document.indexRevision(1, new RevisionLabel(LocalDate.ofInstant(at, KYIV), 1), "checksum", "footprint", slug, code, null,
                "footprints", false, at);
        return document;
    }

    @Test
    void aCodeAskedFromNoPlaceNamesTheCataloguesDocumentAndOnlyThenTheOnlyCopy(@TempDir Path folder) {
        DocumentStore store = storeOf(folder);
        store.open();
        coded(store, "chip-resistor-0805-footprint", null, "FP.CHIP:R0805");
        coded(store, "my-0805", WORKSPACE, "FP.CHIP:R0805");
        coded(store, "rcl-r0402", WORKSPACE, "FP.RCL:R0402");
        coded(store, "rcl-r0402-again", DocumentOwner.of("WORKSPACE", "w2"), "FP.RCL:R0603");
        coded(store, "rcl-r0603", WORKSPACE, "FP.RCL:R0603");

        assertEquals("chip-resistor-0805-footprint", store.findByAddress("FP.CHIP:R0805").orElseThrow().getSlug());
        assertEquals("rcl-r0402", store.findByAddress("FP.RCL:R0402").orElseThrow().getSlug());
        // Two owners' copies and no catalogue one: only a place can say which is meant.
        assertEquals(Optional.empty(), store.findByAddress("FP.RCL:R0603"));
        assertEquals("my-0805", store.findByAddress("my-0805").orElseThrow().getSlug());
    }

    @Test
    void theFolderLetsGitReadItsLongPaths(@TempDir Path folder) throws IOException {
        storeOf(folder).open();
        try (Git opened = Git.open(folder.toFile())) {
            assertEquals("true", opened.getRepository().getConfig().getString("core", null, "longpaths"));
        }
    }

    @Test
    void aDocumentIsItsFileAndEveryRevisionACommit(@TempDir Path folder) throws IOException {
        DocumentStore store = storeOf(folder);
        assertEquals(List.of(), store.open());
        Instant first = Instant.parse("2026-10-23T08:00:00Z");
        StoredDocument resistor = store.save(new StoredDocument("resistor", WORKSPACE, "anna", first));
        index(resistor, 1, first, "passive");
        store.appendSource("resistor", 1, store.nextLabel("resistor", LocalDate.of(2026, 10, 23)), "one", null, RevisionOrigin.IMPORTED, first);
        assertTrue(Files.exists(folder.resolve("owners/WORKSPACE/w1/passive/resistor.component.jm")));
        store.keepFile("resistor", "body.step", new byte[] {1, 2, 3}, "anna");

        // Filed in another category: the file moves, its files with it.
        Instant second = Instant.parse("2026-10-23T09:00:00Z");
        index(resistor, 2, second, "resistors");
        store.appendSource("resistor", 2, store.nextLabel("resistor", LocalDate.of(2026, 10, 23)), "two", "anna", RevisionOrigin.WRITTEN, second);
        assertTrue(Files.exists(folder.resolve("owners/WORKSPACE/w1/resistors/resistor.component.jm")));
        assertFalse(Files.exists(folder.resolve("owners/WORKSPACE/w1/passive/resistor.component.jm")));
        assertEquals(List.of("body.step"), store.fileNames("resistor"));

        // Opened again, the folder gives every revision back, and a change made by hand there is recorded as found.
        Files.writeString(folder.resolve("owners/WORKSPACE/w1/resistors/resistor.component.jm"), "three", StandardCharsets.UTF_8);
        DocumentStore opened = storeOf(folder);
        List<StoredDocument> held = opened.open();
        assertEquals(1, held.size());
        StoredDocument read = held.getFirst();
        assertEquals("resistor", read.getId());
        assertEquals(Optional.of(WORKSPACE), read.owner());
        assertEquals("three", opened.heldText(read));
        List<DocumentRevision> revisions = opened.revisions("resistor");
        assertEquals(List.of(3, 2, 1), revisions.stream().map(DocumentRevision::revision).toList());
        assertEquals(List.of(RevisionOrigin.WRITTEN, RevisionOrigin.WRITTEN, RevisionOrigin.IMPORTED),
                revisions.stream().map(DocumentRevision::origin).toList());
        assertEquals("anna", revisions.get(1).writtenBy());
        assertEquals(first, revisions.get(2).writtenAt());
        assertEquals("20261023_001", revisions.get(2).label().token());
        assertEquals(Optional.of("one"), opened.sourceAt("resistor", 1));
        assertEquals(Optional.of("two"), opened.sourceAt("resistor", 2));
        assertEquals(Optional.of(new byte[] {1, 2, 3}).map(bytes -> bytes.length), opened.readFile("resistor", "body.step").map(bytes -> bytes.length));

        opened.remove(read, "anna");
        assertFalse(Files.exists(folder.resolve("owners/WORKSPACE/w1/resistors/resistor.component.jm")));
        assertEquals(List.of(), storeOf(folder).open());
    }

    /** A file put in by hand is a document from the start: its place says its kind, its category and its owner before its text is read. */
    @Test
    void aFilePutInByHandIsWhatItsPlaceSays(@TempDir Path folder) throws IOException {
        storeOf(folder).open();
        Files.createDirectories(folder.resolve("catalogue/capacitors"));
        Files.writeString(folder.resolve("catalogue/capacitors/ceramic.component.jm"), "not a document yet", StandardCharsets.UTF_8);
        Files.createDirectories(folder.resolve("owners/PROJECT/p1"));
        Files.writeString(folder.resolve("owners/PROJECT/p1/filter.circuit.jm"), "neither", StandardCharsets.UTF_8);

        List<StoredDocument> held = storeOf(folder).open();

        StoredDocument ceramic = held.stream().filter(document -> document.getSlug().equals("ceramic")).findFirst().orElseThrow();
        assertEquals("component", ceramic.getKind());
        assertEquals("capacitors", ceramic.getCategoryCode());
        assertEquals(Optional.empty(), ceramic.owner());
        assertEquals(1, ceramic.getRevision());
        StoredDocument filter = held.stream().filter(document -> document.getSlug().equals("filter")).findFirst().orElseThrow();
        assertEquals("circuit", filter.getKind());
        assertEquals(null, filter.getCategoryCode());
        assertEquals(Optional.of(DocumentOwner.of("PROJECT", "p1")), filter.owner());
    }
}
