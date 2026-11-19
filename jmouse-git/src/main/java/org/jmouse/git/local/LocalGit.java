package org.jmouse.git.local;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Local working-tree operations. No network, no execution of a git binary, and no hook or filter of the folder's own run:
 * what rewrites the tree refuses a repository whose own configuration names a filter.
 */
public interface LocalGit {
    /** The working tree as a commit by the repository's own identity would see it. */
    default WorkingTreeStatus status(Path directory) {
        return status(directory, null);
    }

    /**
     * The working tree as a commit by {@code author} would see it — the author it names, and whether it can commit —; a
     * {@code null} author is the repository's own identity.
     */
    WorkingTreeStatus status(Path directory, CommitAuthor author);

    /**
     * Makes a folder a repository, its files untouched: what is in it becomes changes to commit. A folder already one is
     * refused.
     */
    void initialize(Path directory);

    /** The folder made a repository whose history starts on that branch. */
    void initialize(Path directory, String firstBranch);

    /**
     * Sets one of a repository's own settings, as {@code git config} does in it — {@code core.longpaths} to {@code true} —:
     * a setting git itself reads when it works in the folder. The key is its section and its name, a dot between.
     */
    void configure(Path directory, String key, String value);

    List<CommitInfo> log(Path directory, int limit);

    WorkingTreeDiff diff(Path directory, String source, String currentText);

    CommitResult commit(Path directory, CommitSelection selection);

    /**
     * A commit of exactly these paths as the working tree holds them — a path that is there added or changed, one that is
     * gone removed —, by its author, at its time: what a program that keeps its own files here writes, one change a time.
     */
    CommitResult commitPaths(Path directory, PathsCommit change);

    /** Every commit that changed a file, the oldest first, followed back through its moves; empty for a repository with none. */
    List<PathRevision> history(Path directory, String path);

    /**
     * The history of every file at the head that `wanted` takes, by its path there, read in one walk of the history:
     * what a program that keeps many files here reads them all back by.
     */
    Map<String, List<PathRevision>> histories(Path directory, Predicate<String> wanted);

    /** The text a file had in a commit; empty when it was not there. */
    Optional<String> textAt(Path directory, String commit, String path);

    /** The text of one version of a file, by the identifier of its content ({@link PathRevision#content()}). */
    String text(Path directory, String content);

    /** What a commit holds against its first parent. */
    CommitDetail commit(Path directory, String id);

    /** A file at a commit against its first parent. */
    WorkingTreeDiff commitDiff(Path directory, String id, String source);

    /** Where the repository pushes to, with no credentials in it, as its configuration resolves it; empty for none. */
    Optional<String> remote(Path directory);

    /** The address it pushes to set. */
    void setRemote(Path directory, String address);

    /**
     * The current branch sent to its remote, never forced; refused where a folder's own repository would run a program or
     * move the connection.
     *
     * @param tokenFor the token for the host the address really goes to; empty to send none
     */
    PushOutcome push(Path directory, Function<String, Optional<String>> tokenFor);

    /**
     * The remote's commits of the current branch taken in, only by moving forward; refused where the histories went two ways
     * or a change in hand stands in the way, and nothing is changed.
     *
     * @param tokenFor the token for the host the address really goes to; empty to send none
     */
    PullOutcome pull(Path directory, Function<String, Optional<String>> tokenFor);

    /** The local branches, by name. */
    List<BranchInfo> branches(Path directory);

    /** A new branch at the head, switched to, the changes in hand kept. */
    void createBranch(Path directory, String name);

    /** The working tree moved to a branch; refused while a tracked file differs or a new file would be overwritten. */
    void switchBranch(Path directory, String name);

    /**
     * Another branch brought into the one in hand, by {@code author} — the repository's own identity where it is null. Refused over changes
     * in hand and where both changed the same lines; then nothing is changed.
     */
    MergeOutcome mergeBranch(Path directory, String name, CommitAuthor author);

    /**
     * A file put back as the last commit has it, in the index and on the disk.
     *
     * @param expectedVersion the {@link WorkingTreeChange#version() version} the reader was shown; refused when it is not the
     *                        file's now
     */
    void restore(Path directory, String source, String expectedVersion);
}
