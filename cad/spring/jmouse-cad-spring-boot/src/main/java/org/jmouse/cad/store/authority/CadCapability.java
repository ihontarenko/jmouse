package org.jmouse.cad.store.authority;

import java.util.Locale;

/**
 * What jmCAD asks to be allowed. The library names capabilities only; the host decides which of its own permissions
 * each one means, and at which place.
 */
public enum CadCapability implements Capability {

    /** Write the shared catalogue — documents, categories, their status — and read its drafts. */
    CATALOGUE_WRITE,
    /** See what an owner holds: its documents, its sheets. */
    OWNER_READ,
    /** Write an owner's documents. */
    OWNER_WRITE,
    /** Run a circuit: a simulation, a live run, a check of its expectations. */
    SIMULATION_RUN,
    /** See what the installation is doing: the simulations it runs and the circuits running live. */
    SYSTEM_READ;

    /** The engine's own, as {@code cad:} and the capability in words: {@code cad:owner-read}. */
    @Override
    public String id() {
        return "cad:" + name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
