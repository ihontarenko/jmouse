package org.jmouse.git;

import java.nio.file.Path;

/**
 * A working tree on disk and what is checked out in it.
 *
 * @param directory the directory the caller owns
 * @param branch    the branch checked out
 * @param commit    the commit checked out — the revision every file in the directory came from
 * @param bytes     what the directory holds on disk, repository metadata included
 */
public record Checkout(Path directory, String branch, CommitId commit, long bytes) {

    public Checkout {
        if (directory == null || branch == null || branch.isBlank() || commit == null || bytes < 0) {
            throw new IllegalArgumentException("A checkout needs a directory, a branch, a commit and a nonnegative size.");
        }
    }
}
