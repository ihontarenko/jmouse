package org.jmouse.git.local;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushTest {

    private static final CommitAuthor AUTHOR = new CommitAuthor("Probe", "probe@example.com");

    @TempDir
    Path folder;
    @TempDir
    Path bare;

    private final LocalGit git = new JGitLocalGit();

    private String commit(String path, String text) throws IOException {
        Files.writeString(folder.resolve(path), text);
        WorkingTreeStatus status = git.status(folder, AUTHOR);
        List<CommitSelection.SelectedFile> files = new ArrayList<>();
        for (WorkingTreeChange change : status.changes()) {
            files.add(new CommitSelection.SelectedFile(change.path(), change.version()));
        }
        return git.commit(folder, new CommitSelection("A commit", status.head(), files, AUTHOR)).id();
    }

    @BeforeEach
    void aRepositoryWithARemote() throws Exception {
        Git.init().setBare(true).setDirectory(bare.toFile()).call().close();
        git.initialize(folder);
        git.setRemote(folder, bare.toUri().toString());
    }

    @Test
    void aBranchIsSentAndTheRemoteHasIt() throws Exception {
        String head = commit("a.txt", "one");
        PushOutcome outcome = git.push(folder, host -> Optional.empty());
        assertEquals(PushOutcome.State.SENT, outcome.state());
        try (Git remote = Git.open(bare.toFile())) {
            assertEquals(head, remote.getRepository().resolve("refs/heads/" + outcome.branch()).name());
        }
        assertEquals(PushOutcome.State.ALREADY_THERE, git.push(folder, host -> Optional.empty()).state());
    }

    @Test
    void aRemoteAheadIsNeverOverwritten() throws Exception {
        commit("a.txt", "one");
        PushOutcome outcome = git.push(folder, host -> Optional.empty());
        // Somebody else's commit on the remote: another clone sends it.
        Path other = bare.resolveSibling(bare.getFileName() + "-other");
        try (Git clone = Git.cloneRepository().setURI(bare.toUri().toString()).setDirectory(other.toFile()).setBranch(outcome.branch()).call()) {
            Files.writeString(other.resolve("b.txt"), "theirs");
            clone.add().addFilepattern("b.txt").call();
            clone.commit().setMessage("theirs").setAuthor("Other", "other@example.com").setCommitter("Other", "other@example.com").setSign(false).call();
            clone.push().call();
            String theirs = clone.getRepository().resolve("HEAD").name();
            commit("a.txt", "two");
            LocalGitException refused = assertThrows(LocalGitException.class, () -> git.push(folder, host -> Optional.empty()));
            assertEquals(LocalGitException.Reason.CONFLICT, refused.reason());
            try (Git remote = Git.open(bare.toFile())) {
                assertEquals(theirs, remote.getRepository().resolve("refs/heads/" + outcome.branch()).name());
            }
        }
    }

    /** Another clone's commit, sent to the remote; what it adds is its file. */
    private void theirCommit(String branch, String file) throws Exception {
        Path other = bare.resolveSibling(bare.getFileName() + "-" + file);
        try (Git clone = Git.cloneRepository().setURI(bare.toUri().toString()).setDirectory(other.toFile()).setBranch(branch).call()) {
            Files.writeString(other.resolve(file), "theirs");
            clone.add().addFilepattern(file).call();
            clone.commit().setMessage("theirs").setAuthor("Other", "other@example.com").setCommitter("Other", "other@example.com").setSign(false).call();
            clone.push().call();
        }
    }

    @Test
    void theRemotesCommitsAreTakenByMovingForward() throws Exception {
        commit("a.txt", "one");
        String branch = git.push(folder, host -> Optional.empty()).branch();
        theirCommit(branch, "b.txt");
        assertEquals(PullOutcome.State.TAKEN, git.pull(folder, host -> Optional.empty()).state());
        assertTrue(Files.exists(folder.resolve("b.txt")));
        assertEquals(PullOutcome.State.ALREADY_HERE, git.pull(folder, host -> Optional.empty()).state());
    }

    @Test
    void historiesThatWentTwoWaysAreNotMerged() throws Exception {
        commit("a.txt", "one");
        String branch = git.push(folder, host -> Optional.empty()).branch();
        theirCommit(branch, "b.txt");
        String ours = commit("c.txt", "ours");
        assertThrows(LocalGitException.class, () -> git.pull(folder, host -> Optional.empty()));
        assertEquals(ours, git.status(folder, AUTHOR).head());
    }

    @Test
    void aChangeInHandTheRemoteWouldOverwriteIsNotLost() throws Exception {
        commit("a.txt", "one");
        String branch = git.push(folder, host -> Optional.empty()).branch();
        Path other = bare.resolveSibling(bare.getFileName() + "-same");
        try (Git clone = Git.cloneRepository().setURI(bare.toUri().toString()).setDirectory(other.toFile()).setBranch(branch).call()) {
            Files.writeString(other.resolve("a.txt"), "theirs");
            clone.add().addFilepattern("a.txt").call();
            clone.commit().setMessage("theirs").setAuthor("Other", "other@example.com").setCommitter("Other", "other@example.com").setSign(false).call();
            clone.push().call();
        }
        Files.writeString(folder.resolve("a.txt"), "in hand");
        assertThrows(LocalGitException.class, () -> git.pull(folder, host -> Optional.empty()));
        assertEquals("in hand", Files.readString(folder.resolve("a.txt")));
    }

    @Test
    void aHookOfTheFolderIsRefusedAndNoTokenIsAskedFor() throws Exception {
        commit("a.txt", "one");
        Path hooks = folder.resolve(".git/hooks");
        Files.createDirectories(hooks);
        Files.writeString(hooks.resolve("pre-push"), "#!/bin/sh\nexit 0\n");
        AtomicInteger asked = new AtomicInteger();
        assertThrows(LocalGitException.class, () -> git.push(folder, host -> {
            asked.incrementAndGet();
            return Optional.of("token");
        }));
        assertEquals(0, asked.get());
    }

    @Test
    void aConnectionSetByTheFolderIsRefused() throws Exception {
        commit("a.txt", "one");
        configure(config -> config.setString("http", null, "proxy", "http://somebody.example/"));
        assertThrows(LocalGitException.class, () -> git.push(folder, host -> Optional.of("token")));
    }

    /** A token is asked for by the host the configuration really resolves the address to, not the one it was set to. */
    @Test
    void theTokenIsAskedForByTheHostTheAddressReallyGoesTo() throws Exception {
        commit("a.txt", "one");
        git.setRemote(folder, "https://github.com/someone/project.git");
        configure(config -> config.setString("url", "https://somebody.invalid/", "insteadOf", "https://github.com/"));
        List<String> asked = new ArrayList<>();
        assertThrows(LocalGitException.class, () -> git.push(folder, host -> {
            asked.add(host);
            return Optional.empty();
        }));
        assertEquals(List.of("somebody.invalid"), asked);
    }

    @Test
    void anAddressHoldsNoPassword() {
        assertThrows(LocalGitException.class, () -> git.setRemote(folder, "https://me:secret@github.com/me/project.git"));
        assertTrue(git.remote(folder).isPresent());
        assertNotNull(git.remote(folder).orElseThrow());
    }

    private interface Change {
        void to(StoredConfig config);
    }

    private void configure(Change change) throws Exception {
        try (Git opened = Git.open(folder.toFile())) {
            Repository repository = opened.getRepository();
            StoredConfig config = repository.getConfig();
            change.to(config);
            config.save();
        }
    }
}
