package org.jmouse.cad.plugin.explorer;

import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileTree;

/**
 * One folder the explorer opens at its top: a directory of the server mounted as it stands, or any store a plugin brings
 * — one jMouse storage that is both a {@link FileStore} and a {@link FileTree}. Every change is written to it as it is.
 *
 * @param id       its word in an address: {@code datasheets}
 * @param title    how it is named to a reader
 * @param owner    whose it is — a workspace, a project —; null for the installation's
 * @param store    where its files are read and written
 * @param tree     how its folders are walked and tidied
 * @param writable whether anybody may change it at all; who may is the engine's to ask the host
 */
public record ExplorerMount(String id, String title, DocumentOwner owner, FileStore store, FileTree tree, boolean writable)
        implements Identified {

    /** Where the folders are brought: the server's own, and any store another plugin mounts. */
    public static final Contributions<ExplorerMount> POINT = new Contributions<>("explorer.mounts", ExplorerMount.class);

    public ExplorerMount {
        if (id == null || !id.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("A mount's id is one word of lowercase letters, digits and dashes, not '" + id + "'.");
        }
        title = title == null || title.isBlank() ? id : title;
    }

    /** A store that is a tree, mounted as one folder. */
    public static <S extends FileStore & FileTree> ExplorerMount of(String id, String title, DocumentOwner owner, S store,
                                                                    boolean writable) {
        return new ExplorerMount(id, title, owner, store, store, writable);
    }
}
