package org.jmouse.parallax.script;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * jMX reads every text of this module as it read it when its snapshot was recorded: for every document the same tree,
 * every expression cut out at the same line, column and offset; for every broken variant of one, the same refusal at the
 * same place.
 *
 * <p>A snapshot is a file per document under {@code src/test/snapshots/jmx}, at the document's own path: what reading it
 * ends in ({@link DocumentSnapshot#outcome}), then each broken variant's outcome — a refusal as it is said, a variant that
 * still reads by the hash of its snapshot, since a reading of a whole document is already written in full above it.</p>
 *
 * <p>⚠️ When the language is changed on purpose, record them again and read the difference before committing it:
 * {@code mvn test -pl jmouse-parallax -Dtest=JmxReadingSnapshotTest -Djmx.snapshots.record=true}. Recording also removes
 * the snapshot of a document that is gone.</p>
 *
 * <p>A product's catalogue ({@code jmx.catalogues}) is its data, not this module's, and is never recorded here: each of
 * its documents must read, and {@link JmxWritingRoundTripTest} holds what it says.</p>
 */
class JmxReadingSnapshotTest {

    /** Set to {@code true} to write the snapshots instead of comparing with them. */
    private static final String RECORD = "jmx.snapshots.record";
    private static final Path SNAPSHOTS = Path.of("src/test/snapshots/jmx");
    private static final String EXTENSION = ".snapshot";
    /** How {@link DocumentSnapshot#outcome} begins a refusal. */
    private static final String REFUSED = "REFUSED ";

    /** Every this-many-th line of a document is deleted, one at a time, and its first word replaced. */
    private static final int MUTATION_STRIDE = 7;
    private static final int MUTATIONS_PER_DOCUMENT = 12;

    /** A broken text, and what was broken. */
    private record Variant(String description, String text) {
    }

    @Test
    void everyDocumentReadsAsRecorded() {
        Map<String, String> documents = CatalogueTexts.own();
        assertFalse(documents.isEmpty());
        boolean recording = Boolean.getBoolean(RECORD);
        List<String> differing = new ArrayList<>();
        documents.forEach((name, text) -> {
            String snapshot = snapshotOf(name, text);
            Path file = fileOf(name);
            if (recording) {
                write(file, snapshot);
                return;
            }
            String recorded = Files.exists(file) ? read(file) : null;
            if (recorded == null) {
                differing.add(name + ": no snapshot was recorded; record with -D" + RECORD + "=true");
            } else if (!recorded.equals(snapshot)) {
                differing.add(name + ": " + firstDifference(recorded, snapshot));
            }
        });
        Set<Path> stale = staleSnapshots(documents.keySet());
        if (recording) {
            stale.forEach(JmxReadingSnapshotTest::delete);
            return;
        }
        stale.forEach(file -> differing.add(file + ": the document it was recorded from is gone; record again"));
        assertEquals(List.of(), differing.stream().map(entry -> entry.substring(0, entry.indexOf(':'))).toList(),
                () -> differing.isEmpty() ? "" : differing.size() + " documents differ; the first:\n" + differing.getFirst());
    }

    @Test
    void everyProductDocumentReads() {
        List<String> refused = new ArrayList<>();
        CatalogueTexts.products().forEach((name, text) -> {
            String outcome = DocumentSnapshot.outcome(ExperienceReader::read, text, name);
            if (outcome.startsWith(REFUSED)) {
                refused.add(outcome);
            }
        });
        assertEquals(List.of(), refused);
    }


    /** What a text and each of its broken variants read to, as one file. */
    private static String snapshotOf(String name, String text) {
        StringBuilder snapshot = new StringBuilder(DocumentSnapshot.outcome(ExperienceReader::read, text, name));
        if (snapshot.isEmpty() || snapshot.charAt(snapshot.length() - 1) != '\n') {
            snapshot.append('\n');
        }
        for (Variant variant : variants(text)) {
            String outcome = DocumentSnapshot.outcome(ExperienceReader::read, variant.text(), name);
            snapshot.append("== ").append(variant.description()).append('\n')
                    .append(outcome.startsWith(REFUSED) ? outcome : "READS " + hashOf(outcome)).append('\n');
        }
        return snapshot.toString();
    }

    /** Broken variants of a text: its last brace gone, and one line at a time deleted or begun with a stray word. */
    private static List<Variant> variants(String text) {
        List<Variant> variants = new ArrayList<>();
        int lastBrace = text.lastIndexOf('}');
        if (lastBrace >= 0) {
            variants.add(new Variant("the last brace removed", text.substring(0, lastBrace) + text.substring(lastBrace + 1)));
        }
        String[] lines = text.split("\n", -1);
        int made = 0;
        for (int index = MUTATION_STRIDE / 2; index < lines.length && made < MUTATIONS_PER_DOCUMENT; index += MUTATION_STRIDE) {
            if (lines[index].isBlank()) {
                continue;
            }
            List<String> deleted = new ArrayList<>(List.of(lines));
            deleted.remove(index);
            variants.add(new Variant("line " + (index + 1) + " deleted", String.join("\n", deleted)));
            List<String> stray = new ArrayList<>(List.of(lines));
            stray.set(index, lines[index].replaceFirst("^(\\s*)\\S+", "$1zzz"));
            variants.add(new Variant("line " + (index + 1) + " begun with a stray word", String.join("\n", stray)));
            made++;
        }
        return variants;
    }

    /** A document's snapshot file: its own path from the module, under the snapshots. */
    private static Path fileOf(String name) {
        return SNAPSHOTS.resolve(name.replaceFirst("^src/", "") + EXTENSION);
    }

    /** The snapshots recorded from documents that are no longer there. */
    private static Set<Path> staleSnapshots(Set<String> names) {
        Set<Path> expected = new TreeSet<>();
        names.forEach(name -> expected.add(fileOf(name)));
        Set<Path> stale = new TreeSet<>();
        if (!Files.isDirectory(SNAPSHOTS)) {
            return stale;
        }
        try (Stream<Path> files = Files.walk(SNAPSHOTS)) {
            files.filter(file -> file.toString().endsWith(EXTENSION))
                    .map(file -> Path.of(file.toString().replace('\\', '/')))
                    .filter(file -> !expected.contains(file)).forEach(stale::add);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
        return stale;
    }

    private static String firstDifference(String recorded, String actual) {
        String[] one = recorded.split("\n", -1);
        String[] other = actual.split("\n", -1);
        for (int index = 0; index < Math.max(one.length, other.length); index++) {
            String first = index < one.length ? one[index] : "<end>";
            String second = index < other.length ? other[index] : "<end>";
            if (!first.equals(second)) {
                return "snapshot line " + (index + 1) + "\n  recorded: " + first + "\n  now:      " + second;
            }
        }
        return "nowhere";
    }

    private static String hashOf(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(unavailable);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static void write(Path file, String snapshot) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, snapshot, StandardCharsets.UTF_8);
        } catch (IOException unwritable) {
            throw new UncheckedIOException(unwritable);
        }
    }

    private static void delete(Path file) {
        try {
            Files.delete(file);
        } catch (IOException undeletable) {
            throw new UncheckedIOException(undeletable);
        }
    }
}
