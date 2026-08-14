package org.jmouse.ai;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Which guards a product asked for, and which it actually has.
 *
 * <p>Exists for one startup check, and that check is worth a type: <strong>a product that believes it
 * has a rate limit and does not.</strong> Naming guards in configuration is how a product turns some
 * off and adds its own, and a typo there — or a bean that failed to be contributed, or a module left
 * out of a build — produces a running application whose protection is quietly absent. Nothing logs
 * it, because nothing knows a guard was expected.
 *
 * <p>Deliberately a plain value rather than anything in {@code org.jmouse.ai.guard}. The catalogue is
 * what refuses to start, and making it hold the chain in order to ask it a question about names would
 * couple the two for no reason. Whoever wires the application knows both lists; this is the shape it
 * hands them over in.
 *
 * @param requested what configuration asked for, by name
 * @param available what was actually contributed, by name
 */
public record GuardRoster(Set<String> requested, Set<String> available) {

    public GuardRoster {
        requested = requested == null ? Set.of() : Set.copyOf(requested);
        available = available == null ? Set.of() : Set.copyOf(available);
    }

    /**
     * A product that does not name its guards in configuration — the common case.
     *
     * <p>Nothing was asked for, so nothing can be missing, and the check passes without a product
     * having to declare that it opted out.
     */
    public static GuardRoster unchecked() {
        return new GuardRoster(Set.of(), Set.of());
    }

    /** Every guard named in configuration with nothing behind it. */
    public List<String> missing() {
        return requested.stream()
                .filter(name -> !available.contains(name))
                .sorted()
                .toList();
    }

    /** What the refusal names as the alternative, so a typo is one glance from being fixed. */
    public String describeAvailable() {
        return available.isEmpty()
                ? "no guards were contributed at all"
                : "the guards that exist are " + String.join(", ", new TreeSet<>(available));
    }
}
