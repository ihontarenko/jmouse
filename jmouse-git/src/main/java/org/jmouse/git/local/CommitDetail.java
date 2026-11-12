package org.jmouse.git.local;

import java.util.List;

/**
 * What a commit holds, against its first parent; a first commit, against nothing.
 *
 * @param id      its full id
 * @param message its whole message
 * @param files   the files it changed, by path, no more than the interactive limit
 * @param omitted how many more it changed than are listed
 */
public record CommitDetail(String id, String message, List<CommitFile> files, int omitted) {

    public CommitDetail {
        files = List.copyOf(files);
    }

    /** A file a commit changed: {@code kind} is {@code ADDED}, {@code MODIFIED} or {@code DELETED}. */
    public record CommitFile(String path, WorkingTreeChange.Kind kind) {
    }
}
