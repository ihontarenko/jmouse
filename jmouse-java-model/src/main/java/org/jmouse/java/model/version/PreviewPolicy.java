package org.jmouse.java.model.version;

import java.util.Objects;

/** Preview permission always names the particular feature release. */
public sealed interface PreviewPolicy {
    record Disabled() implements PreviewPolicy {}

    record EnabledForRelease(JavaRelease release) implements PreviewPolicy {
        public EnabledForRelease {
            Objects.requireNonNull(release, "release");
        }
    }

    static PreviewPolicy disabled() {
        return new Disabled();
    }

    static PreviewPolicy enabled(JavaRelease release) {
        return new EnabledForRelease(release);
    }
}
