package org.jmouse.cad.plugin.explorer;

import org.jmouse.plugin.Topic;

/**
 * What the explorer just changed in a mount, told to whoever listens: a plugin keeping an index of the files — the CAD
 * engine mirroring {@code .jm} documents — hears every write, move and removal made through the explorer.
 *
 * @param mount    the mount's id
 * @param key      where it is now; for a removal, where it was
 * @param previous where it was before a move; null for any other change
 */
public record ExplorerChange(String mount, Kind kind, String key, String previous) {

    /** Where the explorer's changes are told. */
    public static final Topic<ExplorerChange> TOPIC = new Topic<>("explorer.changes", ExplorerChange.class);

    public enum Kind {
        WRITTEN,
        FOLDER_MADE,
        MOVED,
        REMOVED
    }
}
