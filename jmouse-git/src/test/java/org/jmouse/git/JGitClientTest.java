package org.jmouse.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/** The real JGit client against local repositories reached over {@code file://}; no network. */
class JGitClientTest {

    private static final GitLimits LIMITS = GitLimits.DEFAULT.withTimeout(java.time.Duration.ofSeconds(30));

    @TempDir
    Path temporary;

    private SourceRepository source;
    private GitClient        client;
    private String           firstCommit;
    private String           mainTip;
    private String           featureTip;

    @BeforeEach
    void createSource() throws Exception {
        source = SourceRepository.create(temporary.resolve("source"));
        firstCommit = source.commit("README.md", "first");
        mainTip = source.commit("README.md", "second");
        source.createBranchAndSwitch("feature");
        featureTip = source.commit("feature.txt", "feature work");
        source.switchTo("main");
        client = new JGitClient(GitAddressPolicy.permitting(GitScheme.FILE));
    }

    @AfterEach
    void closeSource() {
        source.close();
    }

    private GitRemote remote() {
        return GitRemote.anonymous(source.address());
    }

    @Test
    void clonesTheDefaultBranchShallowly() throws Exception {
        Path directory = temporary.resolve("checkout");

        Checkout checkout = client.clone(remote(), BranchSelection.defaultBranch(), directory, LIMITS);

        assertEquals("main", checkout.branch());
        assertEquals(mainTip, checkout.commit().value());
        assertEquals("second", Files.readString(directory.resolve("README.md")));
        assertFalse(Files.exists(directory.resolve("feature.txt")));
        assertTrue(checkout.bytes() > 0);
        try (Git git = Git.open(directory.toFile())) {
            Iterable<RevCommit> history = git.log().call();
            assertEquals(1, StreamSupport.stream(history.spliterator(), false).count(), "depth 1 fetches the tip alone");
            assertFalse(StreamSupport.stream(git.log().call().spliterator(), false)
                    .anyMatch(commit -> commit.name().equals(firstCommit)));
            assertEquals("/dev/null", git.getRepository().getConfig().getString("core", null, "hooksPath"));
            assertFalse(git.getRepository().getConfig().getBoolean("core", null, "symlinks", true));
            assertEquals(source.address().toString(), git.getRepository().getConfig().getString("remote", "origin", "url"));
        }
    }

    @Test
    void clonesANamedBranch() throws Exception {
        Path directory = temporary.resolve("checkout");

        Checkout checkout = client.clone(remote(), BranchSelection.named("feature"), directory, LIMITS);

        assertEquals("feature", checkout.branch());
        assertEquals(featureTip, checkout.commit().value());
        assertEquals("feature work", Files.readString(directory.resolve("feature.txt")));
    }

    @Test
    void aDeeperHistoryIsFetchedWhenAsked() throws Exception {
        Path directory = temporary.resolve("checkout");

        client.clone(remote(), BranchSelection.defaultBranch(), directory, LIMITS.withDepth(2));

        try (Git git = Git.open(directory.toFile())) {
            assertEquals(2, StreamSupport.stream(git.log().call().spliterator(), false).count());
        }
    }

    @Test
    void refusesAMissingBranchAndLeavesTheDirectoryEmpty() throws IOException {
        Path directory = temporary.resolve("checkout");

        GitException failure = assertThrows(GitException.class,
                () -> client.clone(remote(), BranchSelection.named("nowhere"), directory, LIMITS));

        assertEquals(GitException.Reason.BRANCH_NOT_FOUND, failure.reason());
        assertEmpty(directory);
    }

    @Test
    void refusesAnEmptyRepository() throws Exception {
        try (SourceRepository empty = SourceRepository.create(temporary.resolve("empty"))) {
            GitException failure = assertThrows(GitException.class, () -> client.resolve(
                    GitRemote.anonymous(empty.address()), BranchSelection.defaultBranch(), LIMITS));
            assertEquals(GitException.Reason.EMPTY_REPOSITORY, failure.reason());
        }
    }

    @Test
    void refusesARepositoryThatDoesNotExist() {
        URI nowhere = URI.create("file:///" + temporary.resolve("missing").toString().replace('\\', '/'));

        GitException failure = assertThrows(GitException.class, () -> client.clone(
                GitRemote.anonymous(nowhere), BranchSelection.defaultBranch(), temporary.resolve("checkout"), LIMITS));

        assertEquals(GitException.Reason.NOT_FOUND, failure.reason());
    }

    @Test
    void refusesACheckoutOverTheSizeCeilingAndRemovesWhatItWrote() throws Exception {
        byte[] incompressible = new byte[256 * 1024];
        new Random(7).nextBytes(incompressible);
        source.commit("large.bin", incompressible);
        Path directory = temporary.resolve("checkout");

        GitException failure = assertThrows(GitException.class, () -> client.clone(
                remote(), BranchSelection.defaultBranch(), directory, LIMITS.withMaximumBytes(64 * 1024)));

        assertEquals(GitException.Reason.TOO_LARGE, failure.reason());
        assertFalse(failure.retryable());
        assertTrue(Files.isDirectory(directory), "the caller's directory itself is kept");
        assertEmpty(directory);
    }

    @Test
    void refusesToCloneIntoADirectoryThatHoldsFiles() throws IOException {
        Path directory = Files.createDirectories(temporary.resolve("checkout"));
        Files.writeString(directory.resolve("keep.txt"), "mine");

        GitException failure = assertThrows(GitException.class,
                () -> client.clone(remote(), BranchSelection.defaultBranch(), directory, LIMITS));

        assertEquals(GitException.Reason.DIRECTORY_NOT_EMPTY, failure.reason());
        assertEquals("mine", Files.readString(directory.resolve("keep.txt")));
    }

