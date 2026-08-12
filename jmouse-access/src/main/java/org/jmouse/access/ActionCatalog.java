package org.jmouse.access;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Every action this installation publishes, and which values each one carries.
 *
 * <p>An action is <em>what is being done</em> — {@code entry.listByPurpose}, {@code label.print} —
 * as opposed to a permission, which is what somebody <em>may</em> do. One permission covers several
 * routes that are not equally safe: {@code entry:read} gates listing, reading one, exporting and
 * printing, and an action is how a rule says <strong>which</strong>.
 *
 * <h2>Why this exists at all</h2>
 *
 * <p>Because a free string is what makes the whole mechanism feel nailed down.
 * {@code entry.listByPurpos} produces a rule that never fires — and a conditional allow that never
 * holds is a refusal nobody ordered, silent and indistinguishable from working. So actions are
 * declared, like permissions, and checked the same way.
 *
 * <p>⚠️ <strong>The catalogue is the product's, and the engine stays ignorant of the words.</strong>
 * The core no more learns {@code entry.listByPurpose} than it learned {@code SPACE}. What it holds is
 * the seam, so a validator can ask.
 *
 * <p>⚠️ <strong>It is computed, never hand-kept.</strong> Scanning the declarations is what keeps it
 * true; a list written beside them is one commit behind from the day it is written — the same
 * argument that reads {@link PermissionCatalog} off the constants rather than off a second list.
 */
public interface ActionCatalog {

    /** What an installation that publishes no actions has. Checks nothing, refuses nothing. */
    static ActionCatalog empty() {
        return Map::of;
    }

    /**
     * A fixed catalogue.
     *
     * @param publishedValuesByAction every action, mapped to the value names it publishes
     */
    static ActionCatalog of(Map<String, Set<String>> publishedValuesByAction) {
        Map<String, Set<String>> copy = new TreeMap<>();

        publishedValuesByAction.forEach((action, values) -> copy.put(action, Set.copyOf(values)));

        return () -> Map.copyOf(copy);
    }

    /** Every action this installation publishes, mapped to the value names it carries. */
    Map<String, Set<String>> publishedValuesByAction();

    /** Every action, in a stable order — what a picker on a screen offers. */
    default Set<String> all() {
        return new TreeSet<>(publishedValuesByAction().keySet());
    }

    default boolean contains(String action) {
        return publishedValuesByAction().containsKey(action);
    }

    /** The values one action publishes, or an empty set where nothing publishes that action. */
    default Set<String> valuesOf(String action) {
        return publishedValuesByAction().getOrDefault(action, Set.of());
    }

    /**
     * Every value name any action publishes.
     *
     * <p>What a rule written <em>without</em> an action can be checked against — which is all such a
     * rule can be checked against, and the right incentive: an unscoped rule holds everywhere in a
     * call, which is almost never what anybody means.
     */
    default Set<String> everyPublishedValue() {
        Set<String> names = new TreeSet<>();

        publishedValuesByAction().values().forEach(names::addAll);

        return names;
    }
}
