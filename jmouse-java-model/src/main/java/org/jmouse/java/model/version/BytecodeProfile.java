package org.jmouse.java.model.version;

import java.util.Objects;

/** Binary admission is separate from decode capability and source-language feature availability. */
public record BytecodeProfile(JavaRelease targetRelease, PreviewPolicy preview,
                              JavaSpecificationCatalog specification) {
    public BytecodeProfile {
        Objects.requireNonNull(targetRelease, "targetRelease");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(specification, "specification");
        specification.classFileVersion(targetRelease);
        if (preview instanceof PreviewPolicy.EnabledForRelease(JavaRelease release) && !targetRelease.equals(release)) {
            throw new IllegalArgumentException("Preview release differs from binary target release");
        }
    }

    public VersionAdmission admission(ClassFileVersion actual) {
        Objects.requireNonNull(actual, "actual");
        ClassFileVersionRules rules = specification.classFileRules();
        int maximumKnown = specification.knownReleases().stream()
                .mapToInt(release -> specification.classFileVersion(release).major()).max().orElseThrow();
        if (actual.major() < rules.minimumMajor() || actual.major() > maximumKnown) {
            return VersionAdmission.UNKNOWN_FORMAT;
        }
        int targetMajor = specification.classFileVersion(targetRelease).major();
        if (actual.major() > targetMajor) {
            return VersionAdmission.ABOVE_TARGET_RELEASE;
        }
        if (!rules.isValidMinor(actual)) {
            return VersionAdmission.INVALID_MINOR;
        }
        if (rules.isPreview(actual)) {
            if (actual.major() != targetMajor) {
                return VersionAdmission.PREVIEW_RELEASE_MISMATCH;
            }
            if (!(preview instanceof PreviewPolicy.EnabledForRelease)) {
                return VersionAdmission.PREVIEW_DISABLED;
            }
        }
        return VersionAdmission.ACCEPTED;
    }
}
