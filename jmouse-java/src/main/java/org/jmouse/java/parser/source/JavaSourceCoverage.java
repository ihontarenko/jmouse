package org.jmouse.java.parser.source;

import java.util.List;
import java.util.Objects;

/**
 * What the producer actually inspected, independently from errors within those inspected regions.
 */
public record JavaSourceCoverage(JavaCoverageLevel level, List<JavaOmittedRegion> omittedRegions) {
    public JavaSourceCoverage {
        Objects.requireNonNull(level, "level");
        omittedRegions = List.copyOf(omittedRegions);
        if (level == JavaCoverageLevel.FULL_SYNTAX && !omittedRegions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Full syntax coverage cannot contain omitted regions");
        }
    }

    public static JavaSourceCoverage unspecified() {
        return new JavaSourceCoverage(JavaCoverageLevel.UNSPECIFIED, List.of());
    }

    public boolean fullyRead() {
        return level == JavaCoverageLevel.FULL_SYNTAX && omittedRegions.isEmpty();
    }
}
