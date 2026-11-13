package org.jmouse.settings;

import java.util.Objects;

/**
 * 🎚️ Who holds a value of a setting: the installation, or one reader, named as the application names its readers —
 * {@code ACCOUNT} and its id. A reader holds values only of a {@link SettingScope#READER} setting.
 *
 * @param type what kind of holder: {@link #INSTALLATION_TYPE}, or the application's word for a reader
 * @param id   which one of that kind; empty for the installation
 */
public record SettingHolder(String type, String id) {

    /** The installation's type, the one holder that is not a reader. */
    public static final String INSTALLATION_TYPE = "INSTALLATION";

    /** The installation: the holder of every installation setting, and of what every reader starts from. */
    public static final SettingHolder INSTALLATION = new SettingHolder(INSTALLATION_TYPE, "");

    public SettingHolder {
        Objects.requireNonNull(type, "a holder has a type");
        Objects.requireNonNull(id, "a holder has an id");
        if (type.isBlank()) {
            throw new IllegalArgumentException("A setting's holder names its type.");
        }
        if (type.equals(INSTALLATION_TYPE) != id.isEmpty()) {
            throw new IllegalArgumentException("The installation has no id, and a reader has one: " + type + ":" + id);
        }
    }

    /** One reader, as the application names it. */
    public static SettingHolder reader(String type, String id) {
        if (type.equals(INSTALLATION_TYPE)) {
            throw new IllegalArgumentException("A reader is not the installation.");
        }
        return new SettingHolder(type, id);
    }

    /** Whether this is a reader rather than the installation. */
    public boolean isReader() {
        return !type.equals(INSTALLATION_TYPE);
    }
}
