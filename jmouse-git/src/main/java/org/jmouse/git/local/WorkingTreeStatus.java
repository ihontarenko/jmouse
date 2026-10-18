package org.jmouse.git.local;

import java.util.List;

public record WorkingTreeStatus(
        boolean repository,
        String branch,
        String head,
        String state,
        String authorName,
        String authorEmail,
        boolean canCommit,
        List<WorkingTreeChange> changes) {
    public WorkingTreeStatus {
        changes = List.copyOf(changes);
    }

    public static WorkingTreeStatus absent() {
        return new WorkingTreeStatus(false, "", "", "ABSENT", "", "", false, List.of());
    }
}
