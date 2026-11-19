package org.jmouse.java.parser.source;

import java.util.Objects;

/**
 * Source deliberately left unread by a declaration-only request; this is not a syntax diagnostic.
 */
public record JavaOmittedRegion(JavaRegionKind kind, SourceSpan span) {
    public JavaOmittedRegion {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(span, "span");
    }
}
