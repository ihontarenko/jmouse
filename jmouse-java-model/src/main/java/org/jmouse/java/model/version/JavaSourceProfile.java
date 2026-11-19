package org.jmouse.java.model.version;

import java.util.Objects;

/** Immutable source rules, independent of host JVM and binary input versions. */
public record JavaSourceProfile(JavaRelease release, PreviewPolicy preview,
                                JavaSpecificationCatalog specification) {
    public JavaSourceProfile {
        Objects.requireNonNull(release, "release");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(specification, "specification");
        specification.classFileVersion(release);
        if (preview instanceof PreviewPolicy.EnabledForRelease enabled && !release.equals(enabled.release())) {
            throw new IllegalArgumentException("Preview release differs from source release");
        }
    }

    public static JavaSourceProfile standard(JavaRelease release) {
        return new JavaSourceProfile(release, PreviewPolicy.disabled(), JavaSpecificationCatalog.standard());
    }

    public FeatureAvailability availability(JavaFeatureId feature) {
        return specification.feature(feature).availability(release);
    }

    public boolean permits(JavaFeatureId feature) {
        return switch (availability(feature)) {
            case STANDARD -> true;
            case PREVIEW -> preview instanceof PreviewPolicy.EnabledForRelease;
            case UNAVAILABLE, RETIRED -> false;
        };
    }
}
