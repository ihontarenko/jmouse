package org.jmouse.cad.store.authority;

/**
 * What the engine or one of its plugins asks to be allowed. The engine names its own ({@link CadCapability}); a plugin
 * names its own the same way, and the engine knows none of them. The host decides each by its {@link #id()}.
 */
public interface Capability {

    /** Whose it is, then what: {@code cad:owner-read}, {@code cad-explorer:write} — one name across the engine and its plugins. */
    String id();
}
