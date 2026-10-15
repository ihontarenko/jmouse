package org.jmouse.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.SubmoduleConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@link GitClient} in-process over JGit: no git binary is needed and no repository hook can run.
 *
 * <p>Every repository it creates or updates is configured before any transfer: http redirects are not followed
 * (so credentials are never carried to another host), symbolic links are written as plain files, submodules are
 * neither fetched nor checked out, and {@code core.hooksPath} points nowhere. Credentials are handed to JGit for
 * the one call and never written into the repository's configuration or its remote address. No operation reads
 * the machine's git configuration (user, XDG, system or JGit file) — see {@link IsolatedConfiguration} — so
 * {@code url.insteadOf}, {@code http.extraHeader} or {@code http.sslVerify} there cannot redirect or weaken a
 * transfer. JGit takes proxies from the JVM's {@code ProxySelector}, never from git configuration.
 *
 * <p>A clone is built as {@code init}, configure, {@code fetch --depth}, then a hard reset onto the fetched
 * commit — so a clone and an update share one path and the configuration is in place before the first byte
 * arrives. Instances are stateless and safe to share.
 */
public final class JGitClient implements GitClient {

    private static final String REMOTE_NAME = "origin";

    private static final Set<RefUpdate.Result> MOVED_REFERENCE = EnumSet.of(
            RefUpdate.Result.NEW, RefUpdate.Result.FORCED, RefUpdate.Result.FAST_FORWARD, RefUpdate.Result.NO_CHANGE);

    /** Branches the remote's HEAD is assumed to mean when a server advertises it without naming its target. */
    private static final List<String> CONVENTIONAL_DEFAULT_BRANCHES = List.of("main", "master");

    private final GitAddressPolicy policy;

    /** A client that reaches https remotes only. */
    public JGitClient() {
        this(GitAddressPolicy.HTTPS);
    }

