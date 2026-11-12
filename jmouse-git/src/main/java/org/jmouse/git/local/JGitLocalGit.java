package org.jmouse.git.local;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Local operations over JGit; the remote checkout client's transport/configuration policy stays
 * independent.
 */
public final class JGitLocalGit implements LocalGit {
    private final LocalGitLimits limits;
    private final LocalCommitWriter commits;
    private final CommitReader history;
    private final TextComparison comparison;
    private final WorkingTreeChanger changer;
    private final RepositoryPusher   pusher = new RepositoryPusher();
    private final PathCommits        paths  = new PathCommits();

    private static final String FIRST_BRANCH = "main";

    public JGitLocalGit() {
        this(LocalGitLimits.DEFAULT);
    }

    public JGitLocalGit(LocalGitLimits limits) {
        this.limits = java.util.Objects.requireNonNull(limits);
        this.commits = new LocalCommitWriter(limits);
        this.history = new CommitReader(limits);
        this.comparison = new TextComparison(limits);
        this.changer = new WorkingTreeChanger(limits, history);
    }

    @Override
    public void initialize(Path directory) {
        initialize(directory, FIRST_BRANCH);
    }

    @Override
    public void initialize(Path directory, String firstBranch) {
        try (Repository existing = RepositoryAccess.open(directory)) {
            if (existing != null) {
                throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE, "This folder is already in a Git repository.");
            }
        } catch (IOException exception) {
            throw storage("The folder could not be read: " + exception.getMessage());
        }
        try (Git ignored = Git.init().setDirectory(directory.toFile()).setInitialBranch(firstBranch).call()) {
            // Made: nothing else is done to the folder.
        } catch (GitAPIException exception) {
            throw storage("The folder could not be made a Git repository: " + exception.getMessage());
        }
    }


    @Override
    public void configure(Path directory, String key, String value) {
        int dot = key.lastIndexOf('.');
        if (dot <= 0 || dot == key.length() - 1) {
            throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE, "A setting is its section and its name, a dot between: '" + key + "'.");
        }
        try (Repository repository = RepositoryAccess.open(directory)) {
            if (repository == null) {
                throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE, "This folder is no Git repository.");
            }
            StoredConfig settings = repository.getConfig();
            String section = key.substring(0, dot);
            int subsection = section.indexOf('.');
            settings.setString(subsection < 0 ? section : section.substring(0, subsection), subsection < 0 ? null : section.substring(subsection + 1),
                    key.substring(dot + 1), value);
            settings.save();
        } catch (IOException exception) {
            throw storage("The repository's settings could not be written: " + exception.getMessage());
        }
    }
    @Override
    public WorkingTreeStatus status(Path directory, CommitAuthor committing) {
        try (Repository repository = RepositoryAccess.open(directory)) {
            if (repository == null) {
                return WorkingTreeStatus.absent();
            }
            Status status = Git.wrap(repository).status().call();
            WorkingTreeFiles files =
                    new WorkingTreeFiles(repository.getWorkTree().toPath().toRealPath(), limits);
            List<WorkingTreeChange> changes = new ArrayList<>();
            Set<String> paths = new TreeSet<>();
            for (Set<String> contribution :
                    List.of(
                            status.getAdded(),
                            status.getChanged(),
                            status.getRemoved(),
                            status.getModified(),
                            status.getMissing(),
                            status.getUntracked(),
                            status.getConflicting())) {
                paths.addAll(contribution);
            }
            for (String source : paths) {
                String version;
                try {
                    version = files.version(source);
                } catch (LocalGitException exception) {
                    version = WorkingTreeFiles.UNSUPPORTED_VERSION;
                }
                changes.add(
                        new WorkingTreeChange(
                                source,
                                staged(status, source),
                                working(status, source),
                                version,
                                WorkingTreeFiles.selectable(version)));
            }
            GitAuthor author = committing == null ? GitAuthor.read(repository) : new GitAuthor(committing.name(), committing.email());
            ObjectId head = repository.resolve(Constants.HEAD);
            boolean canCommit =
                    repository.getRepositoryState() == RepositoryState.SAFE
                            && status.getConflicting().isEmpty()
                            && author.configured();
            return new WorkingTreeStatus(
                    true,
                    repository.getBranch(),
                    head == null ? "" : head.name(),
                    repository.getRepositoryState().name(),
                    author.name(),
                    author.email(),
                    canCommit,
                    changes);
        } catch (IOException | GitAPIException exception) {
            throw storage("The local Git status could not be read.");
        }
    }

    @Override
    public List<CommitInfo> log(Path directory, int limit) {
        int bounded = Math.clamp(limit, 1, limits.maximumLogEntries());
        try (Repository repository = RepositoryAccess.require(directory)) {
            if (repository.resolve(Constants.HEAD) == null) {
                return List.of();
            }
            Map<String, List<CommitInfo.Reference>> labels = new HashMap<>();
            for (Ref reference : repository.getRefDatabase().getRefsByPrefix(Constants.R_REFS)) {
                CommitInfo.Kind kind =
                        reference.getName().startsWith(Constants.R_HEADS)
                                ? CommitInfo.Kind.BRANCH
                                : reference.getName().startsWith(Constants.R_REMOTES)
                                        ? CommitInfo.Kind.REMOTE
                                        : reference.getName().startsWith(Constants.R_TAGS)
                                                ? CommitInfo.Kind.TAG
                                                : null;
                if (kind == null) {
                    continue;
                }
                Ref peeled = repository.getRefDatabase().peel(reference);
                ObjectId identifier =
                        peeled.getPeeledObjectId() == null
                                ? peeled.getObjectId()
                                : peeled.getPeeledObjectId();
                if (identifier != null) {
                    labels.computeIfAbsent(identifier.name(), ignored -> new ArrayList<>())
                            .add(
                                    new CommitInfo.Reference(
                                            Repository.shortenRefName(reference.getName()), kind));
                }
            }
            List<CommitInfo> result = new ArrayList<>();
            for (RevCommit commit : Git.wrap(repository).log().setMaxCount(bounded).call()) {
                result.add(
                        new CommitInfo(
                                commit.name(),
                                Arrays.stream(commit.getParents()).map(RevCommit::name).toList(),
                                commit.getAuthorIdent().getName(),
                                commit.getAuthorIdent().getWhenAsInstant(),
                                commit.getShortMessage(),
                                labels.getOrDefault(commit.name(), List.of())));
            }
            return List.copyOf(result);
        } catch (IOException | GitAPIException exception) {
            throw storage("The local Git history could not be read.");
        }
    }

    @Override
    public WorkingTreeDiff diff(Path directory, String source, String currentText) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            WorkingTreeFiles files =
                    new WorkingTreeFiles(repository.getWorkTree().toPath().toRealPath(), limits);
            files.path(source);
            byte[] after =
                    currentText == null
                            ? files.read(source)
                            : currentText.getBytes(StandardCharsets.UTF_8);
            if (after == null) {
                after = new byte[0];
            }
            files.requireSize(after.length);
            return comparison.of(source, history.atHead(repository, source), after);
        } catch (IOException exception) {
            throw storage("The local Git diff could not be read.");
        }
    }

    @Override
    public CommitResult commit(Path directory, CommitSelection selection) {
        return commits.commit(directory, selection);
    }

    @Override
    public CommitResult commitPaths(Path directory, PathsCommit change) {
        return paths.commit(directory, change);
    }

    @Override
    public List<PathRevision> history(Path directory, String path) {
        return paths.history(directory, path);
    }

    @Override
    public Map<String, List<PathRevision>> histories(Path directory, Predicate<String> wanted) {
        return paths.histories(directory, wanted);
    }

    @Override
    public Optional<String> textAt(Path directory, String commit, String path) {
        return paths.textAt(directory, commit, path);
    }

    @Override
    public String text(Path directory, String content) {
        return paths.text(directory, content);
    }

    @Override
    public CommitDetail commit(Path directory, String id) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            return history.detail(repository, id);
        } catch (IOException exception) {
            throw storage("The commit could not be read.");
        }
    }

    @Override
    public WorkingTreeDiff commitDiff(Path directory, String id, String source) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            new WorkingTreeFiles(repository.getWorkTree().toPath().toRealPath(), limits).path(source);
            return history.diff(repository, id, source, comparison);
        } catch (IOException exception) {
            throw storage("The commit's file could not be read.");
        }
    }

    @Override
    public List<BranchInfo> branches(Path directory) {
        return changer.branches(directory);
    }

    @Override
    public void createBranch(Path directory, String name) {
        changer.createBranch(directory, name);
    }

    @Override
    public void switchBranch(Path directory, String name) {
        changer.switchBranch(directory, name);
    }

    @Override
    public Optional<String> remote(Path directory) {
        return pusher.remote(directory);
    }

    @Override
    public void setRemote(Path directory, String address) {
        pusher.setRemote(directory, address);
    }

    @Override
    public PushOutcome push(Path directory, Function<String, Optional<String>> tokenFor) {
        return pusher.push(directory, tokenFor);
    }

    @Override
    public PullOutcome pull(Path directory, Function<String, Optional<String>> tokenFor) {
        return pusher.pull(directory, tokenFor);
    }

    @Override
    public void restore(Path directory, String source, String expectedVersion) {
        changer.restore(directory, source, expectedVersion);
    }

    private static WorkingTreeChange.Kind staged(Status status, String source) {
        if (status.getConflicting().contains(source)) {
            return WorkingTreeChange.Kind.CONFLICT;
        }
        if (status.getAdded().contains(source)) {
            return WorkingTreeChange.Kind.ADDED;
        }
        if (status.getChanged().contains(source)) {
            return WorkingTreeChange.Kind.MODIFIED;
        }
        if (status.getRemoved().contains(source)) {
            return WorkingTreeChange.Kind.DELETED;
        }
        return WorkingTreeChange.Kind.NONE;
    }

    private static WorkingTreeChange.Kind working(Status status, String source) {
        if (status.getConflicting().contains(source)) {
            return WorkingTreeChange.Kind.CONFLICT;
        }
        if (status.getUntracked().contains(source)) {
            return WorkingTreeChange.Kind.UNTRACKED;
        }
        if (status.getMissing().contains(source)) {
            return WorkingTreeChange.Kind.DELETED;
        }
        if (status.getModified().contains(source)) {
            return WorkingTreeChange.Kind.MODIFIED;
        }
        return WorkingTreeChange.Kind.NONE;
    }

    static LocalGitException storage(String message) {
        return new LocalGitException(LocalGitException.Reason.STORAGE, message);
    }
}
