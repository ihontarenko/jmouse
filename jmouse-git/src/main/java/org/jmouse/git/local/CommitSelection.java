package org.jmouse.git.local;

import java.util.List;

/**
 * Commit only these reviewed working-tree versions, against the HEAD that was shown to the user.
 */
public record CommitSelection(String message, String expectedHead, List<SelectedFile> files) {
    private static final int MAXIMUM_MESSAGE_CHARACTERS = 65_536;

    public CommitSelection {
        if (message == null
                || message.isBlank()
                || expectedHead == null
                || files == null
                || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "A commit needs a message, an expected HEAD and selected files.");
        }
        files = List.copyOf(files);
        if (message.length() > MAXIMUM_MESSAGE_CHARACTERS) {
            throw new IllegalArgumentException("The commit message exceeds the supported length.");
        }
        if (files.stream().map(SelectedFile::path).distinct().count() != files.size()) {
            throw new IllegalArgumentException("A file can be selected only once.");
        }
    }

    public record SelectedFile(String path, String version) {
        public SelectedFile {
            if (path == null || path.isBlank() || version == null || version.isBlank()) {
                throw new IllegalArgumentException(
                        "A selected file needs its path and reviewed content version.");
            }
        }
    }
}
