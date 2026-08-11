package org.jmouse.access;

import java.util.List;

/**
 * One capability a product declares — what it is called, what shape it has, and where a grant of it
 * may be addressed.
 *
 * <p>The entitlement axis's half of what {@code PermissionCatalog} holds for permissions, and the
 * reason both a policy document and an administration screen can talk about the same thing without
 * either of them owning the list.
 *
 * <h2>⚠️ {@link #scopes} is a list of scope names, not a subject enum</h2>
 *
 * <p>Where a capability may be granted is a question about <em>places</em>, and places already have a
 * vocabulary: {@code ScopeCatalog}. A separate enum of grant subjects would be a second addressing
 * scheme for the same idea, and every reader would have to learn which of the two a given screen
 * meant.
 *
 * <p>It is also what makes the list <em>enforceable</em>: a grant addressed at a scope this definition
 * does not permit is refused when it is written, rather than accepted and then silently never read.
 *
 * <h2>{@link #paid} is not about money</h2>
 *
 * <p>It means <em>closed until something grants it</em>. Whether the thing that grants it was bought,
 * gifted, or switched on by an administrator is provenance, and the engine never needs to know which.
 * A capability that is not paid is simply available, and a grant about it can only ever take it away.
 *
 * @param key         what a grant names, in the product's own vocabulary
 * @param displayName what a screen shows beside it — not decoration: {@code storage-byte} on its own
 *                    tells a reader nothing they could not guess wrongly
 * @param kind        whether it carries a number, and whether that number is counted or consumed
 * @param scopes      the scope names a grant of this may be addressed at, widest first
 * @param paid        whether it is closed until granted
 */
public record CapabilityDefinition(
        String       key,
        String       displayName,
        CapabilityKind kind,
        List<String> scopes,
        boolean      paid
) {

    public CapabilityDefinition {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("A capability has to have a key to be granted by.");
        }

        scopes      = scopes == null ? List.of() : List.copyOf(scopes);
        displayName = displayName == null || displayName.isBlank() ? key : displayName;
    }

    /** Open or closed, and closed until granted. */
    public static CapabilityDefinition gate(String key, String displayName, List<String> scopes) {
        return new CapabilityDefinition(key, displayName, CapabilityKind.GATE, scopes, true);
    }

    /** Open to everybody — a grant about it can only take it away. */
    public static CapabilityDefinition free(String key, String displayName, List<String> scopes) {
        return new CapabilityDefinition(key, displayName, CapabilityKind.GATE, scopes, false);
    }

    /** A standing count. */
    public static CapabilityDefinition limit(String key, String displayName, List<String> scopes) {
        return new CapabilityDefinition(key, displayName, CapabilityKind.LIMIT, scopes, true);
    }

    /** A consumed quantity, counted over a window. */
    public static CapabilityDefinition quota(String key, String displayName, List<String> scopes) {
        return new CapabilityDefinition(key, displayName, CapabilityKind.QUOTA, scopes, true);
    }

    /** Whether a grant of this may be addressed at a place of the named kind. */
    public boolean grantableAt(String scopeName) {
        return scopes.isEmpty() || scopes.contains(scopeName);
    }

    public boolean isMetered() {
        return kind.isMetered();
    }
}
