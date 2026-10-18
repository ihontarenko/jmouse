package org.jmouse.git.local;

import java.nio.file.Path;
import java.util.List;

/**
 * Local working-tree operations. No network, hooks, checkout, reset or execution of a git binary.
 */
public interface LocalGit {
    WorkingTreeStatus status(Path directory);

    List<CommitInfo> log(Path directory, int limit);

    WorkingTreeDiff diff(Path directory, String source, String currentText);

    CommitResult commit(Path directory, CommitSelection selection);
}
