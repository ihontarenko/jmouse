package org.jmouse.cad.folder;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The documents are the library folder's files, no repository made: a write is the file where the document stands, a document
 * filed elsewhere moves with its files, the run keeps every revision, and the folder opened again is its files as they stand
 * — what was put there by hand included.
 */
class DocumentStoreTest {

    private static final DocumentOwner WORKSPACE = DocumentOwner.of("WORKSPACE", "w1");

    /** A layout in words: an owner's type its folder's word, the owner's identifier its name; a kind's folder its word with an s. */
    private static final FolderLayout LAYOUT = new FolderLayout() {
        private static final java.util.Map<String, String> WORDS = java.util.Map.of("WORKSPACE", "workspaces", "PROJECT", "projects");

        @Override
        public String pathOf(Place place) {
            String owner = place.owner() == null ? CATALOGUE : WORDS.get(place.owner().type()) + SEPARATOR + place.owner().id();
            return FolderLayout.standing(owner, place.kind() + "s", place);
        }

        @Override
        public Optional<Placed> placedAt(java.util.List<String> words) {
            if (words.getFirst().equals(CATALOGUE)) {
                return FolderLayout.standingAt(null, words.subList(1, words.size()));
            }
            return WORDS.entrySet().stream().filter(word -> word.getValue().equals(words.getFirst()) && words.size() > 2).findFirst()
                    .flatMap(word -> FolderLayout.standingAt(DocumentOwner.of(word.getKey(), words.get(1)), words.subList(2, words.size())));
        }
    };

    private static DocumentStore storeOf(Path folder) {
        return new DocumentStore(() -> folder, LAYOUT);
    }

    /** What a reader of the text hands the store: the index of a revision. */
    private static void index(StoredDocument document, int revision, Instant at, String category) {
        document.indexRevision(revision, "checksum", "component",
                "Resistor", null, null, category, false, at);
    }

    /** A footprint of a code, kept by an owner or by the catalogue. */
    private static StoredDocument coded(DocumentStore store, String slug, DocumentOwner owner, String code) {
        Instant at = Instant.parse("2026-11-07T08:00:00Z");
        StoredDocument document = store.save(new StoredDocument(slug, owner, "anna", at));
        document.indexRevision(1, "checksum", "footprint", slug, code, null,
                "footprints", false, at);
        return document;
    }

    @Test
    void theListingIsNarrowedByTheWordsAsWrittenNeverByAPatternLanguage(@TempDir Path folder) {
        DocumentStore store = storeOf(folder);
        store.open();
        coded(store, "sot-23-5-footprint", null, "SOT:SOT-23-5:F");
        coded(store, "chip-resistor-0805-footprint", null, "CHIP:R0805:F");
        Set<DocumentStatus> every = EnumSet.allOf(DocumentStatus.class);
        Function<String, List<String>> found = (text) -> store.browse(every, null, null, null, text, null, null, 0, 10)
                .rows().stream().map(StoredDocument::getSlug).toList();

        // Any case, anywhere in the slug; a `%` is a character like any other, so a LIKE pattern finds nothing.
        assertEquals(List.of("sot-23-5-footprint"), found.apply("SOT-23"));
        assertEquals(List.of("chip-resistor-0805-footprint"), found.apply("resistor"));
        assertEquals(List.of(), found.apply("%resistor%"));
    }

    @Test
    void aCodeAskedFromNoPlaceNamesTheCataloguesDocumentAndOnlyThenTheOnlyCopy(@TempDir Path folder) {
        DocumentStore store = storeOf(folder);
        store.open();
        coded(store, "chip-resistor-0805-footprint", null, "CHIP:R0805:F");
        coded(store, "my-0805", WORKSPACE, "CHIP:R0805:F");
        coded(store, "rcl-r0402", WORKSPACE, "RCL:R0402:F");
        coded(store, "rcl-r0402-again", DocumentOwner.of("WORKSPACE", "w2"), "RCL:R0603:F");
        coded(store, "rcl-r0603", WORKSPACE, "RCL:R0603:F");

        assertEquals("chip-resistor-0805-footprint", store.findByAddress("CHIP:R0805:F").orElseThrow().getSlug());
        assertEquals("rcl-r0402", store.findByAddress("RCL:R0402:F").orElseThrow().getSlug());
        // Two owners' copies and no catalogue one: only a place can say which is meant.
        assertEquals(Optional.empty(), store.findByAddress("RCL:R0603:F"));
        assertEquals("my-0805", store.findByAddress("my-0805").orElseThrow().getSlug());
    }

