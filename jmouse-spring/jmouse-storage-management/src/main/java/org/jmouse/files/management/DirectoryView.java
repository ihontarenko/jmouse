package org.jmouse.files.management;

import org.jmouse.files.jpa.directory.StorageDirectory;

import java.util.Map;

/**
 * 🌳 A directory as a sidebar draws it.
 *
 * <p>⚠️ {@code path} is here and the numbering is not. A tree widget needs the address; {@code treeLeft}
 * and {@code treeRight} are how the server answers subtree questions, and putting them on the wire
 * invites a client to compute containment for itself — which is an authorization decision it must never
 * make.</p>
 *
 * <h3>⚠️ {@code configurations} is empty on a tree listing, and populated on one folder</h3>
 *
 * <p>Resolving an effective rule walks a folder's ancestors, so filling it in for every node of a
 * subtree would turn drawing a sidebar into a resolve per row. A folder's own screen asks for one
 * folder and gets the whole answer with it — which is also what lets a file listing be joined against
 * the rule without a second round trip.</p>
 *
 * @param id             the directory's identifier
 * @param parentId       the directory above, or {@code null} at a root
 * @param name           what a person calls it
 * @param path           the whole address, e.g. {@code innoventa/files/manuals}
 * @param root           whether this is a root, and so cannot be renamed or moved
 * @param depth          how far down it sits, a root being one
 * @param configurations what applies here, by kind — empty where nobody asked
 */
public record DirectoryView(String id, String parentId, String name, String path, boolean root,
                            int depth, Map<String, DirectoryConfigurationView> configurations) {

    /**
     * 🏗️ Describe a directory, without resolving anything about it.
     *
     * @param directory the directory
     * @return the view
     */
    public static DirectoryView of(StorageDirectory directory) {
        return of(directory, Map.of());
    }

    /**
     * 🏗️ Describe a directory and what applies to it.
     *
     * @param directory      the directory
     * @param configurations what applies here, by kind
     * @return the view
     */
    public static DirectoryView of(StorageDirectory directory,
                                   Map<String, DirectoryConfigurationView> configurations) {
        return new DirectoryView(directory.getId(), directory.getParentId(), directory.getName(),
                                 directory.getPath(), directory.isRoot(), directory.getDepth(),
                                 configurations);
    }
}
