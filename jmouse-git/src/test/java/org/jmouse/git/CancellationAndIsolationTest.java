package org.jmouse.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The caller's cancellation, and isolation from the machine's git configuration; local repositories only. */
class CancellationAndIsolationTest {

    private static final GitLimits LIMITS = GitLimits.DEFAULT.withTimeout(Duration.ofSeconds(30));
    private static final int       FILES  = 120;

    @TempDir
    Path temporary;

    private SourceRepository source;
    private GitClient        client;
    private String           tip;

    @BeforeEach
    void createSource() throws Exception {
        source = SourceRepository.create(temporary.resolve("source"));
        tip = source.commitMany("first", FILES);
        client = new JGitClient(GitAddressPolicy.permitting(GitScheme.FILE));
    }

    @AfterEach
    void closeSource() {
        source.close();
        SystemReader.setInstance(null);
    }

    private GitRemote remote() {
        return GitRemote.anonymous(source.address());
    }

    @Test
    void cancellingMidCloneRemovesWhatItWrote() throws IOException {
        Path directory = temporary.resolve("checkout");
        PollCounter cancellation = new PollCounter(5);

        GitException failure = assertThrows(GitException.class,
                () -> client.clone(remote(), BranchSelection.defaultBranch(), directory, LIMITS, cancellation));

        assertEquals(GitException.Reason.CANCELLED, failure.reason());
        assertFalse(failure.retryable());
        assertTrue(cancellation.polls() > 3, "cancelled while JGit was transferring, not before it began");
        assertTrue(Files.isDirectory(directory));
        assertEmpty(directory);
    }

    @Test
    void aCloneCancelledBeforeItBeginsWritesNothing() throws IOException {
        Path directory = temporary.resolve("checkout");

        GitException failure = assertThrows(GitException.class,
                () -> client.clone(remote(), BranchSelection.defaultBranch(), directory, LIMITS, () -> true));

        assertEquals(GitException.Reason.CANCELLED, failure.reason());
        assertEmpty(directory);
    }

    @Test
    void aSignalThatThrowsCountsAsCancelled() {
        GitException failure = assertThrows(GitException.class, () -> client.clone(remote(),
                BranchSelection.defaultBranch(), temporary.resolve("checkout"), LIMITS, () -> {
                    throw new IllegalStateException("scan cancelled");
                }));

        assertEquals(GitException.Reason.CANCELLED, failure.reason());
    }

    @Test
    void resolveIsCancellable() {
        GitException failure = assertThrows(GitException.class,
                () -> client.resolve(remote(), BranchSelection.defaultBranch(), LIMITS, () -> true));

        assertEquals(GitException.Reason.CANCELLED, failure.reason());
    }

    @Test
    void aCancelledUpdateLeavesTheCheckoutAtTheCommitItHeld() throws Exception {
        Path directory = temporary.resolve("checkout");
        client.clone(remote(), BranchSelection.defaultBranch(), directory, LIMITS);
        String newTip = source.commitMany("second", FILES);
        PollCounter cancellation = new PollCounter(3);

        GitException failure = assertThrows(GitException.class,
                () -> client.update(remote(), BranchSelection.defaultBranch(), directory, LIMITS, cancellation));

        assertEquals(GitException.Reason.CANCELLED, failure.reason());
        Checkout unchanged = client.inspect(directory);
        assertEquals(tip, unchanged.commit().value());
        assertEquals("first 0", Files.readString(directory.resolve("files/0.txt")));
        try (Git git = Git.open(directory.toFile())) {
            assertTrue(git.status().call().isClean(), "the working tree matches the commit it reports");
        }
        assertEquals(newTip, client.update(remote(), BranchSelection.defaultBranch(), directory, LIMITS).commit().value());
        assertEquals("second 0", Files.readString(directory.resolve("files/0.txt")));
    }

    @Test
    void theMachinesGitConfigurationDoesNotReachAClone() throws Exception {
        String elsewhere;
        try (SourceRepository other = SourceRepository.create(temporary.resolve("elsewhere"))) {
            elsewhere = other.commit("elsewhere.txt", "redirected");
            // A user configuration rewriting the source's address to another repository.
            SystemReader.setInstance(new UserConfigurationReader(SystemReader.getInstance(),
                    other.address().toString(), source.address().toString()));

            // Control: plain JGit honours the rewrite, so the configuration really is in effect.
            Path control = temporary.resolve("control");
            try (Git git = Git.cloneRepository().setURI(source.address().toString()).setDirectory(control.toFile()).call()) {
                assertEquals(elsewhere, git.getRepository().resolve("HEAD").name());
            }

            Checkout cloned = client.clone(remote(), BranchSelection.defaultBranch(), temporary.resolve("cloned"), LIMITS);
            assertEquals(tip, cloned.commit().value());
            Checkout updated = client.update(remote(), BranchSelection.defaultBranch(), temporary.resolve("cloned"), LIMITS);
            assertEquals(tip, updated.commit().value());
            assertEquals("main", client.resolve(remote(), BranchSelection.defaultBranch(), LIMITS).branch());
        }
    }

    private static void assertEmpty(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> children = Files.list(directory)) {
            assertEquals(0, children.count(), "nothing the cancelled clone wrote is left behind");
        }
    }

    /** Asks to stop once it has been polled more than a given number of times. */
    private static final class PollCounter implements GitCancellation {

        private final AtomicInteger polls = new AtomicInteger();
        private final int           allowedPolls;

        private PollCounter(int allowedPolls) {
            this.allowedPolls = allowedPolls;
        }

        @Override
        public boolean cancelled() {
            return polls.incrementAndGet() > allowedPolls;
        }

        int polls() {
            return polls.get();
        }
    }

    /** A machine whose user configuration carries {@code url.<target>.insteadOf = <prefix>}. */
    private static final class UserConfigurationReader extends SystemReader.Delegate {

        private final String target;
        private final String prefix;

        private UserConfigurationReader(SystemReader delegate, String target, String prefix) {
            super(delegate);
            this.target = target;
            this.prefix = prefix;
        }

        @Override
        public StoredConfig getUserConfig() throws ConfigInvalidException, IOException {
            StoredConfig configuration = new StoredConfig() {
                @Override
                public void load() {
                }

                @Override
                public void save() {
                }
            };
            configuration.setString("url", target, "insteadOf", prefix);
            return configuration;
        }
    }
}
