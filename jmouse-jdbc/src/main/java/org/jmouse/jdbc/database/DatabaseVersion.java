package org.jmouse.jdbc.database;

public record DatabaseVersion(int major, int minor, int patch, String raw) {

    public static DatabaseVersion unknown(String raw) {
        return new DatabaseVersion(0, 0, 0, raw);
    }

    @Override
    public String toString() {
        if (major == 0 && minor == 0 && patch == 0) {
            return raw != null ? raw : "unknown";
        }

        return major + "." + minor + (patch > 0 ? ("." + patch) : "");
    }

}
