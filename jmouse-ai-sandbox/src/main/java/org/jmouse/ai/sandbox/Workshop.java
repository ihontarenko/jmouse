package org.jmouse.ai.sandbox;

/**
 * A place parts live in — the sandbox's stand-in for whatever a product scopes by.
 *
 * <p>Two of them share a name deliberately, so that ambiguity is a case the driver can reach rather
 * than a branch nobody exercises.
 */
public record Workshop(String id, String name) {

    /** What every {@code InvocationScope} this sandbox produces calls itself. */
    public static final String KIND = "workshop";
}
