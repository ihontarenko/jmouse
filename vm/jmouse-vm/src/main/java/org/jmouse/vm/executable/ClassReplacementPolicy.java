package org.jmouse.vm.executable;

/** Handling of explicit method overrides when a whole class is subsequently replaced. */
public enum ClassReplacementPolicy {
    REQUIRE_UNMODIFIED,
    PRESERVE_METHOD_REPLACEMENTS,
    DISCARD_METHOD_REPLACEMENTS
}
