package org.jmouse.git;

import java.nio.file.Path;

/**
 * Bounded Git operations on one branch of a remote. Every network operation checks the remote against the
 * client's {@link GitAddressPolicy} before any I/O, and runs within the given {@link GitLimits}; a
 * {@link GitCancellation} lets the caller stop it.
 *
 * <p>A checkout is shallow (to {@link GitLimits#depth()}), fetches no tags, never recurses into submodules,
 * runs no hooks, writes symbolic links as plain files, does not follow http redirects and reads none of the
 * machine's git configuration. Every failure is a {@link GitException}.
 */
public interface GitClient {

    /**
     * Where a branch of the remote points, without cloning anything.
     *
     * @param remote       the remote to ask
     * @param branch       a named branch, or the remote's default
     * @param limits       the call's ceilings; only the timeout applies
     * @param cancellation checked before and after the listing, which itself cannot be interrupted
     * @return the branch's name and the commit it points at
     */
    RemoteHead resolve(GitRemote remote, BranchSelection branch, GitLimits limits, GitCancellation cancellation);

    /** {@link #resolve(GitRemote, BranchSelection, GitLimits, GitCancellation)} that cannot be cancelled. */
    default RemoteHead resolve(GitRemote remote, BranchSelection branch, GitLimits limits) {
        return resolve(remote, branch, limits, GitCancellation.NONE);
    }

    /**
     * Clones one branch into a directory the caller owns. The directory must be absent or empty; on failure or
     * cancellation, everything this call wrote into it is removed again and the directory itself is left in place.
     *
     * @param remote       the remote to clone
     * @param branch       a named branch, or the remote's default
     * @param directory    the working tree to create; its {@code .git} directory holds the repository
     * @param limits       the call's ceilings
     * @param cancellation honoured until the checkout is complete
     * @return what was checked out
     */
    Checkout clone(GitRemote remote, BranchSelection branch, Path directory, GitLimits limits, GitCancellation cancellation);

    /** {@link #clone(GitRemote, BranchSelection, Path, GitLimits, GitCancellation)} that cannot be cancelled. */
    default Checkout clone(GitRemote remote, BranchSelection branch, Path directory, GitLimits limits) {
        return clone(remote, branch, directory, limits, GitCancellation.NONE);
    }

    /**
     * Brings a checkout up to the branch's current tip: fetches it, resets the working tree hard to it and removes
     * every file git does not track. An absent or empty directory is cloned instead. A directory that is not a
     * checkout of the same remote address is refused rather than overwritten.
     *
     * <p>Cancellation is honoured until the fetch completes and ignored afterwards, so a cancelled update leaves the
     * checkout consistent at the commit it held (fetched objects may remain). Any other failure — a timeout or the
     * size ceiling while the working tree is rewritten — leaves it in an unspecified state; the caller clones afresh.
     * Objects of earlier revisions are not pruned, so a checkout updated many times grows, and the size ceiling
     * counts that growth.
     *
     * @param remote       the remote the checkout was cloned from
     * @param branch       a named branch, or the remote's default; it may differ from the one checked out now
     * @param directory    the existing working tree
     * @param limits       the call's ceilings
     * @param cancellation honoured until the fetch completes
     * @return what is checked out afterwards
     */
    Checkout update(GitRemote remote, BranchSelection branch, Path directory, GitLimits limits, GitCancellation cancellation);

    /** {@link #update(GitRemote, BranchSelection, Path, GitLimits, GitCancellation)} that cannot be cancelled. */
    default Checkout update(GitRemote remote, BranchSelection branch, Path directory, GitLimits limits) {
        return update(remote, branch, directory, limits, GitCancellation.NONE);
    }

    /**
     * Reports what a checkout holds, with no network access.
     *
     * @param directory a working tree made by {@link #clone} or {@link #update}
     * @return the branch and commit checked out, and the directory's size
     */
    Checkout inspect(Path directory);
}
