package org.jmouse.git.local;

/**
 * Index and working-tree changes remain separate so an editor can represent staged content
 * honestly.
 */
public record WorkingTreeChange(
        String path, Kind staged, Kind workingTree, String version, boolean selectable) {
    public enum Kind {
        NONE,
        ADDED,
        MODIFIED,
        DELETED,
        CONFLICT,
        UNTRACKED
    }
}
