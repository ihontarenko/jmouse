package org.jmouse.cad.plugin.explorer;

import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;

/**
 * A name the explorer keeps out of every folder — neither listed nor read, written, moved or removed through it —,
 * because another plugin keeps what is under it: Git's {@code .git}, a repository's own workings, which only Git
 * changes.
 *
 * @param id   the contribution's id
 * @param name the entry's name, as it stands in a folder
 */
public record ExplorerReserved(String id, String name) implements Identified {

    /** Where a plugin brings the names it keeps. */
    public static final Contributions<ExplorerReserved> POINT = new Contributions<>("explorer.reserved", ExplorerReserved.class);
}
