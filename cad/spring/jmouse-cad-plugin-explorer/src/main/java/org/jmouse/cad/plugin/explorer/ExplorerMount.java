package org.jmouse.cad.plugin.explorer;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.source.DocumentLocation.Access;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileTree;

import java.nio.file.Path;

/**
 * One folder the explorer opens at its top: a directory of the server mounted as it stands, or any store a plugin brings
 * — one jMouse storage that is both a {@link FileStore} and a {@link FileTree}. Every change is written to it as it is.
 *
 * @param id     its word in an address: {@code datasheets}
 * @param title  how it is named to a reader
 * @param owner  whose it is — a workspace, a project —; null for the installation's
 * @param store  where its files are read and written
 * @param tree   how its folders are walked and tidied
 * @param access what the mount lets anybody do with it; who may is the engine's to ask the host
 * @param folder where it stands on the server's disk, what Git needs of it; null for a store that is no folder
 */
public record ExplorerMount(String id, String title, DocumentOwner owner, FileStore store, FileTree tree, Access access, Path folder)
        implements Identified {

    /** Where the folders are brought: the server's own, and any store another plugin mounts. */
    public static final Contributions<ExplorerMount> POINT = new Contributions<>("explorer.mounts", ExplorerMount.class);

    /** A mount's id: one word, so it is one word of an address. */
    static final String ID_PATTERN = "[a-z0-9]+(-[a-z0-9]+)*";

    public ExplorerMount {
        if (id == null || !id.matches(ID_PATTERN)) {
            throw new IllegalArgumentException("A mount's id is one word of lowercase letters, digits and dashes, not '" + id + "'.");
        }
        if (access == null) {
            throw new IllegalArgumentException("Mount '" + id + "' says nothing of what it lets be done.");
        }
        title = title == null || title.isBlank() ? id : title;
    }

    /** A store that is a tree, mounted as one folder. */
    public static <S extends FileStore & FileTree> ExplorerMount of(String id, String title, DocumentOwner owner, S store,
                                                                    Access access) {
        return new ExplorerMount(id, title, owner, store, store, access, null);
    }

    /** A folder of the server's disk, through the store that reads it. */
    public static <S extends FileStore & FileTree> ExplorerMount ofFolder(String id, String title, DocumentOwner owner, S store,
                                                                          Access access, Path folder) {
        return new ExplorerMount(id, title, owner, store, store, access, folder);
    }

    /** Whether the mount lets its files be changed at all. */
    public boolean writable() {
        return access == Access.READ_WRITE;
    }
}
