package org.jmouse.cad.document.part;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An installed library becomes a place parts are found the same way whatever its format: told missing while its folders are not
 * there, read once in the background — indexing meanwhile —, then its parts scored by every word of a query, the best first.
 */
class InstalledLibraryPartsTest {

    private static final Set<PartCandidate.Kind> EVERY_KIND = EnumSet.allOf(PartCandidate.Kind.class);

    /** A library of a fixed set of parts; whether its folders are there is said by the test. */
    private static class Shelf implements InstalledLibrary {

        private final boolean present;
        private int reads;

        Shelf(boolean present) {
            this.present = present;
        }

        @Override
        public String id() {
            return "shelf";
        }

        @Override
        public String name() {
            return "Shelf libraries";
        }

        @Override
        public Set<PartCandidate.Kind> offers() {
            return EVERY_KIND;
        }

        @Override
        public boolean present() {
            return present;
        }

        @Override
        public String absence() {
            return "Set the shelf's folders.";
        }

        @Override
        public List<IndexedPart> read() {
            reads++;
            return List.of(part("cmp:1", PartCandidate.Kind.COMPONENT, "LM358", "amplifier, dual", "Dual operational amplifier"),
                    part("cmp:2", PartCandidate.Kind.COMPONENT, "NE555", "timer", "Precision timer"),
                    part("pkg:1", PartCandidate.Kind.FOOTPRINT, "SOIC-8", "", ""));
        }

        @Override
        public Optional<String> license() {
            return Optional.of("CC0-1.0");
        }

        @Override
        public FetchedPart fetch(String identifier) {
            throw new IllegalArgumentException(identifier);
        }

        private static IndexedPart part(String identifier, PartCandidate.Kind kind, String title, String keywords, String description) {
            return new IndexedPart(new PartCandidate("shelf", identifier, kind, title, null, null, description, null, null, "CC0-1.0",
                    true, null), keywords, description);
        }
    }

    /** Holds what it is given to run until the test runs it: the background, made visible. */
    private static final class Held implements Executor {

        private final List<Runnable> held = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            held.add(command);
        }

        void runAll() {
            held.forEach(Runnable::run);
            held.clear();
        }
    }

    @Test
    void aLibraryWithNoFolderIsSaidMissingAndNeverRead() {
        Shelf shelf = new Shelf(false);
        Held background = new Held();
        InstalledLibraryParts parts = new InstalledLibraryParts(shelf, background);

        parts.prepare();
        background.runAll();

        assertEquals(new ProviderState(ProviderState.Status.NOT_CONFIGURED, "Set the shelf's folders."), parts.state());
        assertEquals(0, shelf.reads);
    }

    @Test
    void itIsReadOnceInTheBackgroundAndIndexingMeanwhile() {
        Shelf shelf = new Shelf(true);
        Held background = new Held();
        InstalledLibraryParts parts = new InstalledLibraryParts(shelf, background);

        parts.prepare();
        parts.prepare();
        assertEquals(ProviderState.Status.INDEXING, parts.state().status());
        assertEquals(List.of(), parts.search("LM358", EVERY_KIND, 10));

        background.runAll();
        assertEquals(1, shelf.reads);
        assertEquals(ProviderState.ready("2 components, 1 footprint. Fetched parts stay under CC0-1.0."), parts.state());
    }

    @Test
    void everyWordOfTheQueryMustBeFoundTheBestFirst() {
        InstalledLibraryParts parts = new InstalledLibraryParts(new Shelf(true), Runnable::run);
        parts.prepare();

        assertEquals(List.of("cmp:1"), parts.search("dual amplifier", EVERY_KIND, 10).stream().map(PartCandidate::identifier).toList());
        assertEquals(List.of("pkg:1"), parts.search("soic", EVERY_KIND, 10).stream().map(PartCandidate::identifier).toList());
        assertEquals(List.of(), parts.search("dual timer", EVERY_KIND, 10));
        // Asked for footprints, the components are not looked at: the limit is spent on footprints alone.
        assertEquals(List.of(), parts.search("dual amplifier", EnumSet.of(PartCandidate.Kind.FOOTPRINT), 10));
    }

    @Test
    void aLibraryThatCannotBeReadSaysWhy() {
        InstalledLibrary broken = new Shelf(true) {
            @Override
            public List<IndexedPart> read() {
                throw new IllegalStateException("a folder went away");
            }
        };
        InstalledLibraryParts parts = new InstalledLibraryParts(broken, Runnable::run);
        parts.prepare();

        assertEquals(ProviderState.failed("The installed Shelf libraries could not be read: a folder went away"), parts.state());
    }
}