    /** A client that reaches the addresses this policy admits. */
    public JGitClient(GitAddressPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("A Git client needs an address policy.");
        }
        this.policy = policy;
    }

    @Override
    public RemoteHead resolve(GitRemote remote, BranchSelection branch, GitLimits limits, GitCancellation cancellation) {
        requireArguments(remote, branch, limits, cancellation);
        URI address = policy.requireAllowed(remote);
        requireNotCancelled(cancellation);
        RemoteHead head = IsolatedConfiguration.run(() -> {
            // A listing needs a local repository for its configuration; an in-memory one has no file behind it.
            try (Repository isolated = isolatedRepository()) {
                configureTransport(isolated.getConfig());
                return listRemote(isolated, address.toString(), remote.credentials(), branch, limits);
            }
        });
        requireNotCancelled(cancellation);
        return head;
    }

    private static Repository isolatedRepository() {
        try {
            return new InMemoryRepository.Builder()
                    .setRepositoryDescription(new DfsRepositoryDescription("remote-listing"))
                    .setFS(FS.DETECTED)
                    .build();
        } catch (IOException failure) {
            throw new GitException(GitException.Reason.TRANSPORT);
        }
    }

    @Override
    public Checkout clone(GitRemote remote, BranchSelection branch, Path directory, GitLimits limits,
                          GitCancellation cancellation) {
        requireArguments(remote, branch, limits, cancellation);
        requireDirectory(directory);
        URI address = policy.requireAllowed(remote);
        CheckoutDirectory.requireEmpty(directory);
        try {
            return IsolatedConfiguration.run(
                    () -> cloneInto(address, remote.credentials(), branch, directory, limits, cancellation));
        } catch (GitException failure) {
            CheckoutDirectory.deleteContents(directory);
            throw failure;
        }
    }

    @Override
    public Checkout update(GitRemote remote, BranchSelection branch, Path directory, GitLimits limits,
                           GitCancellation cancellation) {
        requireArguments(remote, branch, limits, cancellation);
        requireDirectory(directory);
        URI address = policy.requireAllowed(remote);
        if (CheckoutDirectory.isAbsentOrEmpty(directory)) {
            return clone(remote, branch, directory, limits, cancellation);
        }
        return IsolatedConfiguration.run(() -> updateIn(address, remote.credentials(), branch, directory, limits, cancellation));
    }

    @Override
    public Checkout inspect(Path directory) {
        requireDirectory(directory);
        return IsolatedConfiguration.run(() -> {
            try (Repository repository = openCheckout(directory)) {
                ObjectId head = repository.resolve(Constants.HEAD);
                if (head == null) {
                    throw new GitException(GitException.Reason.NOT_A_CHECKOUT);
                }
                return new Checkout(directory, repository.getBranch(), new CommitId(head.name()),
                        CheckoutDirectory.bytesOf(directory));
            } catch (IOException failure) {
                throw translate(failure, null);
            }
        });
    }

    private Checkout cloneInto(URI address, GitCredentials credentials, BranchSelection branch, Path directory,
                               GitLimits limits, GitCancellation cancellation) {
        BoundedTransferMonitor monitor = new BoundedTransferMonitor(directory, limits, cancellation);
        monitor.requireContinuing();
        try (Git git = Git.init().setDirectory(directory.toFile()).setBare(false).call()) {
            Repository repository = git.getRepository();
            repository.getConfig().setString("remote", REMOTE_NAME, "url", address.toString());
            configureRepository(repository);
            RemoteHead head = listRemote(repository, REMOTE_NAME, credentials, branch, limits);
            monitor.requireContinuing();
            CommitId commit = synchronise(git, head.branch(), credentials, limits, monitor, CheckoutCompletion.CANCELLABLE);
            return measured(directory, head.branch(), commit, limits);
        } catch (IOException | GitAPIException | JGitInternalException failure) {
            throw translate(failure, monitor);
        }
    }

    private Checkout updateIn(URI address, GitCredentials credentials, BranchSelection branch, Path directory,
                              GitLimits limits, GitCancellation cancellation) {
        BoundedTransferMonitor monitor = new BoundedTransferMonitor(directory, limits, cancellation);
        monitor.requireContinuing();
        try (Repository repository = openCheckout(directory); Git git = new Git(repository)) {
            String clonedFrom = repository.getConfig().getString("remote", REMOTE_NAME, "url");
            if (!address.toString().equals(clonedFrom)) {
                throw new GitException(GitException.Reason.NOT_A_CHECKOUT);
            }
            configureRepository(repository);
            RemoteHead head = listRemote(repository, REMOTE_NAME, credentials, branch, limits);
            monitor.requireContinuing();
            CommitId commit = synchronise(git, head.branch(), credentials, limits, monitor, CheckoutCompletion.RUNS_TO_THE_END);
            return measured(directory, head.branch(), commit, limits);
        } catch (IOException | GitAPIException | JGitInternalException failure) {
            throw translate(failure, monitor);
        }
    }

    /** Whether the caller's cancellation may stop the working tree being rewritten, or only the transfer before it. */
    private enum CheckoutCompletion {

        /** A clone: whatever is half-written is removed again, so cancelling at any point is safe. */
        CANCELLABLE,

        /** An update: once the fetch is done, the working tree is rewritten to the end so it is never half-updated. */
        RUNS_TO_THE_END
    }

    /** Fetches the branch to the configured depth and makes it the checked-out branch, discarding anything else. */
    private CommitId synchronise(Git git, String branch, GitCredentials credentials, GitLimits limits,
                                 BoundedTransferMonitor monitor, CheckoutCompletion completion)
            throws GitAPIException, IOException {
        Repository repository     = git.getRepository();
        String     localBranch    = Constants.R_HEADS + branch;
        String     trackingBranch = Constants.R_REMOTES + REMOTE_NAME + "/" + branch;
        git.fetch()
                .setRemote(REMOTE_NAME)
                .setRefSpecs(new RefSpec("+" + localBranch + ":" + trackingBranch))
                .setDepth(limits.depth())
                .setTagOpt(TagOpt.NO_TAGS)
                .setRecurseSubmodules(SubmoduleConfig.FetchRecurseSubmodulesMode.NO)
                .setTimeout(limits.timeoutSeconds())
                .setProgressMonitor(monitor)
                .setCredentialsProvider(credentialsProvider(credentials))
                .call();
        monitor.requireContinuing();
        if (completion == CheckoutCompletion.RUNS_TO_THE_END) {
            // Nothing in the working tree has changed yet; from here on a cancellation would leave it half-written.
            monitor.stopHonouringCancellation();
        }
        ObjectId fetched = repository.resolve(trackingBranch);
        if (fetched == null) {
            throw new GitException(GitException.Reason.BRANCH_NOT_FOUND);
        }
        RefUpdate branchUpdate = repository.updateRef(localBranch);
        branchUpdate.setNewObjectId(fetched);
        branchUpdate.setForceUpdate(true);
        requireMoved(branchUpdate.update());
        RefUpdate headUpdate = repository.updateRef(Constants.HEAD);
        headUpdate.disableRefLog();
        requireMoved(headUpdate.link(localBranch));
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef(fetched.name()).setProgressMonitor(monitor).call();
        git.clean().setCleanDirectories(true).setIgnore(false).setForce(true).call();
        return new CommitId(fetched.name());
    }

    private static RemoteHead listRemote(Repository repository, String remote, GitCredentials credentials,
                                         BranchSelection branch, GitLimits limits) {
        Map<String, Ref> references;
        try {
            references = new LsRemoteCommand(repository)
                    .setRemote(remote)
                    .setTimeout(limits.timeoutSeconds())
                    .setCredentialsProvider(credentialsProvider(credentials))
                    .callAsMap();
        } catch (GitAPIException | JGitInternalException failure) {
            throw translate(failure, null);
        }
        return selectHead(references, branch);
    }

    private static RemoteHead selectHead(Map<String, Ref> references, BranchSelection branch) {
        String name = switch (branch) {
            case BranchSelection.NamedBranch named -> named.name();
            case BranchSelection.DefaultBranch ignored -> defaultBranchName(references);
        };
        Ref reference = references.get(Constants.R_HEADS + name);
        if (reference == null || reference.getObjectId() == null) {
            throw missingBranch(references);
        }
        return new RemoteHead(name, new CommitId(reference.getObjectId().name()));
    }

    private static String defaultBranchName(Map<String, Ref> references) {
        Ref head = references.get(Constants.HEAD);
        if (head == null) {
            throw missingBranch(references);
        }
        if (head.isSymbolic()) {
            String target = head.getTarget().getName();
            if (!target.startsWith(Constants.R_HEADS)) {
                throw missingBranch(references);
            }
            return target.substring(Constants.R_HEADS.length());
        }
        // An older server advertises HEAD's commit but not its target: pick the branch at that commit.
        List<String> candidates = references.values().stream()
                .filter(reference -> reference.getName().startsWith(Constants.R_HEADS))
                .filter(reference -> head.getObjectId() != null && head.getObjectId().equals(reference.getObjectId()))
                .map(reference -> reference.getName().substring(Constants.R_HEADS.length()))
                .sorted()
                .toList();
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        return CONVENTIONAL_DEFAULT_BRANCHES.stream()
                .filter(candidates::contains)
                .findFirst()
                .orElseThrow(() -> missingBranch(references));
    }

    private static GitException missingBranch(Map<String, Ref> references) {
        boolean anyBranch = references.keySet().stream().anyMatch(name -> name.startsWith(Constants.R_HEADS));
        return new GitException(anyBranch ? GitException.Reason.BRANCH_NOT_FOUND : GitException.Reason.EMPTY_REPOSITORY);
    }

    private static Checkout measured(Path directory, String branch, CommitId commit, GitLimits limits) {
        long bytes = CheckoutDirectory.bytesOf(directory);
        if (bytes > limits.maximumBytes()) {
            throw new GitException(GitException.Reason.TOO_LARGE);
        }
        return new Checkout(directory, branch, commit, bytes);
    }

    /** The checkout's own repository, never one found by climbing to a parent directory. */
    private static Repository openCheckout(Path directory) {
        Path metadata = directory.resolve(Constants.DOT_GIT);
        if (!Files.isDirectory(metadata, LinkOption.NOFOLLOW_LINKS)) {
            throw new GitException(GitException.Reason.NOT_A_CHECKOUT);
        }
        try {
            return new FileRepositoryBuilder()
                    .setGitDir(metadata.toFile())
                    .setWorkTree(directory.toFile())
                    .setMustExist(true)
                    .build();
        } catch (RepositoryNotFoundException failure) {
            throw new GitException(GitException.Reason.NOT_A_CHECKOUT);
        } catch (IOException failure) {
            throw new GitException(GitException.Reason.TRANSPORT);
        }
    }

    private static void configureRepository(Repository repository) throws IOException {
        StoredConfig configuration = repository.getConfig();
        configureTransport(configuration);
        configuration.setBoolean("core", null, "symlinks", false);
        configuration.setString("core", null, "hooksPath", "/dev/null");
        configuration.setBoolean("submodule", null, "recurse", false);
        configuration.setBoolean("fetch", null, "recurseSubmodules", false);
        configuration.save();
    }

    private static void configureTransport(Config configuration) {
        configuration.setBoolean("http", null, "followRedirects", false);
    }

    /**
     * The provider for one call. Anonymous access gets a provider that answers nothing, so a JVM-wide default
     * provider installed by somebody else is never offered to an arbitrary host.
     */
    private static CredentialsProvider credentialsProvider(GitCredentials credentials) {
        return switch (credentials) {
            case GitCredentials.Anonymous ignored -> NoCredentials.INSTANCE;
            case GitCredentials.AccessToken token -> new UsernamePasswordCredentialsProvider(token.username(), token.token());
        };
    }

    private static void requireMoved(RefUpdate.Result result) {
        if (!MOVED_REFERENCE.contains(result)) {
            throw new GitException(GitException.Reason.TRANSPORT);
        }
    }

    private static void requireArguments(GitRemote remote, BranchSelection branch, GitLimits limits,
                                         GitCancellation cancellation) {
        if (remote == null || branch == null || limits == null || cancellation == null) {
            throw new IllegalArgumentException(
                    "A Git operation needs a remote, a branch selection, limits and a cancellation signal.");
        }
    }

    private static void requireNotCancelled(GitCancellation cancellation) {
        if (GitCancellation.requested(cancellation)) {
            throw new GitException(GitException.Reason.CANCELLED);
        }
    }

    private static void requireDirectory(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("A Git operation needs a directory.");
        }
    }

    /**
     * Maps a JGit failure onto a reason. The monitor's own cancellation wins, because JGit reports it as a generic
     * transfer failure. Authentication is recognised by JGit's message text, as JGit has no type for it.
     */
    private static GitException translate(Exception failure, BoundedTransferMonitor monitor) {
        Optional<GitException> cancellation = monitor == null ? Optional.empty() : monitor.refusal();
        if (cancellation.isPresent()) {
            return cancellation.get();
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof GitException known) {
                return known;
            }
            if (cause instanceof NoRemoteRepositoryException || cause instanceof InvalidRemoteException
                    || cause instanceof UnknownHostException) {
                return new GitException(GitException.Reason.NOT_FOUND);
            }
            if (cause instanceof InterruptedIOException) {
                return new GitException(GitException.Reason.TIMED_OUT);
            }
            String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("not authorized") || message.contains("authentication is required")
                    || message.contains("auth fail")) {
                return new GitException(GitException.Reason.UNAUTHORISED);
            }
            if (message.contains("timed out") || message.contains("timeout")) {
                return new GitException(GitException.Reason.TIMED_OUT);
            }
        }
        return new GitException(GitException.Reason.TRANSPORT);
    }

    /** A provider that supplies no credentials and never prompts. */
    private static final class NoCredentials extends CredentialsProvider {

        private static final NoCredentials INSTANCE = new NoCredentials();

        @Override
        public boolean isInteractive() {
            return false;
        }

        @Override
        public boolean supports(CredentialItem... items) {
            return false;
        }

        @Override
        public boolean get(URIish uri, CredentialItem... items) {
            return false;
        }
    }
}
