package org.jmouse.git.local;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

    public JGitLocalGit() {
        this(LocalGitLimits.DEFAULT);
    }

    public JGitLocalGit(LocalGitLimits limits) {
        this.limits = java.util.Objects.requireNonNull(limits);
        this.commits = new LocalCommitWriter(limits);
    }

    @Override
    public WorkingTreeStatus status(Path directory) {
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
            GitAuthor author = GitAuthor.read(repository);
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
            byte[] before = baseline(repository, source);
            if (RawText.isBinary(before) || RawText.isBinary(after)) {
                throw new LocalGitException(
                        LocalGitException.Reason.UNSUPPORTED_STATE,
                        "Binary files cannot be shown as a text diff.");
            }
            // A line's content is independent of whether the working tree uses LF or CRLF.
            RawText left = lines(before);
            RawText right = lines(after);
            if (left.size() > limits.maximumDiffLines()
                    || right.size() > limits.maximumDiffLines()) {
                throw new LocalGitException(
                        LocalGitException.Reason.TOO_LARGE,
                        "The diff exceeds the interactive line limit.");
            }
            var edits =
                    DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)
                            .diff(RawTextComparator.DEFAULT, left, right)
                            .stream()
                            .map(
                                    edit ->
                                            new WorkingTreeDiff.LineEdit(
                                                    edit.getBeginA(),
                                                    edit.getEndA(),
                                                    edit.getBeginB(),
                                                    edit.getEndB()))
                            .toList();
            return new WorkingTreeDiff(
                    source,
                    new String(before, StandardCharsets.UTF_8),
                    new String(after, StandardCharsets.UTF_8),
                    edits);
        } catch (IOException exception) {
            throw storage("The local Git diff could not be read.");
        }
    }

    private static RawText lines(byte[] bytes) {
        return new RawText(
                new String(bytes, StandardCharsets.UTF_8)
                        .replace("\r\n", "\n")
                        .getBytes(StandardCharsets.UTF_8));
    }

    private byte[] baseline(Repository repository, String source) throws IOException {
        ObjectId head = repository.resolve(Constants.HEAD);
        if (head == null) {
            return new byte[0];
        }
        try (RevWalk walk = new RevWalk(repository);
                TreeWalk tree =
                        TreeWalk.forPath(repository, source, walk.parseCommit(head).getTree())) {
            if (tree == null) {
                return new byte[0];
            }
            return repository
                    .open(tree.getObjectId(0), Constants.OBJ_BLOB)
                    .getBytes(limits.maximumFileBytes());
        }
    }

    @Override
    public CommitResult commit(Path directory, CommitSelection selection) {
        return commits.commit(directory, selection);
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
