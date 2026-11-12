package org.jmouse.git.local;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A repository a program keeps its own files in: each change is a commit of its paths, by its author at its time, and a
 * file's history follows it where it moved, each revision's text read back as it was.
 */
class PathCommitsTest {

    private static final CommitAuthor ANNA = new CommitAuthor("anna", "anna@example.org");
    private static final CommitAuthor IMPORT = new CommitAuthor("import", "import@example.org");

    private final LocalGit git = new JGitLocalGit();

    private static void write(Path root, String path, String text) throws IOException {
        Files.createDirectories(root.resolve(path).getParent());
        Files.writeString(root.resolve(path), text, StandardCharsets.UTF_8);
    }

    @Test
    void aFileIsFollowedThroughItsMoveAndEachRevisionReadsBack(@TempDir Path root) throws IOException {
        git.initialize(root);
        Instant first = Instant.parse("2026-10-23T08:00:00Z");
        write(root, "passive/resistor.jm", "one");
        git.commitPaths(root, new PathsCommit("Imported\n\nOrigin: IMPORTED", IMPORT, first, List.of("passive/resistor.jm")));
        write(root, "passive/resistor.jm", "two");
        git.commitPaths(root, new PathsCommit("Written", ANNA, null, List.of("passive/resistor.jm")));
        // Filed in another category: the file moves, and the commit says so — its text changed too, past any guess.
        Files.createDirectories(root.resolve("resistors"));
        Files.move(root.resolve("passive/resistor.jm"), root.resolve("resistors/resistor.jm"));
        Files.writeString(root.resolve("resistors/resistor.jm"), "three");
        git.commitPaths(root, new PathsCommit("Moved", ANNA, null, List.of(),
                List.of(new PathsCommit.Move("passive/resistor.jm", "resistors/resistor.jm"))));

        List<PathRevision> history = git.history(root, "resistors/resistor.jm");
        assertEquals(List.of("passive/resistor.jm", "passive/resistor.jm", "resistors/resistor.jm"),
                history.stream().map(PathRevision::path).toList());
        assertEquals(List.of("import", "anna", "anna"), history.stream().map(PathRevision::author).toList());
        assertEquals(first, history.getFirst().time());
        assertEquals("Imported\n\nOrigin: IMPORTED", history.getFirst().message().strip());
        assertEquals(List.of(Optional.of("one"), Optional.of("two"), Optional.of("three")),
                history.stream().map(revision -> git.textAt(root, revision.commit(), revision.path())).toList());
        assertEquals(Optional.empty(), git.textAt(root, history.getFirst().commit(), "resistors/resistor.jm"));
    }

    @Test
    void aFileGoneIsCommittedAsRemoved(@TempDir Path root) throws IOException {
        git.initialize(root);
        write(root, "a.jm", "text");
        git.commitPaths(root, new PathsCommit("Added", ANNA, null, List.of("a.jm")));
        Files.delete(root.resolve("a.jm"));
        String removed = git.commitPaths(root, new PathsCommit("Removed", ANNA, null, List.of("a.jm"))).id();
        assertEquals(Optional.empty(), git.textAt(root, removed, "a.jm"));
        // Gone from the head, it has no history there: its history ended with it.
        assertEquals(List.of(), git.history(root, "a.jm"));
    }

    /** Every file's history in one walk: a move carries it over, a file taken away ends it, one put back starts anew. */
    @Test
    void everyFilesHistoryIsReadInOneWalk(@TempDir Path root) throws IOException {
        git.initialize(root);
        write(root, "a/one.jm", "1");
        write(root, "a/two.jm", "2");
        git.commitPaths(root, new PathsCommit("Both", ANNA, null, List.of("a/one.jm", "a/two.jm")));
        Files.createDirectories(root.resolve("b"));
        Files.move(root.resolve("a/one.jm"), root.resolve("b/one.jm"));
        git.commitPaths(root, new PathsCommit("Moved", ANNA, null, List.of(), List.of(new PathsCommit.Move("a/one.jm", "b/one.jm"))));
        Files.delete(root.resolve("a/two.jm"));
        git.commitPaths(root, new PathsCommit("Removed", ANNA, null, List.of("a/two.jm")));
        write(root, "a/two.jm", "again");
        git.commitPaths(root, new PathsCommit("Put back", IMPORT, null, List.of("a/two.jm")));

        Map<String, List<PathRevision>> histories = git.histories(root, path -> path.endsWith(".jm"));
        assertEquals(List.of("a/two.jm", "b/one.jm"), List.copyOf(histories.keySet()));
        assertEquals(List.of("a/one.jm", "b/one.jm"), histories.get("b/one.jm").stream().map(PathRevision::path).toList());
        assertEquals(List.of("1", "1"), histories.get("b/one.jm").stream().map(revision -> git.text(root, revision.content())).toList());
        assertEquals(List.of("import"), histories.get("a/two.jm").stream().map(PathRevision::author).toList());
        assertEquals("again", git.text(root, histories.get("a/two.jm").getFirst().content()));
    }
}
