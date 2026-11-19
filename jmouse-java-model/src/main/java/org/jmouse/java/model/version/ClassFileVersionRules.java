package org.jmouse.java.model.version;



/** Format rules are catalog data, not an assumption that every minor version must be zero. */
public record ClassFileVersionRules(int minimumMajor, int previewMinimumMajor, int previewMinor) {
    public ClassFileVersionRules {
        if (minimumMajor <= 0 || previewMinimumMajor < minimumMajor || previewMinimumMajor > 0xFFFF
                || previewMinor <= 0 || previewMinor > 0xFFFF) {
            throw new IllegalArgumentException("Invalid classfile version rules");
        }
    }

    public boolean isValidMinor(ClassFileVersion version) {
        return version.major() < previewMinimumMajor || version.minor() == 0 || version.minor() == previewMinor;
    }

    public boolean isPreview(ClassFileVersion version) {
        return version.major() >= previewMinimumMajor && version.minor() == previewMinor;
    }
}
