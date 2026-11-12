package org.jmouse.git.local;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkingTreeChangesTest {

    private static final CommitAuthor AUTHOR = new CommitAuthor("Person", "person@example.test");

    @TempDir
    Path folder;

    private final LocalGit git = new JGitLocalGit();

    private void write(String path, String text) throws IOException {
        Path file = folder.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private String read(String path) throws IOException {
        // The machine's core.autocrlf decides the line endings a checkout writes; what is compared is the text.
        return Files.readString(folder.resolve(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private String commitAll(String message) {
        WorkingTreeStatus status = git.status(folder, AUTHOR);
        List<CommitSelection.SelectedFile> files = status.changes().stream()
                .map(change -> new CommitSelection.SelectedFile(change.path(), change.version())).toList();
        return git.commit(folder, new CommitSelection(message, status.head(), files, AUTHOR)).id();
    }

    private WorkingTreeChange changeOf(String path) {
        return git.status(folder, AUTHOR).changes().stream().filter(change -> change.path().equals(path)).findFirst().orElseThrow();
    }

    private LocalGitException.Reason reasonOf(Runnable work) {
        return assertThrows(LocalGitException.class, work::run).reason();
    }

    @BeforeEach
    void aFirstCommit() throws IOException {
        git.initialize(folder);
        write("board/main.txt", "one\n");
        write("gone.txt", "was here\n");
        commitAll("The first");
    }

    @Test
    void aNewBranchStandsAtTheHeadAndKeepsTheChangesInHand() throws IOException {
        write("board/main.txt", "changed\n");
        git.createBranch(folder, "feature/rework");
        assertEquals("feature/rework", git.status(folder, AUTHOR).branch());
        assertEquals("changed\n", read("board/main.txt"));
        List<BranchInfo> branches = git.branches(folder);
        assertEquals(List.of("feature/rework", "main"), branches.stream().map(BranchInfo::name).toList());
        assertEquals(branches.get(0).head(), branches.get(1).head());
        assertTrue(branches.get(0).current());
    }

    @Test
    void aBranchNameGitWouldNotTakeOrOneTakenIsRefused() {
        assertEquals(LocalGitException.Reason.INVALID_PATH, reasonOf(() -> git.createBranch(folder, "two words")));
        assertEquals(LocalGitException.Reason.INVALID_PATH, reasonOf(() -> git.createBranch(folder, "../escape")));
        assertEquals(LocalGitException.Reason.CONFLICT, reasonOf(() -> git.createBranch(folder, "main")));
    }

    @Test
    void switchingMovesTheFilesBackAndForth() throws IOException {
        git.createBranch(folder, "other");
        write("board/main.txt", "two\n");
        write("only-there.txt", "there\n");
        commitAll("On the other");
        git.switchBranch(folder, "main");
        assertEquals("one\n", read("board/main.txt"));
        assertTrue(Files.notExists(folder.resolve("only-there.txt")));
        git.switchBranch(folder, "other");
        assertEquals("two\n", read("board/main.txt"));
        assertEquals("there\n", read("only-there.txt"));
    }

    @Test
    void aSwitchIsRefusedOverAChangeAndLosesNothing() throws IOException {
        git.createBranch(folder, "other");
        git.switchBranch(folder, "main");
        write("board/main.txt", "unsaved work\n");
        assertEquals(LocalGitException.Reason.CONFLICT, reasonOf(() -> git.switchBranch(folder, "other")));
        assertEquals("unsaved work\n", read("board/main.txt"));
        assertEquals("main", git.status(folder, AUTHOR).branch());
    }

    @Test
    void aSwitchThatWouldOverwriteANewFileIsRefused() throws IOException {
        git.createBranch(folder, "other");
        write("clash.txt", "from the branch\n");
        commitAll("Clash");
        git.switchBranch(folder, "main");
        write("clash.txt", "mine, never committed\n");
        assertEquals(LocalGitException.Reason.CONFLICT, reasonOf(() -> git.switchBranch(folder, "other")));
        assertEquals("mine, never committed\n", read("clash.txt"));
    }

    @Test
    void noSuchBranchIsNotFound() {
        assertEquals(LocalGitException.Reason.NOT_FOUND, reasonOf(() -> git.switchBranch(folder, "nowhere")));
    }

    @Test
    void aRepositoryThatNamesAFilterIsRefusedAndRunsNothing() throws IOException {
        git.createBranch(folder, "other");
        git.switchBranch(folder, "main");
        Path marker = folder.resolve("ran.txt");
        try (Git opened = Git.open(folder.toFile())) {
            StoredConfig config = opened.getRepository().getConfig();
            config.setString("filter", "planted", "smudge", "sh -c 'touch " + marker.toString().replace('\\', '/') + "'");
            config.save();
        }
        assertEquals(LocalGitException.Reason.UNSUPPORTED_STATE, reasonOf(() -> git.switchBranch(folder, "other")));
        assertEquals(LocalGitException.Reason.UNSUPPORTED_STATE, reasonOf(() -> git.createBranch(folder, "another")));
        assertEquals(LocalGitException.Reason.UNSUPPORTED_STATE,
                reasonOf(() -> git.restore(folder, "gone.txt", "any")));
        assertTrue(Files.notExists(marker));
    }

    @Test
    void aChangedFileIsTakenBackAtTheVersionShown() throws IOException {
        write("board/main.txt", "changed\n");
        git.restore(folder, "board/main.txt", changeOf("board/main.txt").version());
        assertEquals("one\n", read("board/main.txt"));
        assertTrue(git.status(folder, AUTHOR).changes().isEmpty());
    }

    @Test
    void aRemovedFileIsTakenBack() throws IOException {
        Files.delete(folder.resolve("gone.txt"));
        git.restore(folder, "gone.txt", changeOf("gone.txt").version());
        assertEquals("was here\n", read("gone.txt"));
        assertTrue(git.status(folder, AUTHOR).changes().isEmpty());
    }

    @Test
    void aFileChangedSinceItWasShownIsNotTakenBack() throws IOException {
        write("board/main.txt", "changed\n");
        String shown = changeOf("board/main.txt").version();
        write("board/main.txt", "changed again, by somebody\n");
        assertEquals(LocalGitException.Reason.CONFLICT, reasonOf(() -> git.restore(folder, "board/main.txt", shown)));
        assertEquals("changed again, by somebody\n", read("board/main.txt"));
    }

    @Test
    void aNewFileHasNothingToGoBackToAndStays() throws IOException {
        write("new.txt", "new\n");
        assertEquals(LocalGitException.Reason.UNSUPPORTED_STATE, reasonOf(() -> git.restore(folder, "new.txt", changeOf("new.txt").version())));
        assertEquals("new\n", read("new.txt"));
    }

    @Test
    void aPathOutOfTheTreeOrIntoGitIsRefused() {
        assertEquals(LocalGitException.Reason.INVALID_PATH, reasonOf(() -> git.restore(folder, "../outside.txt", "any")));
        assertEquals(LocalGitException.Reason.INVALID_PATH, reasonOf(() -> git.restore(folder, ".git/config", "any")));
    }

    @Test
    void aCommitSaysWhatItChangedAgainstItsParent() throws IOException {
        String first = git.log(folder, 1).getFirst().id();
        write("board/main.txt", "two\n");
        Files.delete(folder.resolve("gone.txt"));
        write("added.txt", "new\n");
        String second = commitAll("Second\n\nWith a body.");
        CommitDetail detail = git.commit(folder, second);
        assertEquals("Second\n\nWith a body.", detail.message());
        assertEquals(List.of(new CommitDetail.CommitFile("added.txt", WorkingTreeChange.Kind.ADDED),
                new CommitDetail.CommitFile("board/main.txt", WorkingTreeChange.Kind.MODIFIED),
                new CommitDetail.CommitFile("gone.txt", WorkingTreeChange.Kind.DELETED)), detail.files());
        WorkingTreeDiff diff = git.commitDiff(folder, second, "board/main.txt");
        assertEquals("one\n", diff.before());
        assertEquals("two\n", diff.after());
        assertEquals(1, diff.edits().size());
        CommitDetail root = git.commit(folder, first);
        assertEquals(2, root.files().size());
        assertTrue(root.files().stream().allMatch(file -> file.kind() == WorkingTreeChange.Kind.ADDED));
        assertEquals("", git.commitDiff(folder, first, "gone.txt").before());
    }

    @Test
    void aCommitIsNamedByItsFullIdAlone() {
        assertEquals(LocalGitException.Reason.NOT_FOUND, reasonOf(() -> git.commit(folder, "HEAD")));
        assertEquals(LocalGitException.Reason.NOT_FOUND, reasonOf(() -> git.commit(folder, "main@{1}")));
        assertEquals(LocalGitException.Reason.NOT_FOUND, reasonOf(() -> git.commit(folder, "0".repeat(40))));
    }
}
