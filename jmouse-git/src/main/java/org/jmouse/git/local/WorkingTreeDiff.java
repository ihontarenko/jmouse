package org.jmouse.git.local;

import java.util.List;

/**
 * Zero-based, exclusive line ranges. The left side is HEAD; the right side may be an unsaved editor
 * buffer.
 */
public record WorkingTreeDiff(String path, String before, String after, List<LineEdit> edits) {
    public WorkingTreeDiff {
        edits = List.copyOf(edits);
    }

    public record LineEdit(int oldStart, int oldEnd, int newStart, int newEnd) {}
}
