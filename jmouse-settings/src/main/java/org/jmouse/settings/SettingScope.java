package org.jmouse.settings;

/**
 * 🎚️ Whose a setting's value is: the installation's alone, or each reader's — a reader who keeps none reading the
 * installation's, which is then what every reader starts from.
 */
public enum SettingScope {
    /** One value for everybody: how the engine runs, what a plugin reaches. */
    INSTALLATION,
    /** A value for each reader — a column's width, a look —, the installation's standing for a reader who keeps none. */
    READER
}
