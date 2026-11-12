package org.jmouse.git.local;

import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

import java.nio.charset.StandardCharsets;

/** Two versions of a file as line edits: one way, for a working tree against its head and for a commit against its parent. */
final class TextComparison {

    private final LocalGitLimits limits;

    TextComparison(LocalGitLimits limits) {
        this.limits = limits;
    }

    WorkingTreeDiff of(String source, byte[] before, byte[] after) {
        if (RawText.isBinary(before) || RawText.isBinary(after)) {
            throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE, "Binary files cannot be shown as a text diff.");
        }
        // A line's content is independent of whether the working tree uses LF or CRLF.
        RawText left = lines(before);
        RawText right = lines(after);
        if (left.size() > limits.maximumDiffLines() || right.size() > limits.maximumDiffLines()) {
            throw new LocalGitException(LocalGitException.Reason.TOO_LARGE, "The diff exceeds the interactive line limit.");
        }
        var edits = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)
                .diff(RawTextComparator.DEFAULT, left, right).stream()
                .map(edit -> new WorkingTreeDiff.LineEdit(edit.getBeginA(), edit.getEndA(), edit.getBeginB(), edit.getEndB()))
                .toList();
        return new WorkingTreeDiff(source, new String(before, StandardCharsets.UTF_8), new String(after, StandardCharsets.UTF_8), edits);
    }

    private static RawText lines(byte[] bytes) {
        return new RawText(new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8));
    }
}
