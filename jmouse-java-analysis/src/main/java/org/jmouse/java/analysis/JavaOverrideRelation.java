package org.jmouse.java.analysis;

import org.jmouse.java.model.symbol.JavaMethodKey;

import java.util.Objects;

/** A language override and its erased-signature bridge requirement, before target lowering. */
public record JavaOverrideRelation(
        JavaMethodKey implementation, JavaMethodKey declaration, JavaBridgeRequirement bridge) {
    public JavaOverrideRelation {
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(declaration, "declaration");
        Objects.requireNonNull(bridge, "bridge");
    }
}