    @Test
    void updateMovesToTheNewTipAndDiscardsLocalChanges() throws Exception {
        Path directory = temporary.resolve("checkout");
        client.clone(remote(), BranchSelection.defaultBranch(), directory, LIMITS);
        String newTip = source.commit("README.md", "third");
        Files.writeString(directory.resolve("README.md"), "edited locally");
        Files.writeString(directory.resolve("stray.txt"), "untracked");

        Checkout checkout = client.update(remote(), BranchSelection.defaultBranch(), directory, LIMITS);

        assertEquals(newTip, checkout.commit().value());
        assertEquals("third", Files.readString(directory.resolve("README.md")));
        assertFalse(Files.exists(directory.resolve("stray.txt")));
        assertEquals(newTip, client.inspect(directory).commit().value());
    }

    @Test
    void updateSwitchesToAnotherBranch() throws Exception {
        Path directory = temporary.resolve("checkout");
        client.clone(remote(), BranchSelection.defaultBranch(), directory, LIMITS);

        Checkout checkout = client.update(remote(), BranchSelection.named("feature"), directory, LIMITS);

        assertEquals("feature", checkout.branch());
        assertEquals(featureTip, checkout.commit().value());
        assertTrue(Files.exists(directory.resolve("feature.txt")));
        assertEquals("feature", client.inspect(directory).branch());
    }

    @Test
    void updateClonesIntoAnEmptyDirectory() {
        Path directory = temporary.resolve("checkout");

        Checkout checkout = client.update(remote(), BranchSelection.defaultBranch(), directory, LIMITS);

        assertEquals(mainTip, checkout.commit().value());
    }

    @Test
    void updateRefusesADirectoryThatIsNotACheckoutOfThatRemote() throws Exception {
        Path plain = Files.createDirectories(temporary.resolve("plain"));
        Files.writeString(plain.resolve("notes.txt"), "not a repository");
        GitException notRepository = assertThrows(GitException.class,
                () -> client.update(remote(), BranchSelection.defaultBranch(), plain, LIMITS));
        assertEquals(GitException.Reason.NOT_A_CHECKOUT, notRepository.reason());
        assertEquals("not a repository", Files.readString(plain.resolve("notes.txt")));

        try (SourceRepository other = SourceRepository.create(temporary.resolve("other"))) {
            other.commit("other.txt", "other");
            Path directory = temporary.resolve("checkout");
            client.clone(GitRemote.anonymous(other.address()), BranchSelection.defaultBranch(), directory, LIMITS);

            GitException anotherRemote = assertThrows(GitException.class,
                    () -> client.update(remote(), BranchSelection.defaultBranch(), directory, LIMITS));
            assertEquals(GitException.Reason.NOT_A_CHECKOUT, anotherRemote.reason());
            assertTrue(Files.exists(directory.resolve("other.txt")));
        }
    }

    @Test
    void inspectReportsTheCommitCheckedOut() {
        Path directory = temporary.resolve("checkout");
        client.clone(remote(), BranchSelection.named("feature"), directory, LIMITS);

        Checkout checkout = client.inspect(directory);

        assertEquals("feature", checkout.branch());
        assertEquals(featureTip, checkout.commit().value());
        assertTrue(checkout.bytes() > 0);
        assertEquals(GitException.Reason.NOT_A_CHECKOUT,
                assertThrows(GitException.class, () -> client.inspect(temporary.resolve("nothing"))).reason());
    }

    @Test
    void resolvesTheDefaultAndANamedBranchWithoutCloning() {
        RemoteHead defaultHead = client.resolve(remote(), BranchSelection.defaultBranch(), LIMITS);
        RemoteHead feature = client.resolve(remote(), BranchSelection.named("feature"), LIMITS);

        assertEquals(new RemoteHead("main", new CommitId(mainTip)), defaultHead);
        assertEquals(new RemoteHead("feature", new CommitId(featureTip)), feature);
        assertEquals(GitException.Reason.BRANCH_NOT_FOUND, assertThrows(GitException.class,
                () -> client.resolve(remote(), BranchSelection.named("absent"), LIMITS)).reason());
    }

    @Test
    void theDefaultClientRefusesALocalAddressBeforeTouchingTheDirectory() {
        Path directory = temporary.resolve("checkout");

        GitException failure = assertThrows(GitException.class, () -> new JGitClient().clone(
                remote(), BranchSelection.defaultBranch(), directory, LIMITS));

        assertEquals(GitException.Reason.ADDRESS_NOT_ALLOWED, failure.reason());
        assertFalse(Files.exists(directory));
    }

    @Test
    void aTokenIsNeverSentToALocalAddress() {
        GitRemote withToken = new GitRemote(source.address(), GitCredentials.accessToken("x-access-token", "secret-token-value"));

        GitException failure = assertThrows(GitException.class,
                () -> client.resolve(withToken, BranchSelection.defaultBranch(), LIMITS));

        assertEquals(GitException.Reason.ADDRESS_NOT_ALLOWED, failure.reason());
        assertFalse(failure.getMessage().contains("secret-token-value"));
    }

    private static void assertEmpty(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> children = Files.list(directory)) {
            assertEquals(0, children.count(), "nothing the failed clone wrote is left behind");
        }
    }
}