    @Test
    void theFolderIsFilesAndNoRepositoryIsMade(@TempDir Path folder) {
        storeOf(folder).open();
        assertFalse(Files.exists(folder.resolve(".git")), "opening the library folder makes no repository");
    }

    @Test
    void aDocumentIsItsFileAndEveryRevisionOfTheRunIsKept(@TempDir Path folder) throws IOException {
        DocumentStore store = storeOf(folder);
        assertEquals(List.of(), store.open());
        Instant first = Instant.parse("2026-10-23T08:00:00Z");
        StoredDocument resistor = store.save(new StoredDocument("resistor", WORKSPACE, "anna", first));
        index(resistor, 1, first, "passive");
        store.appendSource("resistor", 1, "one", null, RevisionOrigin.IMPORTED, first);
        assertTrue(Files.exists(folder.resolve("workspaces/w1/components/passive/resistor.component.jm")));
        store.keepFile("resistor", "body.step", new byte[] {1, 2, 3});

        // Filed in another category: the file moves, its files with it.
        Instant second = Instant.parse("2026-10-23T09:00:00Z");
        index(resistor, 2, second, "resistors");
        store.appendSource("resistor", 2, "two", "anna", RevisionOrigin.WRITTEN, second);
        assertTrue(Files.exists(folder.resolve("workspaces/w1/components/resistors/resistor.component.jm")));
        assertFalse(Files.exists(folder.resolve("workspaces/w1/components/passive/resistor.component.jm")));
        assertEquals(List.of("body.step"), store.fileNames("resistor"));

        // While the engine runs, every revision written is kept, the one written over its file too.
        List<DocumentRevision> revisions = store.revisions("resistor");
        assertEquals(List.of(2, 1), revisions.stream().map(DocumentRevision::revision).toList());
        assertEquals(List.of(RevisionOrigin.WRITTEN, RevisionOrigin.IMPORTED), revisions.stream().map(DocumentRevision::origin).toList());
        assertEquals("anna", revisions.getFirst().writtenBy());
        // A revision is named by its number, or as the newest: nothing else — no day, no label — names one.
        assertEquals(Optional.of(2), store.revisionWritten("resistor", "latest"));
        assertEquals(Optional.of(1), store.revisionWritten("resistor", "1"));
        assertEquals(Optional.empty(), store.revisionWritten("resistor", "3"));
        assertEquals(Optional.empty(), store.revisionWritten("resistor", "20261023_001"));
        assertEquals(Optional.of("one"), store.sourceAt("resistor", 1));
        assertEquals(Optional.of("two"), store.sourceAt("resistor", 2));

        // Opened again, the folder is its files: the document is its file as it stands, its first revision.
        Files.writeString(folder.resolve("workspaces/w1/components/resistors/resistor.component.jm"), "three", StandardCharsets.UTF_8);
        DocumentStore opened = storeOf(folder);
        List<StoredDocument> held = opened.open();
        assertEquals(1, held.size());
        StoredDocument read = held.getFirst();
        assertEquals("resistor", read.getId());
        assertEquals(Optional.of(WORKSPACE), read.owner());
        assertEquals("three", opened.heldText(read));
        assertEquals(List.of(1), opened.revisions("resistor").stream().map(DocumentRevision::revision).toList());
        assertEquals(Optional.of("three"), opened.sourceAt("resistor", 1));
        assertEquals(Optional.of(3), opened.readFile("resistor", "body.step").map(bytes -> bytes.length));

        opened.remove(read);
        assertFalse(Files.exists(folder.resolve("workspaces/w1/components/resistors/resistor.component.jm")));
        assertEquals(List.of(), storeOf(folder).open());
    }

    /** A file put in by hand is a document from the start: its place says its kind, its category and its owner before its text is read. */
    @Test
    void aFilePutInByHandIsWhatItsPlaceSays(@TempDir Path folder) throws IOException {
        storeOf(folder).open();
        Files.createDirectories(folder.resolve("catalogue/components/capacitors"));
        Files.writeString(folder.resolve("catalogue/components/capacitors/ceramic.component.jm"), "not a document yet", StandardCharsets.UTF_8);
        Files.createDirectories(folder.resolve("projects/p1/circuits"));
        Files.writeString(folder.resolve("projects/p1/circuits/filter.circuit.jm"), "neither", StandardCharsets.UTF_8);

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
