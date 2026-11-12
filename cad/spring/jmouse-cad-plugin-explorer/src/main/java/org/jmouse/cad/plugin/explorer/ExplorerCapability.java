package org.jmouse.cad.plugin.explorer;

import org.jmouse.cad.store.authority.Capability;

import java.util.Locale;

/** What the explorer asks to be allowed; the host decides each by its id, as it decides the engine's own. */
public enum ExplorerCapability implements Capability {

    /** See a mount, walk it and read its files: {@code cad-explorer:read}. */
    READ,

    /** Change a writable mount — write, make folders, rename, move, remove: {@code cad-explorer:write}. */
    WRITE,

    /**
     * Mount a folder of the server's disk, or unmount one, across the installation: {@code cad-explorer:mount}. Whoever may
     * names any folder the server can read, so it is the host's strongest permission, never a member's.
     */
    MOUNT;

    @Override
    public String id() {
        return ExplorerPlugin.ID + ":" + name().toLowerCase(Locale.ROOT);
    }
}
