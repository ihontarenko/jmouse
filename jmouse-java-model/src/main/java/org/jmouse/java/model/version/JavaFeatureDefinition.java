package org.jmouse.java.model.version;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Availability metadata does not itself claim a parser implementation supports the feature. */
public record JavaFeatureDefinition(JavaFeatureId identity, Optional<JavaRelease> standardSince,
                                    Set<JavaRelease> previewReleases, Optional<JavaRelease> retiredSince) {
    public JavaFeatureDefinition {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(standardSince, "standardSince");
        Objects.requireNonNull(retiredSince, "retiredSince");
        previewReleases = Set.copyOf(previewReleases);
        if (standardSince.isPresent() && retiredSince.isPresent()
                && standardSince.get().compareTo(retiredSince.get()) >= 0) {
            throw new IllegalArgumentException("A feature must retire after standardization");
        }
        if (previewReleases.stream().anyMatch(release -> standardSince.isPresent()
                && release.compareTo(standardSince.get()) >= 0)) {
            throw new IllegalArgumentException("Preview releases must precede standardization");
        }
    }

    public FeatureAvailability availability(JavaRelease release) {
        if (retiredSince.isPresent() && release.compareTo(retiredSince.get()) >= 0) {
            return FeatureAvailability.RETIRED;
        }
        if (standardSince.isPresent() && release.compareTo(standardSince.get()) >= 0) {
            return FeatureAvailability.STANDARD;
        }
        return previewReleases.contains(release) ? FeatureAvailability.PREVIEW : FeatureAvailability.UNAVAILABLE;
    }
}
