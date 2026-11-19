package org.jmouse.java.model.version;



/** Exact unsigned classfile header values; admission is decided by a specification profile. */
public record ClassFileVersion(int major, int minor) {
    public ClassFileVersion {
        if (major < 0 || major > 0xFFFF || minor < 0 || minor > 0xFFFF) {
            throw new IllegalArgumentException("Classfile version fields are unsigned 16-bit values");
        }
    }
}
