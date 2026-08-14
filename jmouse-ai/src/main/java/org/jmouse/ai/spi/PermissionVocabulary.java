package org.jmouse.ai.spi;

import java.util.Set;

/**
 * Which permission names exist at all.
 *
 * <p>Asked once, at startup, and never again. A permission nobody can hold is as bad as no permission
 * in the other direction: the action becomes permanently unreachable, and it reads as a broken tool
 * rather than as a typo in its declaration. Somebody eventually "fixes" the tool.
 *
 * <p>The library never learns what a permission <em>means</em> — that is
 * {@link ToolAuthorizer}'s, and keeping the two apart is what lets {@code jmouse-ai} depend on no
 * authorization engine. All this answers is whether the name is a name.
 *
 * <p>⚠️ <strong>An empty vocabulary means the check is skipped, not that everything fails.</strong> A
 * product whose permissions are rows rather than constants, or that has not decided them yet, must be
 * able to run a tool; it says so by supplying {@link #unchecked()} and losing only this one check.
 * Failing every action instead would make the honest answer "I do not have a list" indistinguishable
 * from a catastrophic misconfiguration.
 */
@FunctionalInterface
public interface PermissionVocabulary {

    /** Every permission name a caller could conceivably hold. Empty means "do not check". */
    Set<String> all();

    /** A product that cannot enumerate its permissions, or has not yet decided them. */
    static PermissionVocabulary unchecked() {
        return Set::of;
    }

    /** A fixed set — a product whose permissions are compile-time constants. */
    static PermissionVocabulary of(Set<String> names) {
        Set<String> fixed = Set.copyOf(names);
        return () -> fixed;
    }
}
