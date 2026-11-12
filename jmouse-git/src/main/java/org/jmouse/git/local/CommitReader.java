package org.jmouse.git.local;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/** What is in the history: a commit's files, a file's bytes at a commit. It only ever reads. */
final class CommitReader {

    /** A commit is named by its full id alone: a revision expression — {@code HEAD~1}, {@code main@{1}} — is not a name. */
    private static final Pattern FULL_ID = Pattern.compile("[0-9a-f]{40}");

    private static final byte[] NOTHING = new byte[0];

    private final LocalGitLimits limits;

    CommitReader(LocalGitLimits limits) {
        this.limits = limits;
    }

    CommitDetail detail(Repository repository, String id) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = require(repository, walk, id);
            try (TreeWalk tree = new TreeWalk(repository)) {
                tree.setRecursive(true);
                tree.setFilter(TreeFilter.ANY_DIFF);
                if (commit.getParentCount() == 0) {
                    tree.addTree(new EmptyTreeIterator());
                } else {
                    tree.addTree(walk.parseCommit(commit.getParent(0)).getTree());
                }
                tree.addTree(commit.getTree());
                List<DiffEntry> entries = DiffEntry.scan(tree);
                int listed = Math.min(entries.size(), limits.maximumSelectedFiles());
                List<CommitDetail.CommitFile> files = entries.subList(0, listed).stream().map(CommitReader::fileOf).toList();
                return new CommitDetail(commit.name(), commit.getFullMessage(), files, entries.size() - listed);
            }
        }
    }

    /** A file at a commit and at its first parent, compared. */
    WorkingTreeDiff diff(Repository repository, String id, String source, TextComparison comparison) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = require(repository, walk, id);
            byte[] after = bytesOf(repository, commit.getTree(), source);
            byte[] before = commit.getParentCount() == 0 ? NOTHING
                    : bytesOf(repository, walk.parseCommit(commit.getParent(0)).getTree(), source);
            return comparison.of(source, before, after);
        }
    }

    /** A file as the head has it; empty where it has none, or there is no head yet. */
    byte[] atHead(Repository repository, String source) throws IOException {
        ObjectId head = repository.resolve(Constants.HEAD);
        if (head == null) {
            return NOTHING;
        }
        try (RevWalk walk = new RevWalk(repository)) {
            return bytesOf(repository, walk.parseCommit(head).getTree(), source);
        }
    }

    /** Whether the head has the file: what makes it something that can be gone back to. */
    boolean inHead(Repository repository, String source) throws IOException {
        ObjectId head = repository.resolve(Constants.HEAD);
        if (head == null) {
            return false;
        }
        try (RevWalk walk = new RevWalk(repository); TreeWalk tree = TreeWalk.forPath(repository, source, walk.parseCommit(head).getTree())) {
            return tree != null;
        }
    }

    private byte[] bytesOf(Repository repository, RevTree root, String source) throws IOException {
        try (TreeWalk tree = TreeWalk.forPath(repository, source, root)) {
            if (tree == null) {
                return NOTHING;
            }
            return repository.open(tree.getObjectId(0), Constants.OBJ_BLOB).getBytes(limits.maximumFileBytes());
        }
    }

    private static RevCommit require(Repository repository, RevWalk walk, String id) throws IOException {
        if (id == null || !FULL_ID.matcher(id).matches() || !repository.getObjectDatabase().has(ObjectId.fromString(id))) {
            throw new LocalGitException(LocalGitException.Reason.NOT_FOUND, "There is no such commit: " + id);
        }
        try {
            return walk.parseCommit(ObjectId.fromString(id));
        } catch (org.eclipse.jgit.errors.IncorrectObjectTypeException notACommit) {
            throw new LocalGitException(LocalGitException.Reason.NOT_FOUND, "There is no such commit: " + id);
        }
    }

    private static CommitDetail.CommitFile fileOf(DiffEntry entry) {
        return switch (entry.getChangeType()) {
            case ADD, COPY -> new CommitDetail.CommitFile(entry.getNewPath(), WorkingTreeChange.Kind.ADDED);
            case DELETE -> new CommitDetail.CommitFile(entry.getOldPath(), WorkingTreeChange.Kind.DELETED);
            case MODIFY, RENAME -> new CommitDetail.CommitFile(entry.getNewPath(), WorkingTreeChange.Kind.MODIFIED);
        };
    }
}
