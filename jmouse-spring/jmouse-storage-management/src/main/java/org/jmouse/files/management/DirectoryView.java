package org.jmouse.files.management;

import org.jmouse.files.jpa.directory.StorageDirectory;

/**
 * 🌳 A directory as a sidebar draws it.
 *
 * <p>⚠️ {@code path} is here and the numbering is not. A tree widget needs the address; {@code treeLeft}
 * and {@code treeRight} are how the server answers subtree questions, and putting them on the wire
 * invites a client to compute containment for itself — which is an authorization decision it must never
 * make.</p>
 *
 * @param id       the directory's identifier
 * @param parentId the directory above, or {@code null} at a root
 * @param name     what a person calls it
 * @param path     the whole address, e.g. {@code innoventa/files/manuals}
 * @param root     whether this is a root, and so cannot be renamed or moved
 * @param depth    how far down it sits, a root being one
 */
public record DirectoryView(String id, String parentId, String name, String path, boolean root,
                            int depth) {

    /**
     * 🏗️ Describe a directory.
     *
     * @param directory the directory
     * @return the view
     */
    public static DirectoryView of(StorageDirectory directory) {
        return new DirectoryView(directory.getId(), directory.getParentId(), directory.getName(),
                                 directory.getPath(), directory.isRoot(), directory.getDepth());
    }
}
