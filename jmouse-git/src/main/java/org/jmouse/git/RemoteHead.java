package org.jmouse.git;

/**
 * Where a branch of a remote points, as the remote reported it.
 *
 * @param branch the branch's short name — for a default-branch request, the name the remote's {@code HEAD} names
 * @param commit the commit the branch points at
 */
public record RemoteHead(String branch, CommitId commit) {

    public RemoteHead {
        if (branch == null || branch.isBlank() || commit == null) {
            throw new IllegalArgumentException("A remote head needs a branch and a commit.");
        }
    }
}
